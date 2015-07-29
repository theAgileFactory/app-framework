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

/**
 * The interface to be implemented by the various ValueHolders which are used by
 * multiple types of visual components (picker, dropdownlists, etc.)
 * 
 * @param <T>
 *            the type of the value in the value holder
 * @author Pierre-Yves Cloux
 */
public interface ISelectableValueHolder<T> {
    /**
     * Return the name of the value (what is displayed to the end user)
     */
    public String getName();

    /**
     * Return an optional URL to get some information about the value holder.
     */
    public String getUrl();

    /**
     * Return a description for the value (may be null)
     */
    public String getDescription();

    /**
     * Return a String representation of the value.<br/>
     * This one is to be used in the templates for visual representation.
     */
    public T getValue();

    /**
     * Return true if the value holder is selectable
     */
    public boolean isSelectable();

    /**
     * Return true if the value holder is deleted
     */
    public boolean isDeleted();

    /**
     * Setter for the URL
     */
    public void setUrl(String url);

    /**
     * the compare to method
     */
    public int compareTo(Object o);
}
