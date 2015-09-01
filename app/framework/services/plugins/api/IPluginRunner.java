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

import framework.commons.message.EventMessage;

/**
 * Generic interface for a plugin instance.<br/>
 * A plugin is first "initialized" by calling the "init" method. This is done
 * when the plugin is registered to the container. <b>WARNING : the
 * initialization not allocate any resources (connection to DB for instance). It
 * is mainly to provide the plugin with its context and to notify that its
 * descriptor can be accessed.</b> The plugin will be run within a container
 * associated with up to 3 asynchronous interfaces:
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
     * The configurator gathers the dynamic configuration features of the
     * plugin.<br/>
     * THIS INTERFACE IS NOT CALLED BEFORE THE PLUGIN IS INITIALIZED. You may
     * implement dynamic data access and other runtime actions.
     */
    public IPluginRunnerConfigurator getConfigurator();

    /**
     * Initialize the plugin.<br/>
     * This method is called only once in the context of the plugin lifecycle.
     * <br/>
     * 
     * @param pluginContext
     *            the context of the plugin (access to various plugin services)
     *            <b>WARNING</b> : the init phase should just ensure that the
     *            {@link IStaticPluginRunnerDescriptor} is appropriately
     *            initialized.<br/>
     *            <b>You must not lead any external resources during this
     *            phase</b>
     */
    public void init(IPluginContext pluginContext) throws PluginException;

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
}