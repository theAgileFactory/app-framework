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
package models.framework_models.parent;

import com.avaje.ebean.Model;

/**
 * The interface to be implemented by the data objects from the framework. It
 * provides a number of mandatory features.
 * 
 * @author Pierre-Yves Cloux
 */
public interface IModel {
    /**
     * Dump the state of the object for audit purpose
     * 
     * @param a
     *            String representation of the object
     */
    public String audit();

    /**
     * Initialize the Model object with some default values
     */
    public void defaults();

    /**
     * Delete the object according to the chosen strategy.<br/>
     * This might be:
     * <ul>
     * <li>A real delete (with a call to the delete() method)</li>
     * <li>A virtual delete by setting the object as "deleted" in the database</li>
     * </ul>
     * Here is the code to be usually set in this field for the
     * "virtual deletion":
     * 
     * <pre>
     * deleted = true;
     * </pre>
     * 
     * Obviously the following field must be defined in the {@link Model} class:
     * 
     * <pre>
     * public boolean deleted = false;
     * </pre>
     **/
    public void doDelete();

}
