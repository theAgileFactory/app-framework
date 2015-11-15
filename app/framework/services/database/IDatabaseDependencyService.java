package framework.services.database;

/**
 * The interface to a service which is ensuring that the database is activated
 * before the services are activated
 * 
 * @author Pierre-Yves Cloux
 */
public interface IDatabaseDependencyService {
    /**
     * Register a database change listener.<br/>
     * <b>WARNING</b>: the handling of this event must be "unblocking" to avoid
     * any impact on the database performance.
     * 
     * @param listener
     *            a listener
     */
    public void addDatabaseChangeListener(IDatabaseChangeListener listener);

    /**
     * Unregister a database change listener<br/>
     * <b>WARNING</b>: the handling of this event must be "unblocking" to avoid
     * any impact on the database performance.
     * 
     * @param listener
     *            a listener
     */
    public void removeDatabaseChangeListener(IDatabaseChangeListener listener);
}