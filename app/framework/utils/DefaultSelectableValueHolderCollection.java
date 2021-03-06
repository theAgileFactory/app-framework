/*! LICENSE
 *
 * Copyright (c) 2015, The Agile Factory SA and/or its affiliates. All rights
 * reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package framework.utils;

import java.util.*;

/**
 * The default implementation for {@link ISelectableValueHolderCollection}.<br/>
 * This collection is thread safe.
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <T>
 */
public class DefaultSelectableValueHolderCollection<T> implements ISelectableValueHolderCollection<T> {
    private Map<T, ISelectableValueHolder<T>> valueHoldersMap = new HashMap<>();

    /**
     * Default constructor
     */
    public DefaultSelectableValueHolderCollection() {
    }

    /**
     * Creates a {@link DefaultSelectableValueHolderCollection} from the
     * specified list.<br/>
     * <b>WARNING</b>: we assume that the list contains Objects which are
     * ISelectableValueHolder of the right type otherwise an Exception will be
     * raised<br/>
     * <b>WARNING</b>: any later modification of the list will not be taken into
     * account
     * 
     * @param valueHolderList the source list
     */
    public DefaultSelectableValueHolderCollection(@SuppressWarnings("rawtypes") final Collection valueHolderList) {
        if (valueHolderList != null) {
            for (Object valueHolderAsObject : valueHolderList) {
                @SuppressWarnings("unchecked")
                ISelectableValueHolder<T> valueHolder = (ISelectableValueHolder<T>) valueHolderAsObject;
                getValueHoldersMap().put(valueHolder.getValue(), valueHolder);
            }
        }
    }

    /**
     * Creates a {@link DefaultSelectableValueHolderCollection} with only one
     * value holder.
     * 
     * @param valueHolder the initial value holder
     */
    public DefaultSelectableValueHolderCollection(ISelectableValueHolder<T> valueHolder) {
        if (valueHolder != null) {
            getValueHoldersMap().put(valueHolder.getValue(), valueHolder);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * framework.utils.ISelectableValueHolderCollection#remove(framework.utils
     * .ISelectableValueHolder)
     */
    @Override
    public synchronized void remove(ISelectableValueHolder<T> valueHolder) {
        remove(valueHolder.getValue());
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.utils.ISelectableValueHolderCollection#remove(T)
     */
    @Override
    public synchronized void remove(T value) {
        if (getValueHoldersMap().containsKey(value)) {
            getValueHoldersMap().remove(value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * framework.utils.ISelectableValueHolderCollection#add(framework.utils.
     * ISelectableValueHolder)
     */
    @Override
    public synchronized void add(ISelectableValueHolder<T> valueHolder) {
        getValueHoldersMap().put(valueHolder.getValue(), valueHolder);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * framework.utils.ISelectableValueHolderCollection#addAll(framework.utils.
     * ISelectableValueHolder)
     */
    @Override
    public void addAll(ISelectableValueHolderCollection<T> collection) {
        collection.getValues().stream().forEach(this::add);
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.utils.ISelectableValueHolderCollection#getValues()
     */
    @Override
    public synchronized Collection<ISelectableValueHolder<T>> getValues() {
        return getValueHoldersMap().values();
    }

    @Override
    public synchronized List<ISelectableValueHolder<T>> getSortedValues() {
        List<ISelectableValueHolder<T>> list = new ArrayList<>(getValueHoldersMap().values());
        Collections.sort(list, ISelectableValueHolder::compareTo);
        return list;
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.utils.ISelectableValueHolderCollection#getValueHolder(T)
     */
    @Override
    public synchronized ISelectableValueHolder<T> getValueHolder(T value) {
        return getValueHoldersMap().get(value);
    }

    @Override
    public synchronized boolean containsValue(T value) {
        return getValueHoldersMap().containsKey(value);
    }

    @Override
    public synchronized ISelectableValueHolderCollection<T> substract(List<T> values) {
        ISelectableValueHolderCollection<T> substractCollection = new DefaultSelectableValueHolderCollection<>();
        if (values != null) {
            getValues().stream().filter(valueHolder -> !values.contains(valueHolder.getValue())).forEach(substractCollection::add);
        }
        return substractCollection;
    }

    @Override
    public synchronized ISelectableValueHolderCollection<T> getSubCollection(List<T> values) {
        ISelectableValueHolderCollection<T> subCollection = new DefaultSelectableValueHolderCollection<>();
        if (values != null) {
            for (T value : values) {
                if (containsValue(value)) {
                    subCollection.add(getValueHolder(value));
                } else {
                    throw new IllegalArgumentException("Unknown value holder for value " + value);
                }
            }
        }
        return subCollection;
    }

    @Override
    public Set<T> getValuesValueSet() {
        return getValueHoldersMap().keySet();
    }

    /**
     * A map of {@link ISelectableValueHolder}
     */
    private Map<T, ISelectableValueHolder<T>> getValueHoldersMap() {
        return valueHoldersMap;
    }
}
