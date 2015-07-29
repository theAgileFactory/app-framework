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
package framework.services.plugins.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import models.framework_models.plugin.PluginConfigurationBlock;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import framework.services.ServiceManager;
import framework.services.storage.ISharedStorageService;

/**
 * An utility class dedicated to plugins
 * 
 * @author Pierre-Yves Cloux
 */
public class PluginUtils {

    public PluginUtils() {
    }

    /**
     * Get a properties object from an array of bytes (probably extracted from a
     * {@link PluginConfigurationBlock})
     * 
     * @param rawConfigurationBlock
     *            an array of bytes
     * @return a properties object
     * @throws PluginException
     */
    public static PropertiesConfiguration getPropertiesConfigurationFromByteArray(byte[] rawConfigurationBlock) throws PluginException {
        if (rawConfigurationBlock == null || rawConfigurationBlock.length == 0) {
            return new PropertiesConfiguration();
        }
        PropertiesConfiguration properties = new PropertiesConfiguration();
        try {
            properties.load(new ByteArrayInputStream(rawConfigurationBlock));
        } catch (ConfigurationException e) {
            throw new PluginException("Unable to parse the configuration", e);
        }
        return properties;
    }

    /**
     * Return an input stream pointing to a file on the local storage (sFTP
     * storage).<br/>
     * 
     * @param filePath
     *            the path in the local storage
     * @return an input stream
     */
    public static InputStream getFile(String filePath) throws IOException {
        ISharedStorageService sharedStorageService = ServiceManager.getService(ISharedStorageService.NAME, ISharedStorageService.class);
        return sharedStorageService.getFileAsStream(filePath);
    }

    /**
     * Get an outputstream to write in.<br/>
     * It is not possible to overwrite an existing file.
     * 
     * @param filePath
     *            the path in the local storage
     * @param overwrite
     *            if true any existing file will be overwritten (otherwise an
     *            exception is thrown)
     * @return an outputstream
     */
    public static OutputStream writeFile(String filePath, boolean overwrite) throws IOException {
        ISharedStorageService sharedStorageService = ServiceManager.getService(ISharedStorageService.NAME, ISharedStorageService.class);
        return sharedStorageService.writeFile(filePath, overwrite);
    }

    /**
     * Delete the specified file.<br/>
     * 
     * @param filePath
     *            the path of the file in the local storage
     */
    public static void deleteFile(String filePath) throws IOException {
        ISharedStorageService sharedStorageService = ServiceManager.getService(ISharedStorageService.NAME, ISharedStorageService.class);
        sharedStorageService.deleteFile(filePath);
    }

    /**
     * Lists the files in a named folder
     * 
     * @param directoryPath
     *            a directory path
     * @return a list of file names
     * @throws IOException
     */
    public static String[] getFileList(String directoryPath) throws IOException {
        ISharedStorageService sharedStorageService = ServiceManager.getService(ISharedStorageService.NAME, ISharedStorageService.class);
        return sharedStorageService.getFileList(directoryPath);
    }

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
    public static void rename(String sourceFilePath, String targetFilePath) throws IOException {
        ISharedStorageService sharedStorageService = ServiceManager.getService(ISharedStorageService.NAME, ISharedStorageService.class);
        sharedStorageService.rename(sourceFilePath, targetFilePath);
    }

    /**
     * Move a file into a named folder
     * 
     * @param sourceFilePath
     *            the path to the source file
     * @param targetFolderPath
     *            the path to the target folder
     */
    public static void move(String sourceFilePath, String targetFolderPath) throws IOException {
        ISharedStorageService sharedStorageService = ServiceManager.getService(ISharedStorageService.NAME, ISharedStorageService.class);
        sharedStorageService.move(sourceFilePath, targetFolderPath);
    }
}
