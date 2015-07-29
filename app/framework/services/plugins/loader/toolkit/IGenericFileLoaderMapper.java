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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Generic interface for implementing an object converter.<br/>
 * This class is to be used by the {@link GenericFileLoader}
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <K>
 */
public interface IGenericFileLoaderMapper<K extends ILoadableObject> {
    /**
     * Return the name of the loaded object.<br/>
     * This name is used in the report and must be "human readable"
     */
    public String getLoadedObjectName();

    /**
     * Perform initializations actions.
     * 
     * @throws IOException
     */
    public void init() throws IOException;

    /**
     * Create a new {@link ILoadableObject}.
     * 
     * @return
     */
    public K createNewLoadableObject();

    /**
     * Convert the specified CSV record into the target object.
     * 
     * @param record
     *            the CSV record
     * @param loadableObject
     *            the loadable object
     * @return false if the object must be ignored
     */
    public boolean convert(final CSVRecord record, K loadableObject);

    /**
     * Perform some checks and validation of the loaded objects.<br/>
     * This method is called before "saveInDatabase" and must not persists any
     * data
     * 
     * @param listOfLoadedObjects
     *            a list of loaded objects
     * 
     * @return a Map([the row of the object found invalid],[the error message])
     * @throws IOException
     */
    public Map<Long, String> validate(List<K> listOfLoadedObjects) throws IOException;

    /**
     * Perform an operation before saving all the valid objects.<br/>
     * This method is called within the database transaction which will save the
     * objects.<br/>
     * An exception will trigger a rollback.
     * 
     * @param listOfValidLoadedObjects
     *            list of valid loaded objects
     * 
     * @return a report Pair([Report title],[List of items to be displayed in
     *         the final report])
     * @throws IOException
     */
    public Pair<String, List<String>> beforeSave(List<K> listOfValidLoadedObjects) throws IOException;

    /**
     * Perform an operation after the valid objects are saved.<br/>
     * This method is called within the database transaction which will save the
     * objects.<br/>
     * An exception will trigger a rollback.
     * 
     * @param listOfValidLoadedObjects
     *            list of valid loaded objects
     * 
     * @return a report Pair([Report title],[List of items to be displayed in
     *         the final report])
     * @throws IOException
     */
    public Pair<String, List<String>> afterSave(List<K> listOfValidLoadedObjects) throws IOException;

    /**
     * Perform closing operations.
     */
    public void close();
}
