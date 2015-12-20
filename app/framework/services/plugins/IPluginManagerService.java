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
package framework.services.plugins;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import framework.commons.DataType;
import framework.commons.message.EventMessage;
import framework.services.ext.IPluginStopper;
import framework.services.ext.api.IExtensionDescriptor.IPluginConfigurationBlockDescriptor;
import framework.services.ext.api.IExtensionDescriptor.IPluginDescriptor;
import framework.services.plugins.api.IPluginActionDescriptor;
import framework.services.plugins.api.IPluginMenuDescriptor;
import framework.services.plugins.api.PluginException;
import models.framework_models.plugin.PluginConfiguration;
import models.framework_models.plugin.PluginConfigurationBlock;

/**
 * The interface to be implemented by the service which manages the plugin
 * instances.<br/>
 * 
 * @author Pierre-Yves Cloux
 */
public interface IPluginManagerService extends IPluginStopper {

    /**
     * The various status for plugin
     * 
     * @author Pierre-Yves Cloux
     */
    public enum PluginStatus {
        STARTED, // the plugin is started and normally running
        STOPPED, // the plugin is stopped
        STOPPING, // the plugin is being stopped
        STARTING, // the plugin is being started
        START_FAILED// the plugin start failed yet it is possible to attempt to
                    // stop it
    }

    /**
     * An interface that gathers some information about a plugin
     */
    public interface IPluginInfo {
        /**
         * Return the plugin configuration name (as chosen by the end user)
         * 
         * @return
         */
        public String getPluginConfigurationName();

        /**
         * Provide an interface giving access to the plugin status
         * 
         * @return
         */
        public PluginStatus getPluginStatus();

        /**
         * Provide a description of the plugin
         * 
         * @return
         */
        public IPluginDescriptor getDescriptor();

        /**
         * True if the plugin has a custom configurator
         * 
         * @return
         */
        public boolean hasCustomConfigurator();

        /**
         * Return a link to the custom configuration controller default action
         * 
         * @return
         */
        public String getLinkToCustomConfiguration();

        /**
         * Return true if the plugin can be registered with this data type
         * 
         * @param dataType
         *            a data type
         * @return
         */
        public boolean isRegistrableWith(DataType dataType);

        /**
         * Return a link to the default action of the registration controller
         * managing the specified {@link DataType}
         * 
         * @param dataType
         *            a data type
         * @param objectId
         *            an object id (the one which will be registered)
         * @return
         */
        public String getLinkToRegistrationConfiguration(DataType dataType, Long objectId);

        /**
         * Returns a descriptor of plugin menu entry (giving access to a plugin
         * GUI)
         * 
         * @return a plugin menu descriptor
         */
        public IPluginMenuDescriptor getMenuDescriptor();

        /**
         * Returns a map of actions supported by this plugin indexed by their
         * unique identifier
         * 
         * @return a map of actions descriptors
         */
        public Map<String, IPluginActionDescriptor> getActionDescriptors();
    }

    /**
     * Register the specified plugin.<br/>
     * This requires that:
     * <ul>
     * <li>the plugin is available</li>
     * <li>the plugin is not "mono-instance" and already registered</li>
     * </ul>
     * 
     * @param name
     *            the name of the plugin configuration
     * @param pluginDefinitionIdentifier
     *            the unique plugin identifier
     */
    public void registerPlugin(String name, String pluginDefinitionIdentifier) throws PluginException;

    /**
     * UnRegister the specified plugin.<br/>
     * This requires that the plugin is STOPPED.
     * 
     * @param pluginConfigurationId
     *            the unique id for the plugin
     */
    public void unregisterPlugin(Long pluginConfigurationId) throws PluginException;

    /**
     * Return the {@link PluginConfigurationBlock} value associated with the
     * provided identifier
     * 
     * @param pluginConfigurationId
     * @param pluginConfigurationBlockIdentifier
     * @return a pair of (configuration block descriptor, byte array filled with
     *         the value of the block (or a default one if not already filled))
     */
    public Pair<IPluginConfigurationBlockDescriptor, byte[]> getPluginConfigurationBlock(Long pluginConfigurationId, String pluginConfigurationBlockIdentifier)
            throws PluginException;

    /**
     * Update the specified {@link PluginConfigurationBlock} for the specified
     * {@link PluginConfiguration}.
     * 
     * @param pluginConfigurationId
     *            the plugin configuration id
     * @param pluginConfigurationBlockIdentifier
     *            the plugin configuration block identifier
     * @param value
     *            the content of the plugin configuration block
     * @return the descriptor of the updated configuration
     */
    public IPluginConfigurationBlockDescriptor updatePluginConfiguration(Long pluginConfigurationId, String pluginConfigurationBlockIdentifier, byte[] value)
            throws PluginException;

