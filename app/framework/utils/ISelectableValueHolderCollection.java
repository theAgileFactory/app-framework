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

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * An interface to be used with {@link ISelectableValueHolder}.<br/>
 * This interface is to be implemented by a class (such as
 * {@link DefaultSelectableValueHolderCollection}) which provides a simply mean
 * for indexing a list of {@link ISelectableValueHolder}.
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <T>
 */
public interface ISelectableValueHolderCollection<T> {
    /**
     * Add a new value holder to the collection
     * 
     * @param valueHolder
     *            a value holder
     */
    public abstract void add(ISelectableValueHolder<T> valueHolder);

    /**
     * Remove the specified value holder from the collection
     * 
     * @param valueHolder
     *            a value holder
     */
    public abstract void remove(ISelectableValueHolder<T> valueHolder);

    /**
     * Remove the specified value holder associated to the specified value from
     * the collection
     * 
     * @param valueHolder
     *            a value holder
     */
    public abstract void remove(T value);

    /**
     * Get the {@link List} of {@link ISelectableValueHolder}
     * 
     * @return a list of value holders
     */
    public abstract Collection<ISelectableValueHolder<T>> getValues();

    /**
     * Get the {@link List} of {@link ISelectableValueHolder}
     * 
     * @return a sorted list of value holders
     */
    public abstract List<ISelectableValueHolder<T>> getSortedValues();

    /**
     * Get the value holder which is specifically associated with this value
     * 
     * @param value
     *            a value
     * @return the corresponding value holder (or null if not found)
     */
    public abstract ISelectableValueHolder<T> getValueHolder(T value);

    /**
     * Return a collection created from the current collection which contains
     * the {@link ISelectableValueHolder} which value match the ones passed as a
     * parameter
     * 
     * @param values
     *            some values
     * @return a new collection
     */
    public abstract ISelectableValueHolderCollection<T> getSubCollection(List<T> values);

    /**
     * Return a collection created from the current collection which contains
     * all the value holders of the current collection "minus" the ones which
     * value match the ones passed as a parameter ones passed as a parameter
     * 
     * @param values
     *            some values
     * @return a new collection
     */
    public abstract ISelectableValueHolderCollection<T> substract(List<T> values);

    /**
     * Returns true if a {@link ISelectableValueHolder} with the specified value
     * can be found
     * 
     * @param value
     *            a value
     * @return a boolean
     */
    public abstract boolean containsValue(T value);

    /**
     * Return a list of values from the {@link ISelectableValueHolder}
     * 
     * @return a list of value from the value holders of the collection
     */
    public abstract Set<T> getValuesValueSet();
}