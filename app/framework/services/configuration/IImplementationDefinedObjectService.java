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
package framework.services.configuration;

import models.framework_models.common.DynamicSingleItemCustomAttributeValue;
import play.mvc.Call;
import framework.commons.DataType;
import framework.services.plugins.PluginConfiguratorController;

/**
 * This is not a real service.<br/>
 * This interface is to be used by the framework to retrieve some objects which
 * can only be defined in the implementation.<br/>
 * Example : routes.<br/>
 * This means that this service must be implemented in the application using the
 * framework but must also be registered into the Spring service manager.
 * 
 * @author Pierre-Yves Cloux
 */
public interface IImplementationDefinedObjectService {
    /**
     * Return the default currency for the system
     * 
     * @return
     */
    public String getDefaultCurrencyCode();

    /**
     * Return the route to the wait ajax image (the animated gif for wait)
     * 
     * @return a route
     */
    public Call getRouteForAjaxWaitImage();

    /**
     * Get the routes to the JSON API which is associated with the
     * {@link DynamicSingleItemCustomAttributeValue}. (see corresponding
     * documentation)
     * 
     * @return a route
     */
    public Call getRouteForDynamicSingleCustomAttributeApi();

    /**
     * Get the route to download an attachment associated with the specified id
     */
    public Call getRouteForDownloadAttachedFile(Long attachmentId);

    /**
     * Get the route to delete an attachment associated with the specified id
     */
    public Call getRouteForDeleteAttachedFile(Long attachmentId);

    /**
     * Get the route to the ad-panel URL.<br/>
     * This URL is used to retrieve an AdPanel asynchronously
     */
    public Call getRouteForAdPanelContent(String page);

    /**
     * Get the route to the {@link PluginConfiguratorController}
     * doGetForCustomConfiguratorController method
     */
    public Call getRouteForPluginConfiguratorControllerDoGetCustom(Long pluginConfigurationId, String actionId);

    /**
     * Get the route to the {@link PluginConfiguratorController}
     * doPostForCustomConfiguratorController method
     */
    public Call getRouteForPluginConfiguratorControllerDoPostCustom(Long pluginConfigurationId, String actionId);

    /**
     * Get the route to the {@link PluginConfiguratorController}
     * doGetForRegistrationConfiguratorController method
     */
    public Call getRouteForPluginConfiguratorControllerDoGetRegistration(Long pluginConfigurationId, DataType dataType, Long objectId, String actionId);

    /**
     * Get the route to the {@link PluginConfiguratorController}
     * doPostForRegistrationConfiguratorController method
     */
    public Call getRouteForPluginConfiguratorControllerDoPostRegistration(Long pluginConfigurationId, DataType dataType, Long objectId, String actionId);

    /**
     * Get the route for switching the perspective.
     * 
     * @param key
     *            the top menu bar perspective key
     */
    public Call getRouteForSwitchingTopMenuBarPerspective(String key);

    /**
     * Reset the top menu bar to defaults
     */
    public void resetTopMenuBar();

    /**
     * Render an object.<br/>
     * This method can make use of advanced rendering (using views parts) or
     * simply a toString
     */
    public String renderObject(Object object);
}