    /**
     * Export the a plugin configuration as XML
     * 
     * @param pluginConfigurationId
     *            the unique id for the plugin
     * @return an XML document
     */
    public String exportPluginConfiguration(Long pluginConfigurationId) throws PluginException;

    /**
     * Export the a plugin configuration as XML
     * 
     * @param pluginConfigurationId
     *            the unique id for the plugin
     * @param configuration
     *            an XML configuration previously exported using the previous
     *            function
     */
    public void importPluginConfiguration(Long pluginConfigurationId, String configuration) throws PluginException;

    /**
     * Return a list of descriptors of the plugins (including the ones which are
     * not available).<br/>
     * Return a map with the following structure:
     * <ul>
     * <li>key : the plugin definition identifier</li>
     * <li>value :
     * <ul>
     * <li>available : true if the plugin is "available" (can be registered)
     * </li>
     * <li>the {@link IStaticPluginRunnerDescriptor} describing the plugin</li>
     * </ul>
     * </li>
     * </ul>
     * 
     * @return a map
     */
    public Map<String, Pair<Boolean, IPluginDescriptor>> getAllPluginDescriptors();

    /**
     * Return true if the plugin is available
     * 
     * @param pluginDefinitionIdentifier
     *            a unique plugin definition identifier
     * @return a boolean
     */
    public boolean isPluginAvailable(String pluginDefinitionIdentifier);

    /**
     * Return the "available" descriptor associated with the specified id.
     * 
     * @param pluginDefinitionIdentifier
     *            a unique plugin definition identifier
     * @return a descriptor or null if no available descriptor can be found
     */
    public IPluginDescriptor getAvailablePluginDescriptor(String pluginDefinitionIdentifier);

    /**
     * Return the descriptor associated with the specified id.
     * 
     * @param pluginDefinitionIdentifier
     *            a unique plugin definition identifier
     * @return a descriptor or null if not descriptor can be found
     */
    public IPluginDescriptor getPluginDescriptor(String pluginDefinitionIdentifier);

    /**
     * Return a map with the following structure:
     * <ul>
     * <li>key : the plugin configuration id</li>
     * <li>value : the {@link IPluginInfo} describing the plugin and its status
     * (as registered)</li>
     * </ul>
     * 
     * @return a map
     */
    public Map<Long, IPluginInfo> getRegisteredPluginDescriptors();

    /**
     * Return the list of plugins which support registrations for the specified
     * type of data. The list is made of "triples" associating:
     * <ul>
     * <li>The plugin configuration id</li>
     * <li>The plugin configuration name</li>
     * <li>The plugin Info (some meta information about the plugin)</li>
     * </ul>
     * Registration is a standard way for a plugin to "filter" the events it
     * received about a certain type of object (only registered plugins are
     * taken into account)
     */
    public List<Triple<Long, String, IPluginInfo>> getPluginSupportingRegistrationForDataType(DataType dataType);

    /**
     * Stop all the plugins which are registered (and started)
     */
    public void stopAll();

    /**
     * Return the status of the plugin:
     * <ul>
     * <li>STARTED : The plugin is started</li>
     * <li>STOPPED : The plugin is stopped</li>
     * <li>STOPPING : The plugin is being stopped (not yet stopped)</li>
     * <li>STARTING : The plugin is being started (not yet started)</li>
     * <li>START_FAILED : the plugin start failed yet it is possible to attempt
     * to stop it</li>
     * </ul>
     * 
     * @param pluginConfigurationId
     *            the unique id for the plugin
     * @return a status
     * @throws PluginException
     *             when the plugin does not exists (not registered)
     */
    public PluginStatus getPluginStatus(Long pluginConfigurationId) throws PluginException;

    /**
     * Start the specified plugin.<br/>
     * This method also change the plugin "autostart" flag so that it will start
     * automatically after system restart.
     * 
     * @param pluginConfigurationId
     *            the plugin configuration id
     */
    public void startPlugin(Long pluginConfigurationId) throws PluginException;

    /**
     * Stop the specified plugin.<br/>
     * This method also change the plugin "autostart" flag so that it will NOT
     * start automatically after system restart.
     * 
     * @param pluginConfigurationId
     *            the id of the plugin configuration
     */
    public void stopPlugin(Long pluginConfigurationId);

    /**
     * Post an event to the IN interface.
     * 
     * @param eventMessage
     *            an event message
     */
    public void postInMessage(EventMessage eventMessage);

    /**
     * Return the small image source associated with the specified plugin
     * definition
     * 
     * @param pluginDefinitionIdentifier
     *            the unique plugin definition identifier
     * @return an input stream to the image
     */
    public InputStream getPluginSmallImageSrc(String pluginDefinitionIdentifier);

    /**
     * Return the big image source associated with the specified plugin
     * definition
     * 
     * @param pluginDefinitionIdentifier
     *            the unique plugin definition identifi
     * @return an input stream to the image
     */
    public InputStream getPluginBigImageSrc(String pluginDefinitionIdentifier);
}
