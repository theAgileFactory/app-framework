package framework.services.database;

import com.avaje.ebean.ValuePair;

/**
 * The implementation of the {@link ModificationPair} interface
 * 
 * @author Pierre-Yves Cloux
 */
public class ModificationPairImpl implements ModificationPair {
    private Object oldValue;
    private Object newValue;

    ModificationPairImpl(ValuePair valuePair) {
        if (valuePair != null) {
            this.oldValue = valuePair.getOldValue();
            this.newValue = valuePair.getNewValue();
        }
    }

    public ModificationPairImpl(Object oldValue, Object newValue) {
        super();
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public Object getOldValue() {
        return oldValue;
    }

    @Override
    public Object getNewValue() {
        return newValue;
    }

    @Override
    public String toString() {
        return "ModificationPairImpl [oldValue=" + oldValue + ", newValue=" + newValue + "]";
    }
}
