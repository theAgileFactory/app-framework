package framework.services.audit;

import java.util.List;

/**
 * The service which is managing the audit log
 */
public interface IAuditLoggerService {

    /**
     * Log the creation of an entity
     * 
     * @param entity
     */
    public abstract void logCreate(Object entity);

    /**
     * Log the update of an entity
     * 
     * @param entity
     */
    public abstract void logUpdate(Object entity);

    /**
     * Log the deletion of an entity
     * 
     * @param entity
     */
    public abstract void logDelete(Object entity);

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