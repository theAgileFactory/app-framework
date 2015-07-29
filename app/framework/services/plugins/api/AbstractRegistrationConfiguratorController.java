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

import play.mvc.Call;
import play.mvc.Result;
import framework.commons.DataType;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.configuration.ImplementationDefineObjectServiceFactory;
import framework.services.plugins.api.IPluginContext.HttpMethod;

/**
 * The class to be extended by the plugin controllers which are managing
 * configuration actions for the registration. See
 * {@link IPluginRegistrationConfigurator}
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class AbstractRegistrationConfiguratorController extends AbstractConfiguratorController {
    private DataType dataType;

    public AbstractRegistrationConfiguratorController(DataType dataType, IPluginContext pluginContext) {
        super(pluginContext);
        this.dataType = dataType;
    }

    /**
     * Return the route to the registration configurator
     * 
     * @param method
     *            the HTTP method to be used
     * @param dataType
     *            the data type for the configurator
     * @param actionId
     *            the action Id to be passed to the controller
     * @return
     */
    public Call getRouteForRegistrationController(HttpMethod method, Long objectId, String actionId) {
        IImplementationDefinedObjectService implementationDefinedObjectService = ImplementationDefineObjectServiceFactory.getInstance();
        if (method.equals(HttpMethod.GET)) {
            return implementationDefinedObjectService.getRouteForPluginConfiguratorControllerDoGetRegistration(getPluginConfigurationId(), getDataType(),
                    objectId, actionId);
        } else {
            return implementationDefinedObjectService.getRouteForPluginConfiguratorControllerDoPostRegistration(getPluginConfigurationId(), getDataType(),
                    objectId, actionId);
        }
    }

    /**
     * Handle a get request from a configurator screen
     * 
     * @return
     */
    public Result doGet(Long objectId, String actionId) {
        return badRequest("No implementation available");
    }

    /**
     * Handle a post request from a configuration screen
     * 
     * @return
     */
    public Result doPost(Long objectId, String actionId) {
        return badRequest("No implementation available");
    }

    /**
     * The data type to which this registration option can be sent
     * 
     * @return a data type
     */
    public DataType getDataType() {
        return dataType;
    }
}
