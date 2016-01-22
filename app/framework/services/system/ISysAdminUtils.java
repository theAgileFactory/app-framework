package framework.services.system;

import akka.actor.Cancellable;
import scala.concurrent.duration.FiniteDuration;

public interface ISysAdminUtils {
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
    public abstract Cancellable scheduleOnce(boolean exclusive, String scheduledActionUuid, FiniteDuration initialDelay, Runnable runnable);

    /**
     * Create a scheduler instance (executed repeatedly until cancellation) with
     * Akka.<br/>
     * Schedules a function to be run repeatedly with an initial delay and a
     * frequency. E.g. if you would like the function to be run after 2 seconds
     * and thereafter every 100ms you would set delay = Duration(2,
     * TimeUnit.SECONDS) and interval = Duration(100, TimeUnit.MILLISECONDS).
     * <br/>
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
    public abstract Cancellable scheduleRecurring(boolean exclusive, String scheduledActionUuid, FiniteDuration initialDelay, FiniteDuration interval,
            Runnable runnable, boolean logInDebug);

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
    public abstract Cancellable scheduleRecurring(boolean exclusive, String scheduledActionUuid, FiniteDuration initialDelay, FiniteDuration interval,
            Runnable runnable);

    /**
     * Log the VM memory and thread configuration (max values).
     * 
     * @param scheduledActionName
     *            the name of an action to which this dump is attached
     */
    public abstract void dumpSystemConfiguration();

    /**
     * Log the VM memory and thread status.
     * 
     * @param eventName
     *            the name of the event attached to this log
     */
    public abstract void dumpSystemStatus(String eventName);

    /**
     * Log the VM memory and thread status.
     * 
     * @param eventName
     *            the name of the event attached to this log
     * @param logAsDebug
     *            if true, log in debug mode otherwise log in info mode
     */
    public abstract void dumpSystemStatus(String eventName, boolean logAsDebug);

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
    public abstract long[] getMaxSystemParameters();

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
    public abstract long[] getSystemStatus();

}