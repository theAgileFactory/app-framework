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

import java.util.Map;

import framework.commons.message.EventMessage;

/**
 * Generic interface for a plugin instance.<br/>
 * A plugin is initialized automatically.<br/>
 * A plugin should require to be injected with the {@link IPluginContext}
 * instance in order to communicate with the container and benefit from various
 * services.
 * 
 * <b>WARNING : the initialization should not allocate any resources (connection
 * to DB for instance). It is mainly to provide the plugin with its context and
 * to notify that its descriptor can be accessed.</b> <br/>
 * 
 * <p>
 * The allocation of ressources must happen when the plugin is started (and the
 * resources must be freed when the plugin is stopped)
 * </p>
 * 
 * The plugin will be run within a container associated with up to 3
 * asynchronous interfaces:
 * <ul>
 * <li>the lifecycle management interface (associated with the methods start and
 * stop) which notifies the plugin with START and STOP events</li>
 * <li>the OUT and IN interfaces (if any since they are optional) which transfer
 * the {@link EventMessage} provided to the plugin</li>
 * </ul>
 * <p>
 * It is important to manage carefully the synchronization (thread safe
 * management) of the plugin. If you "synchronize" each method, a long running
 * OUT method will prevent the plugin from handling an IN message or from being
 * started. However, the contained is not impacted since the messages (either
 * lifecycle or event) are provided to the plugin asynchronously.
 * </p>
 * 
 * @author Pierre-Yves Cloux
 */
public interface IPluginRunner {
    /**
     * Start the plugin with the specified context.
     * 
     * @throws PluginException
     *             if the plugin cannot be started, this will stop automatically
     *             the plugin
     */
    public void start() throws PluginException;

    /**
     * Fully stop a plugin
     */
    public void stop();

    /**
     * A generic interface handling OUT event messages (messages sent from the
     * internal system).<br/>
     * This method will handle the message synchronously and report an error if
     * its fails.
     * 
     * @param eventMessage
     *            a message
     * @throws PluginException
     */
    public void handleOutProvisioningMessage(EventMessage eventMessage) throws PluginException;

    /**
     * A generic interface handling OUT event messages (messages sent from the
     * internal system).<br/>
     * This method will handle the message synchronously and report an error if
     * its fails.
     * 
     * @param eventMessage
     *            a message
     * @throws PluginException
     */
    public void handleInProvisioningMessage(EventMessage eventMessage) throws PluginException;

    /**
     * Returns a descriptor of plugin menu entry (giving access to a plugin GUI)
     * 
     * @return a plugin menu descriptor
     */
    public IPluginMenuDescriptor getMenuDescriptor();

    /**
     * Returns a map of actions supported by this plugin indexed by their unique
     * identifier
     * 
     * @return a map of actions descriptors
     */
    public Map<String, IPluginActionDescriptor> getActionDescriptors();
}