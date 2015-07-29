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
 * The interface to be implemented by the service which allow to access the
 * instance shared storage.<br/>
 * This shared storage might also be accessible through sFTP on some
 * configuration.
 * 
 * @author Pierre-Yves Cloux
 * 
 */
public interface ISharedStorageService {
    public static final String NAME = "sharedStorageService";

    /**
     * Return a file reference to a file from the shared storage.<br/>
     * 
     * @param filePath
     *            the path in the local storage
     * @return an input stream
     */
    public File getFile(String filePath) throws IOException;

    /**
     * Return an input stream pointing to a file on the local storage (sFTP
     * storage).<br/>
     * 
     * @param filePath
     *            the path in the local storage
     * @return an input stream
     */
    public InputStream getFileAsStream(String filePath) throws IOException;

    /**
     * Get an outputstream to write in the specified location.<br/>
     * 
     * @param filePath
     *            the path in the local storage
     * @param overwrite
     *            if true any existing file will be overwritten (otherwise an
     *            exception is thrown)
     * @return an outputstream
     */
    public OutputStream writeFile(String filePath, boolean overwrite) throws IOException;

    /**
     * Create a new folder at the specified location
     * 
     * @param directoryPath
     *            the path to the folder to create
     * @throws IOException
     */
    public void createNewFolder(String directoryPath) throws IOException;

    /**
     * Delete the specified file.<br/>
     * 
     * @param filePath
     *            the path of the file in the local storage
     */
    public void deleteFile(String filePath) throws IOException;

    /**
     * Delete the specified folder.<br/>
     * 
     * @param directoryPath
     *            the path of the folder in the local storage
     */
    public void deleteFolder(String directoryPath) throws IOException;

    /**
     * Lists the files in a named folder
     * 
     * @param directoryPath
     *            a directory path
     * @return a list of file names
     * @throws IOException
     */
    public String[] getFileList(String directoryPath) throws IOException;

    /**
     * Rename a file in one folder to another name.<br/>
     * You can only rename a file in the same folder
     * 
     * @param sourceFilePath
     *            the path to the source file
     * @param targetFilePath
     *            the path to the target file
     * @throws IOException
     */
    public void rename(String sourceFilePath, String targetFilePath) throws IOException;

    /**
     * Copy a file to another file
     * 
     * @param sourceFilePath
     *            the path to the source file
     * @param targetFilePath
     *            the path to the target file
     */
    public void copy(String sourceFilePath, String targetFilePath) throws IOException;

    /**
     * Move a file into a named folder
     * 
     * @param sourceFilePath
     *            the path to the source file
     * @param targetFolderPath
     *            the path to the target folder
     */
    public void move(String sourceFilePath, String targetFolderPath) throws IOException;

    /**
     * Get the size (bytes) of the shared storage folder.
     */
    public Long getSize();
}
