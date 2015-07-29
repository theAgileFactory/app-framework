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
package framework.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import models.framework_models.scheduler.SchedulerState;

import org.apache.commons.lang3.ArrayUtils;

import play.Logger;
import play.Play;
import play.libs.Akka;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.Cancellable;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.TxIsolation;

/**
 * Utility class which provides methods usefull system level features, namelly:
 * <ul>
 * <li>Scheduler</li>
 * <li>System moniotoring (memory and threads)</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class SysAdminUtils {
    private static Logger.ALogger log = Logger.of(SysAdminUtils.class);
    private static final String PERMGEN_MEMORY_POOL_NAME = "PS Perm Gen";

    public SysAdminUtils() {
    }

    /**
     * Create a scheduler instance (executed only once = asynchronous action)
     * with Akka.<br/>
     * Schedules a Runnable to be run once with a delay, i.e. a time period that
     * has to pass before the runnable is executed.<br/>
     * 
     * If the "exclusive" flag is set, the system will check with the db if
     * another action with the same uuid is already running. If yes, then the
     * action is not run.<br/>
     * 
     * @param exclusive
     * @param scheduledActionUuid
     * @param initialDelay
     * @param runnable
     * @return
     */
    public static Cancellable scheduleOnce(final boolean exclusive, final String scheduledActionUuid, FiniteDuration initialDelay, final Runnable runnable) {
        return Akka.system().scheduler().scheduleOnce(initialDelay, new Runnable() {
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
        }, Akka.system().dispatcher());
    }

    /**
     * Create a scheduler instance (executed repeatedly until cancellation) with
     * Akka.<br/>
     * Schedules a function to be run repeatedly with an initial delay and a
     * frequency. E.g. if you would like the function to be run after 2 seconds
     * and thereafter every 100ms you would set delay = Duration(2,
     * TimeUnit.SECONDS) and interval = Duration(100, TimeUnit.MILLISECONDS).<br/>
     * <b>The start of the scheduled action and the stop is logged</b> <br/>
     * If the "exclusive" flag is set, the system will check with the db if
     * another action with the same uuid is already running. If yes, then the
     * action is not run.
     * 
     * @param exclusive
     * @param scheduledActionUuid
     * @param initialDelay
     * @param interval
     * @param runnable
     * @param logInDebug
     *            if true, the scheduler do not log any message in the INFO log
     *            but rather in the DEBUG log (this is to avoid saturating the
     *            logs with messages for highly frequent operations)
     * @return
     */
    public static Cancellable scheduleRecurring(final boolean exclusive, final String scheduledActionUuid, FiniteDuration initialDelay,
            FiniteDuration interval, final Runnable runnable, final boolean logInDebug) {
        return Akka.system().scheduler().schedule(initialDelay, interval, new Runnable() {
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
        }, Akka.system().dispatcher());
    }

    /**
     * See the <b>scheduleRecurring</b> method.
     * 
     * @param exclusive
     * @param scheduledActionUuid
     * @param initialDelay
     * @param interval
     * @param runnable
     * @return
     */
    public static Cancellable scheduleRecurring(final boolean exclusive, final String scheduledActionUuid, FiniteDuration initialDelay,
            FiniteDuration interval, final Runnable runnable) {
        return scheduleRecurring(exclusive, scheduledActionUuid, initialDelay, interval, runnable, false);
    }

    /**
     * Flush all the scheduler status.<br/>
     * This should be called in principle only at system startup.<br/>
     * Use with care.
     */
    public static void flushAllSchedulerStates() {
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
    private static boolean checkRunAuthorization(String transactionId, String scheduledActionUuid) {
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
                log.info(String
                        .format("Conflict notification : The scheduled process %s with transaction id %s will not run because another process is already running with transaction id %s",
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
    private static void markAsCompleted(String transactionId, String scheduledActionUuid) {
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
                log.info(String.format("Scheduled action for %s with transaction id %s completed, scheduler state flushed", scheduledActionUuid,
                        transactionId));
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

    /**
     * Log the VM memory and thread configuration (max values).
     * 
     * @param scheduledActionName
     *            the name of an action to which this dump is attached
     */
    public static void dumpSystemConfiguration() {
        log.info("INITIAL CONFIGURATION " + ArrayUtils.toString(getMaxSystemParameters()));
    }

    /**
     * Log the VM memory and thread status.
     * 
     * @param eventName
     *            the name of the event attached to this log
     */
    public static void dumpSystemStatus(String eventName) {
        dumpSystemStatus(eventName, false);
    }

    /**
     * Log the VM memory and thread status.
     * 
     * @param eventName
     *            the name of the event attached to this log
     * @param logAsDebug
     *            if true, log in debug mode otherwise log in info mode
     */
    public static void dumpSystemStatus(String eventName, boolean logAsDebug) {
        if (logAsDebug) {
            log.debug(eventName + " " + ArrayUtils.toString(getSystemStatus()));
        } else {
            log.info(eventName + " " + ArrayUtils.toString(getSystemStatus()));
        }
    }

    /**
     * Return a structure which contains various numeric values related to the
     * maximum boundaries of some system parameters, namely:
     * <ol>
     * <li>HeapMemory max</li>
     * <li>NonHeapMemory max</li>
     * <li>PermGen max</li>
     * </ol>
     * 
     * @return
     */
    public static long[] getMaxSystemParameters() {
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

    /**
     * Return a structure which contains various numeric measures, namely:
     * <ol>
     * <li>HeapMemory used</li>
     * <li>NonHeapMemory used</li>
     * <li>ThreadCount used</li>
     * </ol>
     * 
     * @return
     */
    public static long[] getSystemStatus() {
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

    private static void rollbackTransactionSilent() {
        try {
            Ebean.rollbackTransaction();
        } catch (Exception e) {
        }
    }

    private static void endTransactionSilent() {
        try {
            Ebean.endTransaction();
        } catch (Exception e) {
        }
    }
}
