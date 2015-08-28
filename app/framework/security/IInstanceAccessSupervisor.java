package framework.security;

/**
 * An object which is listening to the login event
 * 
 * @author Pierre-Yves Cloux
 */
public interface IInstanceAccessSupervisor {
    /**
     * Log a login event associated with the specified user
     * 
     * @param uid
     *            a user unique id
     */
    public void logSuccessfulLoginEvent(String uid);

    /**
     * Check of the instance can be accessed.<br/>
     * 
     * @return true if the instance can be accessed
     */
    public boolean checkLoginAuthorized();
}
