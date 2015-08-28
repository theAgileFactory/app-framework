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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.tuple.Pair;

import framework.commons.DataType;
import framework.commons.message.EventMessage;
import models.framework_models.plugin.PluginConfigurationBlock;

/**
 * The plugin context provides each plugin with some services, namely:
 * <ul>
 * <li>Logging services : logging events to the system or to the end user</li>
 * <li>State storage : the ability to store some persistent data</li>
 * <li>Object registration check : the ability to check is an internal BizDock
 * object is registered to the plugin (or not)</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
public interface IPluginContext {
    /**
     * The log levels supported for the monitoring
     * 
     * @author Pierre-Yves Cloux
     */
    public enum LogLevel {
        DEBUG, INFO, ERROR
    }

    /**
     * The HTTP method for the configurator controller
     * 
     * @author Pierre-Yves Cloux
     */
    public enum HttpMethod {
        GET, POST
    }

    /**
     * Returns the plugin configuration name (the instance of the plugin
     * definition)
     * 
     * @return a name
     */
    public String getPluginConfigurationName();

    /**
     * Log a message into the plugin "technical log".<br/>
     * This log is not visible from the customer. It is to be used to monitor
     * the plugin and possibly to troubleshoot some issues.
     * 
     * @param level
     *            the level of the log
     * @param message
     *            a text message
     */
    public void log(LogLevel level, String message);

    /**
     * Log a message into the plugin "technical log".<br/>
     * This log is not visible from the customer. It is to be used to monitor
     * the plugin and possibly to troubleshoot some issues.
     * 
     * @param level
     *            the level of the log
     * @param message
     *            a text message
     * @param exception
     *            an exception
     */
    public void log(LogLevel level, String message, Exception exception);

    /**
     * Find the internal Id associated with the specified externalId.
     * 
     * @param externalId
     *            an external Id as String
     * @param linkType
     *            a type of link
     * @return the internal Id or null if it is not found
     * @throws RuntimeException
     *             if more than one link is found
     */
    public Long getUniqueInternalId(String externalId, String linkType);

    /**
     * Find the external id associated with the specified internal id.
     * 
     * @param internalId
     *            an internal Id as Long
     * @param linkType
     *            a type of link
     * @return the external Id or null if it is not found
     * @throws RuntimeException
     *             if more than one link is found
     */
    public String getUniqueExternalId(Long internalId, String linkType);

    /**
     * Find the internal Id associated with the specified externalId.
     * 
     * @param externalId
     *            an external Id as String
     * @param linkType
     *            a type of link
     * @return the internal Ids or null if it is not found
     */
    public List<Long> getMultipleInternalId(String externalId, String linkType);

    /**
     * Find the external id associated with the specified internal id.
     * 
     * @param internalId
     *            an internal Id as Long
     * @param linkType
     *            a type of link
     * @return the external Ids or null if it is not found
     */
    public List<String> getMultipleExternalId(Long internalId, String linkType);

    /**
     * Find the internal ID associated with the specified externalId and parent
     * link.
     * 
     * @param externalId
     *            the external id
     * @param linkType
     *            the link type
     * @param parentInternalId
     *            the internal id of the parent link
     * @param parentExternalId
     *            the external id of the parent link
     * @param parentLinkType
     *            the link type of the parent link
     */
    public Long getUniqueInternalIdWithParent(String externalId, String linkType, Long parentInternalId, String parentExternalId, String parentLinkType);

    /**
     * Get the internal and external ID of the children of a link.
     * 
     * @param internalId
     *            the internal ID of the parent link
     * @param externalId
     *            the external ID of the parent link
     * @param linkType
     *            the link type of the parent link
     * @parm childLinkType the link type
     */
    public List<Pair<Long, String>> getChildrenIdOfLink(Long internalId, String externalId, String linkType, String childLinkType);

    /**
     * Create an association between an internal and an external id. Only if
     * there is not already one.
     * 
     * @param internalId
     *            an internal Id as Long
     * @param externalId
     *            an external Id as String
     * @param linkType
     *            a type of link
     * @throws PluginException
     *             if a link already partially exists : same linkType but
     *             different externalId or internalId
     */
    public void createOneToOneLink(Long internalId, String externalId, String linkType) throws PluginException;

    /**
     * Create an association between an internal and an external id
     * 
     * @param internalId
     *            an internal Id as Long
     * @param externalId
     *            an external Id as String
     * @param linkType
     *            a type of link
     * @throws PluginException
     *             if a link already partially exists : same linkType but
     *             different externalId or internalId
     */
    public void createLink(Long internalId, String externalId, String linkType) throws PluginException;

    /**
     * Create an association between an internal and an external id and for
     * which the association has a parent.
     * 
     * @param internalId
     *            the internal id
     * @param externalId
     *            the external id
     * @param linkType
     *            the link type
     * @param parentInternalId
     *            the internal id of the parent link
     * @param parentExternalId
     *            the external id of the parent link
     * @param parentLinkType
     *            the link type of the parent link
     */
    public void createLink(Long internalId, String externalId, String linkType, Long parentInternalId, String parentExternalId, String parentLinkType)
            throws PluginException;

    /**
     * Delete an association between an internal and an external id
     * 
     * @param internalId
     *            an internal Id as Long
     * @param externalId
     *            an external Id as String
     * @param linkType
     *            a type of link
     * @return false if the link was not found
     */
    public boolean deleteOneToOneLink(Long internalId, String externalId, String linkType);

    /**
     * Delete a unique link.
     * 
     * @param internalId
     *            the internal ID of the unique link
     * @param externalId
     *            the external ID of the unique link
     * @param linkType
     *            the link type of the unique link
     */
    public void deleteLink(Long internalId, String externalId, String linkType);

    /**
     * Report a message at plugin startup
     * 
     * @param isError
     *            true if the message is an error
     * @param logMessage
     *            a message
     */
    public void reportOnStartup(boolean isError, String logMessage);

    /**
     * Report a message at plugin startup
     * 
     * @param isError
     *            true if the message is an error
     * @param logMessage
     *            a message
     * @param exception
     *            an Exception
     */
    public void reportOnStartup(boolean isError, String logMessage, Exception exception);

    /**
     * Report a status about the the handling of an event.<br/>
     * Such trace will be displayed to the end user.<br/>
     * It should give an understandable information about the status of the
     * plugin.<br/>
     * The log should also (yet it is optional) contains the Ids of the source &
     * target objects handled by the transaction associated with this log
     * 
     * @param transactionId
     *            the unique Id of the transaction
     * @param isError
     *            true if the log is resulting from an error
     * @param eventMessageType
     *            the type of the Message which was associated with this log
     * @param logMessage
     *            a text message which will be visible from the end user
     */
    public void reportOnEventHandling(String transactionId, boolean isError, EventMessage eventMessage, String logMessage);

    /**
     * Report a status about the the handling of an event.<br/>
     * Such trace will be displayed to the end user.<br/>
     * It should give an understandable information about the status of the
     * plugin.<br/>
     * The log should also (yet it is optional) contains the Ids of the source &
     * target objects handled by the transaction associated with this log
     * 
     * @param transactionId
     *            the unique Id of the transaction
     * @param isError
     *            true if the log is resulting from an error
     * @param eventMessageType
     *            the type of the Message which was associated with this log
     * @param logMessage
     *            a text message which will be visible from the end user
     * @param exception
     *            an exception
     */
    public void reportOnEventHandling(String transactionId, boolean isError, EventMessage eventMessage, String logMessage, Exception exception);

    /**
     * Store the plugin state into a persistent storage
     * 
     * @param stateObject
     *            an object which must be {@link Serializable}
     */
    public void setState(Object stateObject) throws PluginException;

    /**
     * Retrieve the state of the plugin from the persistent storage
     * 
     * @return the state object
     */
    public Object getState() throws PluginException;

    /**
     * Return the configuration content associated with the specified
     * configuration block Id
     * 
     * @param configurationBlockDescriptor
     *            a configuration block descriptor
     * @param defaultIfNull
     *            return the default configuration if the database does not
     *            contain any value yet
     * @return a Pair of (boolean: true if the version of the stored
     *         configuration is too old, configuration data)
     */
    public Pair<Boolean, byte[]> getConfiguration(IPluginConfigurationBlockDescriptor configurationBlockDescriptor, boolean defaultIfNull)
            throws PluginException;

    /**
     * WARNING : only for PROPERTIES configuration block Return the
     * configuration content associated with the specified configuration block
     * Id. If a stored configuration is found and is older than the current
     * version, it merges it with the default one and save it back to the
     * database. If no configuration is found, then the default one is returned.
     * 
     * @param configurationBlockDescriptor
     *            a configuration block descriptor
     * @return configuration data
     */
    public byte[] getConfigurationAndMergeWithDefault(IPluginConfigurationBlockDescriptor configurationBlockDescriptor) throws PluginException;

    /**
     * Save the specified configuration in the database
     * 
     * @param configurationBlockDescriptor
     * @param configuration
     * @throws PluginException
     */
    public void setConfiguration(IPluginConfigurationBlockDescriptor configurationBlockDescriptor, byte[] configuration) throws PluginException;

    /**
     * Return true if the specified BizDock object is registered with this
     * plugin
     * 
     * @param objectType
     *            a type of BizDock object
     * @param objectId
     *            the id of an object
     */
    public boolean isRegistered(DataType dataType, Long objectId);

    /**
     * Remove the registration for a data type and an object id.
     * 
     * @param dataType
     *            the data type
     * @param objectId
     *            the object id
     */
    public void removeRegistration(DataType dataType, Long objectId);

    /**
     * Return the registration configuration for the specified object
     * 
     * @param objectType
     *            a type of BizDock object
     * @param objectId
     *            the id of an object
     * @return
     */
    public byte[] getRegistrationConfiguration(DataType dataType, Long objectId);

    /**
     * Set the configuration for a registration for the specified object.
     * 
     * @param dataType
     *            the data type
     * @param objectId
     *            the object id
     * @param configuration
     *            the configuration
     */
    public void setRegistrationConfiguration(DataType dataType, Long objectId, byte[] configuration);

    /**
     * Delete all the links for the specified plugin. WARNING: no recovery for
     * this. Please use only in case resynchronization
     */
    public void flushAllLinks();

    /**
     * To be called if I need to stop.<br/>
     * This method should only be called if the plugin repeatedly fails.
     */
    public void killMe();

    /**
     * Post an event to the OUT interface of a plugin (could be the current
     * plugin or anything else).
     * 
     * @param eventMessage
     *            an event message
     */
    public void postOutMessage(EventMessage eventMessage);

    /**
     * Get a properties object from an array of bytes (probably extracted from a
     * {@link PluginConfigurationBlock})
     * 
     * @param rawConfigurationBlock
     *            an array of bytes
     * @return a properties object
     * @throws PluginException
     */
    public PropertiesConfiguration getPropertiesConfigurationFromByteArray(byte[] rawConfigurationBlock) throws PluginException;

    /**
     * Return an input stream pointing to a file on the local storage (sFTP
     * storage).<br/>
     * 
     * @param filePath
     *            the path in the local storage
     * @return an input stream
     */
    public InputStream getFileFromSharedStorage(String filePath) throws IOException;

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
    public OutputStream writeFileInSharedStorage(String filePath, boolean overwrite) throws IOException;

    /**
     * Delete the specified file.<br/>
     * 
     * @param filePath
     *            the path of the file in the local storage
     */
    public void deleteFileInSharedStorage(String filePath) throws IOException;

    /**
     * Lists the files in a named folder
     * 
     * @param directoryPath
     *            a directory path
     * @return a list of file names
     * @throws IOException
     */
    public String[] getFileListInSharedStorage(String directoryPath) throws IOException;

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
    public void renameFileInSharedStorage(String sourceFilePath, String targetFilePath) throws IOException;

    /**
     * Move a file into a named folder
     * 
     * @param sourceFilePath
     *            the path to the source file
     * @param targetFolderPath
     *            the path to the target folder
     */
    public void moveFileInSharedStorage(String sourceFilePath, String targetFolderPath) throws IOException;
}
