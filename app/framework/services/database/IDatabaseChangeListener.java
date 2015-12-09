package framework.services.database;

import java.util.Set;

/**
 * An interface which can be registered to listen for database changes (INSERT,
 * DELETE, UPDATE).<br/>
 * Warning: please ensure that the events are handled "fast enough" otherwise
 * the events may be discarded to avoid any performance impacts on the system.
 * 
 * @author Pierre-Yves Cloux
 */
public interface IDatabaseChangeListener {
    /**
     * Notify an INSERT on the specified object
     * 
     * @param bean
     *            the object itself
     */
    public void postInsert(Object bean);

    /**
     * Notify a DELETE on the specified object
     * 
     * @param bean
     *            the object itself
     */
    public void postDelete(Object bean);

    /**
     * Notify an UPDATE on the specified object
     * 
     * @param bean
     *            the object itself
     * @param modifiedAttributes
     *            the attributes modified by the update
     */
    public void postUpdate(Object bean, Set<String> modifiedAttributes);
}
