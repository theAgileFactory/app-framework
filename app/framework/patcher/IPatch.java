package framework.patcher;

/**
 * The interface to be implemented by the patches of a system.
 * 
 * @author Pierre-Yves Cloux
 */
public interface IPatch {
    /**
     * Run the patch code.
     * 
     * @param log
     *            a log to be used to record message
     */
    public void perform(IPatchLog log);
}
