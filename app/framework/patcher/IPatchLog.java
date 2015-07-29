package framework.patcher;

/**
 * The interface to be implemented by the system which is managing the logs for
 * a named system.<br/>
 * This interface is used by the {@link IPatch} patches.
 * 
 * @author Pierre-Yves Cloux
 */
public interface IPatchLog {
    /**
     * Log a message which is informational
     * 
     * @param message
     */
    public void info(String message);

    /**
     * Log a message which is a warning.<br/>
     * This must be used when something noticeable appended during the patching
     * process yet nothing which is considered as a "failed" patch.
     * 
     * @param message
     */
    public void warn(String message);
}
