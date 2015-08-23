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
package framework.services.system;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.TxIsolation;

import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import framework.services.database.IDatabaseDependencyService;
import framework.utils.Utilities;
import models.framework_models.scheduler.SchedulerState;
import play.Configuration;
import play.Logger;
import play.Play;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Utility class which provides methods usefull system level features, namelly:
 * <ul>
 * <li>Scheduler</li>
 * <li>System moniotoring (memory and threads)</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class SysAdminUtilsImpl implements ISysAdminUtils {
    private static Logger.ALogger log = Logger.of(SysAdminUtilsImpl.class);
    private static final String PERMGEN_MEMORY_POOL_NAME = "PS Perm Gen";
    private ActorSystem actorSystem;
    private Cancellable automaticSystemStatus;

    /**
     * Create a new SysAdminUtilsImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param databaseDependencyService
     *            the service which secure the availability of the database
     * @param actorSystem
     *            the Akka actor system
     */
    @Inject
    public SysAdminUtilsImpl(ApplicationLifecycle lifecycle, Configuration configuration, IDatabaseDependencyService databaseDependencyService,
            ActorSystem actorSystem) {
        log.info("SERVICE>>> SysAdminUtilsImpl starting...");
        this.actorSystem = actorSystem;
        flushAllSchedulerStates();
        initAutomatedSystemStatus(configuration);
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> SysAdminUtilsImpl stopping...");
            if (automaticSystemStatus != null) {
                try {
                    getAutomaticSystemStatus().cancel();
                } catch (Exception e) {
                    log.error("Unable to stop the automatic system status", e);
                }
            }
            log.info("SERVICE>>> SysAdminUtilsImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> SysAdminUtilsImpl started");
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.services.system.ISysAdminUtils#scheduleOnce(boolean,
     * java.lang.String, scala.concurrent.duration.FiniteDuration,
     * java.lang.Runnable)
     */
    @Override
    public Cancellable scheduleOnce(final boolean exclusive, final String scheduledActionUuid, FiniteDuration initialDelay, final Runnable runnable) {
        return getActorSystem().scheduler().scheduleOnce(initialDelay, new Runnable() {
            @Override
            public void run() {
                String transactionId = Utilities.getRandomID();
                dumpSystemStatus("ASYNC ACTION START for " + scheduledActionUuid + " and transaction " + transactionId);
                if (!exclusive || checkRunAuthorization(transactionId, scheduledActionUuid)) {
                    runnable.run();
                }
                if (exclusive) {
                    markAsCompleted(transactionId, scheduledActionUuid);
                }
                dumpSystemStatus("ASYNC ACTION STOP for " + scheduledActionUuid + " and transaction " + transactionId);
            }
        }, getActorSystem().dispatcher());
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.services.system.ISysAdminUtils#scheduleRecurring(boolean,
     * java.lang.String, scala.concurrent.duration.FiniteDuration,
     * scala.concurrent.duration.FiniteDuration, java.lang.Runnable, boolean)
     */
    @Override
    public Cancellable scheduleRecurring(final boolean exclusive, final String scheduledActionUuid, FiniteDuration initialDelay, FiniteDuration interval,
            final Runnable runnable, final boolean logInDebug) {
        return getActorSystem().scheduler().schedule(initialDelay, interval, new Runnable() {
            @Override
            public void run() {
                String transactionId = Utilities.getRandomID();
                dumpSystemStatus("SCHEDULER START for " + scheduledActionUuid + " and transaction " + transactionId, logInDebug);
                if (!exclusive || checkRunAuthorization(transactionId, scheduledActionUuid)) {
                    runnable.run();
                    if (exclusive) {
                        markAsCompleted(transactionId, scheduledActionUuid);
                    }
                }
                dumpSystemStatus("SCHEDULER STOP for " + scheduledActionUuid + " and transaction " + transactionId, logInDebug);
            }
        }, getActorSystem().dispatcher());
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.services.system.ISysAdminUtils#scheduleRecurring(boolean,
     * java.lang.String, scala.concurrent.duration.FiniteDuration,
     * scala.concurrent.duration.FiniteDuration, java.lang.Runnable)
     */
    @Override
    public Cancellable scheduleRecurring(final boolean exclusive, final String scheduledActionUuid, FiniteDuration initialDelay, FiniteDuration interval,
            final Runnable runnable) {
        return scheduleRecurring(exclusive, scheduledActionUuid, initialDelay, interval, runnable, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.services.system.ISysAdminUtils#flushAllSchedulerStates()
     */
    @Override
    public void flushAllSchedulerStates() {
        try {
            Ebean.beginTransaction(TxIsolation.SERIALIZABLE);
            SchedulerState.flushAllStates();
            Ebean.commitTransaction();
        } catch (Exception e) {
            log.error("Failed to flush the scheduler states", e);
            rollbackTransactionSilent();
        } finally {
            endTransactionSilent();
        }
    }

    /**
     * Return true if the scheduled process is allowed to run false if another
     * one is already running.
     * 
     * @param transactionId
     *            the unique transaction id for this action
     * @param scheduledActionUuid
     *            the unique name of an action
     * @return
     */
    private boolean checkRunAuthorization(String transactionId, String scheduledActionUuid) {
        try {
            Ebean.beginTransaction(TxIsolation.SERIALIZABLE);
            SchedulerState schedulerState = SchedulerState.getRunningSchedulerStateFromActionUuid(scheduledActionUuid);
            if (schedulerState == null) {
                schedulerState = new SchedulerState();
                schedulerState.actionUuid = scheduledActionUuid;
                schedulerState.isRunning = true;
                schedulerState.transactionId = transactionId;
                schedulerState.save();
                Ebean.commitTransaction();
                return true;
            } else {
                Timestamp lastUpdate = schedulerState.lastUpdate;
                int numberOfMinutes = Play.application().configuration().getInt("maf.test.old.running.process");
                Date currentMinus24Hours = new Date(System.currentTimeMillis() - (numberOfMinutes * 60 * 1000));
                log.info(String.format(
                        "Conflict notification : The scheduled process %s with transaction id %s will not run because another process is already running with transaction id %s",
                        scheduledActionUuid, transactionId, schedulerState.transactionId));
                if (lastUpdate.before(currentMinus24Hours)) {
                    log.error(String.format("ERROR : the scheduled process %s with transaction id %s is still running after %d minutes", scheduledActionUuid,
                            schedulerState.transactionId, numberOfMinutes));
                }
            }
        } catch (Exception e) {
            log.debug("Failed to update the scheduler state", e);
            rollbackTransactionSilent();
        } finally {
            endTransactionSilent();
        }
        return false;
    }

    /**
     * Mark the specified action as completed and flush the old transaction
     * states
     * 
     * @param transactionId
     *            the unique transaction id for this action
     * @param scheduledActionUuid
     *            the unique name of an action
     */
    private void markAsCompleted(String transactionId, String scheduledActionUuid) {
        try {
            Ebean.beginTransaction(TxIsolation.SERIALIZABLE);
            SchedulerState schedulerState = SchedulerState.getRunningSchedulerStateFromActionUuid(scheduledActionUuid);
            if (schedulerState == null) {
                log.error(String.format(
                        "Strange ... No running scheduled action for %s with transaction id %s while one was running and mark as completed is requested",
                        scheduledActionUuid, transactionId));
            } else {
                schedulerState.isRunning = false;
                schedulerState.save();
                log.info(
                        String.format("Scheduled action for %s with transaction id %s completed, scheduler state flushed", scheduledActionUuid, transactionId));
            }
            SchedulerState.flushOldStates();
            Ebean.commitTransaction();
        } catch (Exception e) {
            log.debug("Failed to mark as complete and flush the scheduler state", e);
            rollbackTransactionSilent();
        } finally {
            endTransactionSilent();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.services.system.ISysAdminUtils#dumpSystemConfiguration()
     */
    @Override
    public void dumpSystemConfiguration() {
        log.info("INITIAL CONFIGURATION " + ArrayUtils.toString(getMaxSystemParameters()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.services.system.ISysAdminUtils#dumpSystemStatus(java.lang.
     * String )
     */
    @Override
    public void dumpSystemStatus(String eventName) {
        dumpSystemStatus(eventName, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.services.system.ISysAdminUtils#dumpSystemStatus(java.lang.
     * String , boolean)
     */
    @Override
    public void dumpSystemStatus(String eventName, boolean logAsDebug) {
        if (logAsDebug) {
            log.debug(eventName + " " + ArrayUtils.toString(getSystemStatus()));
        } else {
            log.info(eventName + " " + ArrayUtils.toString(getSystemStatus()));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.services.system.ISysAdminUtils#getMaxSystemParameters()
     */
    @Override
    public long[] getMaxSystemParameters() {
        long[] systemData = new long[3];
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        systemData[0] = memoryMXBean.getHeapMemoryUsage().getMax();
        systemData[1] = memoryMXBean.getNonHeapMemoryUsage().getMax();
        List<MemoryPoolMXBean> mbeans = ManagementFactory.getMemoryPoolMXBeans();
        if (mbeans != null) {
            for (MemoryPoolMXBean mbean : mbeans) {
                MemoryUsage memUsage = mbean.getUsage();
                if (mbean.getName().equals(PERMGEN_MEMORY_POOL_NAME)) {
                    systemData[2] = memUsage.getMax();
                }
            }
        }
        return systemData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.services.system.ISysAdminUtils#getSystemStatus()
     */
    @Override
    public long[] getSystemStatus() {
        long[] systemData = new long[4];
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        systemData[0] = memoryMXBean.getHeapMemoryUsage().getUsed();
        systemData[1] = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        List<MemoryPoolMXBean> mbeans = ManagementFactory.getMemoryPoolMXBeans();
        if (mbeans != null) {
            for (MemoryPoolMXBean mbean : mbeans) {
                MemoryUsage memUsage = mbean.getUsage();
                if (mbean.getName().equals(PERMGEN_MEMORY_POOL_NAME)) {
                    systemData[2] = memUsage.getUsed();
                }
            }
        }
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        systemData[3] = threadMXBean.getThreadCount();
        return systemData;
    }

    private void rollbackTransactionSilent() {
        try {
            Ebean.rollbackTransaction();
        } catch (Exception e) {
        }
    }

    private void endTransactionSilent() {
        try {
            Ebean.endTransaction();
        } catch (Exception e) {
        }
    }

    /**
     * Initialize the automated system status.
     */
    private void initAutomatedSystemStatus(Configuration configuration) {
        if (configuration.getBoolean("maf.sysadmin.dump.vmstatus.active")) {
            int frequency = configuration.getInt("maf.sysadmin.dump.vmstatus.frequency");
            log.info(">>>>>>>>>>>>>>>> Activate automated system status, frequency " + frequency);
            automaticSystemStatus = scheduleRecurring(true, "AUTOMATED STATUS", Duration.create(frequency, TimeUnit.SECONDS),
                    Duration.create(frequency, TimeUnit.SECONDS), new Runnable() {
                        @Override
                        public void run() {
                            // Do nothing, the system will anyway
                            // display the
                            // status
                        }
                    });
            log.info(">>>>>>>>>>>>>>>> Activate automated system status (end)");
        }
    }

    private Cancellable getAutomaticSystemStatus() {
        return automaticSystemStatus;
    }

    private ActorSystem getActorSystem() {
        return actorSystem;
    }
}
