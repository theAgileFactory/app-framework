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

import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.SubjectPresent;
import framework.commons.DataType;
import framework.services.ServiceManager;
import framework.services.plugins.IPluginManagerService.IPluginInfo;
import framework.services.plugins.api.AbstractCustomConfiguratorController;
import framework.services.plugins.api.AbstractRegistrationConfiguratorController;

/**
 * The controller implementation which redirect an HTTP request toward the right
 * {@link AbstractRegistrationConfiguratorController} or
 * {@link AbstractCustomConfiguratorController}
 * 
 * @author Pierre-Yves Cloux
 */
@SubjectPresent(forceBeforeAuthCheck = true)
public class PluginConfiguratorController extends Controller {
    public PluginConfiguratorController() {
    }

    /**
     * Handle a get request for a custom configurator
     * 
     * @param pluginConfigurationId
     *            the unique id for a plugin configuration
     * @param actionId
     *            the id of an action (will be passed as a parameter to the
     *            controller)
     * @return
     */
    public static Result doGetForCustomConfiguratorController(Long pluginConfigurationId, String actionId) {
        IPluginManagerService pluginManagerService = ServiceManager.getService(IPluginManagerService.NAME, IPluginManagerService.class);
        IPluginInfo pluginInfo = pluginManagerService.getRegisteredPluginDescriptors().get(pluginConfigurationId);
        return pluginInfo.getConfigurator().getCustomConfigurator().doGet(actionId);
    }

    /**
     * Handle a post request for a custom configurator
     * 
     * @param pluginConfigurationId
     *            the unique id for a plugin configuration
     * @param actionId
     *            the id of an action (will be passed as a parameter to the
     *            controller)
     * @return
     */
    public static Result doPostForCustomConfiguratorController(Long pluginConfigurationId, String actionId) {
        IPluginManagerService pluginManagerService = ServiceManager.getService(IPluginManagerService.NAME, IPluginManagerService.class);
        IPluginInfo pluginInfo = pluginManagerService.getRegisteredPluginDescriptors().get(pluginConfigurationId);
        return pluginInfo.getConfigurator().getCustomConfigurator().doPost(actionId);
    }

    /**
     * Handle a get request for a custom configurator
     * 
     * @param pluginConfigurationId
     *            the unique id for a plugin configuration
     * @param dataTypeName
     *            the name of a {@link DataType}
     * @param objectId
     *            the id of the currently edited object
     * @param actionId
     *            the id of an action (will be passed as a parameter to the
     *            controller)
     * @return
     */
    public static Result doGetForRegistrationConfiguratorController(Long pluginConfigurationId, String dataTypeName, Long objectId, String actionId) {
        IPluginManagerService pluginManagerService = ServiceManager.getService(IPluginManagerService.NAME, IPluginManagerService.class);
        IPluginInfo pluginInfo = pluginManagerService.getRegisteredPluginDescriptors().get(pluginConfigurationId);
        return pluginInfo.getConfigurator().getDataTypesWithRegistration().get(DataType.getDataType(dataTypeName)).doGet(objectId, actionId);
    }

    /**
     * Handle a post request for a custom configurator
     * 
     * @param pluginConfigurationId
     *            the unique id for a plugin configuration
     * @param dataTypeName
     *            the name of a {@link DataType}
     * @param objectId
     *            the id of the currently edited object
     * @param actionId
     *            the id of an action (will be passed as a parameter to the
     *            controller)
     * @return
     */
    public static Result doPostForRegistrationConfiguratorController(Long pluginConfigurationId, String dataTypeName, Long objectId, String actionId) {
        IPluginManagerService pluginManagerService = ServiceManager.getService(IPluginManagerService.NAME, IPluginManagerService.class);
        IPluginInfo pluginInfo = pluginManagerService.getRegisteredPluginDescriptors().get(pluginConfigurationId);
        return pluginInfo.getConfigurator().getDataTypesWithRegistration().get(DataType.getDataType(dataTypeName)).doPost(objectId, actionId);
    }
}
