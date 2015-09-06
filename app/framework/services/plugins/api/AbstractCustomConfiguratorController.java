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

import framework.services.ServiceStaticAccessor;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.ext.ILinkGenerationService;
import framework.services.plugins.api.IPluginContext.HttpMethod;
import play.mvc.Call;
import play.mvc.Result;

/**
 * The class to be extended by the plugin controllers which are managing
 * configuration actions for a plugin (= custom configurator)
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class AbstractCustomConfiguratorController extends AbstractConfiguratorController {
    public AbstractCustomConfiguratorController(ILinkGenerationService linkGenerationService) {
        super(linkGenerationService);
    }

    /**
     * Return the route to the custom configurator
     * 
     * @param method
     *            the HTTP method to be used
     * @param actionId
     *            the action Id to be passed to the controller
     * @return
     */
    public Call getRouteForCustomController(HttpMethod method, String actionId) {
        IImplementationDefinedObjectService implementationDefinedObjectService = ServiceStaticAccessor.getImplementationDefinedObjectService();
        if (method.equals(HttpMethod.GET)) {
            return implementationDefinedObjectService.getRouteForPluginConfiguratorControllerDoGetCustom(getPluginContext().getPluginConfigurationId(),
                    actionId);
        } else {
            return implementationDefinedObjectService.getRouteForPluginConfiguratorControllerDoPostCustom(getPluginContext().getPluginConfigurationId(),
                    actionId);
        }
    }

    /**
     * Handle a get request from a configurator screen
     * 
     * @return
     */
    public Result doGet(String actionId) {
        return badRequest("No implementation available");
    }

    /**
     * Handle a post request from a configuration screen
     * 
     * @return
     */
    public Result doPost(String actionId) {
        return badRequest("No implementation available");
    }
}
