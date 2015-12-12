/*! LICENSE
 *
 * Copyright (c) 2015, The Agile Factory SA and/or its affiliates. All rights
 * reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package framework.services.job;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import akka.actor.Cancellable;
import framework.services.database.IDatabaseDependencyService;
import framework.services.system.ISysAdminUtils;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * The jobs service.
 * 
 * @author Johann Kohler
 */
@Singleton
public class JobsServiceImpl implements IJobsService {
    private static Logger.ALogger log = Logger.of(JobsServiceImpl.class);

    private Map<String, IJobDescriptor> jobDescriptorMap = Collections.synchronizedMap(new HashMap<String, IJobDescriptor>());

    private List<Cancellable> schedulers = null;
    private ISysAdminUtils sysAdminUtils;

    /**
     * Create a new JobsServiceImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param sysAdminUtils
     *            the sysadmin utils
     * @param jobInitialConfig
     *            the list of jobs to be managed by the service. If the right
     *            element of the Pair is "true" then the job is triggered once
     *            the job service is started.
     * @param databaseDependencyService
     */
    @Inject
    public JobsServiceImpl(ApplicationLifecycle lifecycle, Configuration configuration, ISysAdminUtils sysAdminUtils,
            @Named("JobConfig") JobInitialConfig jobInitialConfig, IDatabaseDependencyService databaseDependencyService) {
        log.info("SERVICE>>> JobsServiceImpl starting...");
        this.sysAdminUtils = sysAdminUtils;
        start(jobInitialConfig.getJobs());
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> JobsServiceImpl stopping...");
            cancel();
            log.info("SERVICE>>> JobsServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> JobsServiceImpl started");
    }

    public void start(List<IJobDescriptor> jobs) {
        log.info("***** START start JobsServiceImpl ****");
        this.schedulers = Collections.synchronizedList(new ArrayList<Cancellable>());

        // initialize the schedulers
        for (IJobDescriptor job : jobs) {

            getJobDescriptorMap().put(job.getId(), job);

            log.info("***** START initialization JobsServiceImpl:" + job.getId() + " ****");

            Date now = new Date();

            Calendar firstExecCal = Calendar.getInstance();
            firstExecCal.set(Calendar.HOUR_OF_DAY, job.getStartHour());
            firstExecCal.set(Calendar.MINUTE, job.getStartMinute());
            firstExecCal.set(Calendar.SECOND, 0);
            firstExecCal.set(Calendar.MILLISECOND, 0);

            if (firstExecCal.getTime().before(now)) {
                switch (job.getFrequency()) {
                case HOURLY:
                    while (firstExecCal.getTime().before(now)) {
                        firstExecCal.add(Calendar.HOUR_OF_DAY, 1);
                    }
                    break;
                default:
                    firstExecCal.add(Calendar.DATE, 1);
                    break;
                }
            }

            long diff = firstExecCal.getTimeInMillis() - now.getTime();
            Long howMuchMinutesUntilStartTime = diff / (60 * 1000);

            FiniteDuration frequency = null;
            switch (job.getFrequency()) {
            case ONE_TIME:
                break;
            case HOURLY:
                frequency = FiniteDuration.create(1, TimeUnit.HOURS);
                break;
            default:
                frequency = FiniteDuration.create(1, TimeUnit.DAYS);
                break;
            }

            if (frequency != null) {
                this.schedulers.add(getSysAdminUtils().scheduleRecurring(true, "JobsServiceImpl:" + job.getId(),
                        Duration.create(howMuchMinutesUntilStartTime, TimeUnit.MINUTES), frequency, new Runnable() {
                            @Override
                            public void run() {
                                job.trigger();
                            }
                        }));
            } else {
                this.schedulers
                        .add(getSysAdminUtils().scheduleOnce(true, "JobsServiceImpl:" + job.getId(), Duration.create(5, TimeUnit.MINUTES), new Runnable() {
                            @Override
                            public void run() {
                                job.trigger();
                            }
                        }));
            }

            log.info("***** END initialization JobsServiceImpl:" + job.getId() + " ****");
        }

        log.info("***** END start JobsServiceImpl ****");
    }

    public void cancel() {
        log.info("***** START cancel JobsServiceImpl ****");
        for (Cancellable scheduler : schedulers) {
            if (scheduler != null) {
                scheduler.cancel();
                log.info("***** Scheduler was not null and is cancelled ****");
            }
        }
        log.info("***** END cancel JobsServiceImpl ****");
    }

    @Override
    public List<IJobDescriptor> getJobs() {
        return Collections.unmodifiableList(new ArrayList<IJobDescriptor>(getJobDescriptorMap().values()));
    }

    @Override
    public void trigger(String id) {
        if (getJobDescriptorMap().containsKey(id)) {
            IJobDescriptor job = getJobDescriptorMap().get(id);
            log.info("***** START execution JobsServiceImpl:" + job.getId() + " ****");
            job.trigger();
            log.info("***** END execution JobsServiceImpl:" + job.getId() + " ****");
        } else {
            log.error("Attempt to start a non existing job " + id);
        }
    }

    private Map<String, IJobDescriptor> getJobDescriptorMap() {
        return jobDescriptorMap;
    }

    private ISysAdminUtils getSysAdminUtils() {
        return sysAdminUtils;
    }
}
