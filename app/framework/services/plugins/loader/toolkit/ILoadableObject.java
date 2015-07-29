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
package framework.services.plugins.loader.toolkit;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Define a object to load (a CSV entry).
 * 
 * @author Pierre-Yves Cloux
 * 
 */
public interface ILoadableObject {

    /**
     * Return the row number associated with this loaded object.
     * 
     * @return a long
     */
    public long getSourceRowNumber();

    /**
     * Set the rown number associated with this loaded object.
     * 
     * @param sourceRowNumber
     *            the number of rows
     */
    public void setSourceRowNumber(long sourceRowNumber);

    /**
     * Validate the object and possibly complete it with reference data.
     * 
     * @return a Pair([true if the load was successful],[an error message if it
     *         was not])
     */
    public Pair<Boolean, String> validateAndComplete();

    /**
     * Update or create the object in the database.<br/>
     * Return the id of the created or updated object.
     * 
     * @return a tuple (id of Actor object, identification of the created object
     *         to be displayed in the report) or null (object update)
     */
    public Pair<Long, String> updateOrCreate();
}
