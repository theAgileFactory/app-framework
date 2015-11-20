package framework.services.audit;

import java.io.InputStream;
import java.util.List;

/**
 * The service which is managing the audit log and various other logging
 * features
 */
public interface IAuditLoggerService {
    /**
     * Max number of minutes for changing the log to "DEBUG"
     */
    public static final int MAX_DEBUG_LEVEL_CHANGE = 60;

    /**
     * Change the log level to debug for specified number of minutes
     * 
     * @param minutes
     *            a number of minutes (cannot exceed 1 hour)
     */
    public void changeLogLevelToDebug(int minutes);

    /**
     * Return true if the log level is set to debug
     * 
     * @return a boolean
     */
    public boolean isLogLevelDebug();

    /**
     * Allow to download the current application log for debugging purpose.<br/>
     * Only the current application log is downloaded (meaning the last file
     * since the rotation).
     * 
     * @return an input stream targetting the application log
     */
    public InputStream getApplicationLog();

    /**
     * Reload the audit configuration
     */
    public abstract void reload();

    /**
     * Return the Auditable associated with the specified objectClass
     * 
     * @param objectClass
     *            an Auditable objectClass
     * @return a Auditable instance
     */
    public abstract Auditable getAuditableFromObjectClass(String objectClass);

    /**
     * Return all the {@link Auditable} objects which are not deleted
     * 
     * @return
     */
    public abstract List<Auditable> getAllActiveAuditable();

    /**
     * Save an auditable object to the system (either update or new)
     * 
     * @param auditable
     */
    public abstract void saveAuditable(Auditable auditable);

    /**
     * Delete an auditable object to the system
     * 
     * @param objectClass
     *            an Auditable objectClass
     */
    public abstract void deleteAuditable(String objectClass);

}