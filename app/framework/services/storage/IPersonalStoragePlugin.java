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
package framework.services.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The service which manages the personal storage of a user.<br/>
 * This service allow to:
 * <ul>
 * <li><b>Read the content of the storage</b></li>
 * <li><b>Add a file to the content storage</b></li>
 * </ul>
 * <b>NB</b>: the personal content store is cleaned automatically once every 12
 * hours
 * 
 * @author Pierre-Yves Cloux
 */
public interface IPersonalStoragePlugin {
    public static final String IGNORE_FILE_PREFIX = "_";

/**
 * Return a list of {@link File} stored into the personal storage.<br/>
 * The files pre-fixed with the IGNORE_FILE_PREFIX are not listed.
 * @param uid a user unique id (see {@link models.framework_models.account.Principal)
 * @return a list of file
 */
    public File[] getContentView(String uid) throws IOException;

/**
 * Create a new file in the data storage with the specified file.<br/>
 * <b>WARNING</b>:
 * <ul>
 * <li>The command returns an output stream which is to be used to write the data to the file</li>
 * <li>If a file already exists with the same name it is overwritten</li>
 * </ul>
 * @param uid a user unique id (see {@link models.framework_models.account.Principal)
 * @param name the name of the file to create
 * @return a stream open for write
 */
    public OutputStream createNewFile(String uid, String name) throws IOException;

/**
 * Get an input stream on a file in the personal storage folder.<br/>
 * Return "null" if the file does not exists.
 * @param uid a user unique id (see {@link models.framework_models.account.Principal)
 * @param name the name of the file to create
 * @return a stream open for write
 */
    public InputStream readFile(String uid, String name) throws IOException;

/**
 * Move a file existing in another place on the server to the
 * personal file system of the specified user
 * @param uid a user unique id (see {@link models.framework_models.account.Principal)
 * @param absoluteSourceFilePath the path to the specified file (must be in a folder which the system is allowed to read)
 * @param name the name of the file to create in the personal storage
 */
    public void moveFile(String uid, File absoluteSourceFilePath, String name) throws IOException;

/**
 * Delete the specified file in the user personal space
 * @param uid a user unique id (see {@link models.framework_models.account.Principal)
 * @param name the name of the file to delete
 */
    public void deleteFile(String uid, String name) throws IOException;

    /**
     * Get the size (bytes) of personal storage folder.
     */
    public Long getSize();
}
