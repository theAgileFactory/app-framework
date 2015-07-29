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
import java.util.List;
import java.util.concurrent.TimeUnit;

import play.Logger;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.Cancellable;
import framework.utils.SysAdminUtils;

/**
 * The jobs service.
 * 
 * @author Johann Kohler
 */
public class JobsServiceImpl implements IJobsService {

    private List<IJobDescriptor> jobs = null;

    private List<Cancellable> schedulers = null;

    @Override
    public void start(List<IJobDescriptor> jobs) {
        Logger.info("***** START start JobsServiceImpl ****");

        this.jobs = jobs;

        this.schedulers = Collections.synchronizedList(new ArrayList<Cancellable>());

        // initialize the schedulers
        for (final IJobDescriptor job : this.jobs) {

            Logger.info("***** START initialization JobsServiceImpl:" + job.getId() + " ****");

            Date now = new Date();

            Calendar firstExecCal = Calendar.getInstance();
            firstExecCal.set(Calendar.HOUR_OF_DAY, job.getStartHour());
            firstExecCal.set(Calendar.MINUTE, job.getStartMinute());
            firstExecCal.set(Calendar.SECOND, 0);
            firstExecCal.set(Calendar.MILLISECOND, 0);

            if (firstExecCal.getTime().before(now)) {
                firstExecCal.add(Calendar.DATE, 1);
            }

            long diff = firstExecCal.getTimeInMillis() - now.getTime();
            Long howMuchMinutesUntilStartTime = diff / (60 * 1000);

            FiniteDuration frequency = null;
            switch (job.getFrequency()) {
            case DAILY:
                frequency = FiniteDuration.create(1, TimeUnit.DAYS);
                break;
            case HOURLY:
                frequency = FiniteDuration.create(1, TimeUnit.HOURS);
                break;
            default:
                frequency = FiniteDuration.create(1, TimeUnit.DAYS);
                break;
            }

            this.schedulers.add(SysAdminUtils.scheduleRecurring(true, "JobsServiceImpl:" + job.getId(),
                    Duration.create(howMuchMinutesUntilStartTime, TimeUnit.MINUTES), frequency, new Runnable() {
                        @Override
                        public void run() {
                            job.trigger();
                        }
                    }));

            Logger.info("***** END initialization JobsServiceImpl:" + job.getId() + " ****");

        }

        Logger.info("***** END start JobsServiceImpl ****");
    }

    @Override
    public void cancel() {
        Logger.info("***** START cancel JobsServiceImpl ****");
        for (Cancellable scheduler : schedulers) {
            if (scheduler != null) {
                scheduler.cancel();
            }
        }
        Logger.info("***** END cancel JobsServiceImpl ****");
    }

    @Override
    public List<IJobDescriptor> getJobs() {
        return this.jobs;
    }

    @Override
    public void trigger(String id) {
        for (IJobDescriptor job : jobs) {
            if (job.getId().equals(id)) {
                Logger.info("***** START execution JobsServiceImpl:" + job.getId() + " ****");
                job.trigger();
                Logger.info("***** END execution JobsServiceImpl:" + job.getId() + " ****");
                break;
            }
        }
    }
}
