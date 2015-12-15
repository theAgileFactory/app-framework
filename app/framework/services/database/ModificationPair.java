package framework.services.database;

/**
 * The modification of an attribute
 * 
 * @author Pierre-Yves
 */
public interface ModificationPair {
    /**
     * The value before the modification of the attribute
     * 
     * @return an object
     */
    public Object getOldValue();

    /**
     * The value after the modification of the attribute
     * 
     * @return an object
     */
    public Object getNewValue();

}