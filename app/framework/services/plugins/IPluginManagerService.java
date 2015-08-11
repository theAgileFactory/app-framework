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
import framework.services.actor.IActorServiceLifecycleHook;
import framework.services.ext.IExtension;
import framework.services.plugins.api.IPluginRunner;
import framework.services.plugins.api.IPluginRunnerConfigurator;
import framework.services.plugins.api.IStaticPluginRunnerDescriptor;
import framework.services.plugins.api.PluginException;

/**
 * The interface to be implemented by the service which manages the plugin
 * instances.<br/>
 * 
 * @author Pierre-Yves Cloux
 */
public interface IPluginManagerService extends IActorServiceLifecycleHook {

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
        public PluginStatus getPluginStatus();

        public IStaticPluginRunnerDescriptor getStaticDescriptor();

        public IPluginRunnerConfigurator getConfigurator();

        public String getPluginSmallImage();

        public String getPluginBigImage();
    }

    /**
     * Load plugin in the manager.<br/>
     * Loading a plugin will create an entry into the PluginDefinition table so
     * that later a plugin can be registered with such definition.<br/>
     * If the plugin definition record already exists, it is kept "as is".
     * 
     * @param pluginExtension
     *            a plugin extension
     * @param identifier
     *            a unique plugin identifier
     * @param clazz
     *            a {@link IPluginRunner} class name
     * @param isAvailable
     *            true if the plugin will be available for creation
     * @throws PluginException
     */
    public void loadPluginExtension(IExtension pluginExtension, String identifier, String clazz, boolean isAvailable) throws PluginException;

    /**
     * Register the specified plugin.<br/>
     * This requires that:
     * <ul>
     * <li>the plugin is not already registered</li>
     * <li>the plugin is available</li>
     * </ul>
     * 
     * @param pluginConfigurationId
     *            the unique id for the plugin
     */
    public void registerPlugin(Long pluginConfigurationId) throws PluginException;

    /**
     * UnRegister the specified plugin.<br/>
     * This requires that the plugin is STOPPED.
     * 
     * @param pluginConfigurationId
     *            the unique id for the plugin
     */
    public void unregisterPlugin(Long pluginConfigurationId) throws PluginException;

    /**
     * Return a list of descriptors of the plugins (including the ones which are
     * not available).<br/>
     * Return a map with the following structure:
     * <ul>
     * <li>
     * <ul>
     * <li>key : the plugin definition identifier</li>
     * <li>available : true if the plugin is "available" (can be registered)</li>
     * </ul>
     * </li>
     * <li>value : the {@link IStaticPluginRunnerDescriptor} describing the
     * plugin</li>
     * </ul>
     * 
     * @return a map
     */
    public Map<Pair<String, Boolean>, IStaticPluginRunnerDescriptor> getAllPluginDescriptors();

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
    public IStaticPluginRunnerDescriptor getAvailablePluginDescriptor(String pluginDefinitionIdentifier);

    /**
     * Return the descriptor associated with the specified id.
     * 
     * @param pluginDefinitionIdentifier
     *            a unique plugin definition identifier
     * @return a descriptor or null if not descriptor can be found
     */
    public IStaticPluginRunnerDescriptor getPluginDescriptor(String pluginDefinitionIdentifier);

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
     * Start the specified plugin
     * 
     * @param pluginConfigurationId
     *            the plugin configuration id
     */
    public void startPlugin(Long pluginConfigurationId) throws PluginException;

    /**
     * Stop the specified plugin
     * 
     * @param pluginConfigurationId
     *            the id of the plugin configuration
     */
    public void stopPlugin(Long pluginConfigurationId);

    /**
     * Post an event to the OUT interface.
     * 
     * @param eventMessage
     *            an event message
     */
    public void postOutMessage(EventMessage eventMessage);

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
