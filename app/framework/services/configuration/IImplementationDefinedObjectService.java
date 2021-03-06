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
    String getDefaultCurrencyCode();

    /**
     * Return the route to the wait ajax image (the animated gif for wait)
     * 
     * @return a route
     */
    Call getRouteForAjaxWaitImage();

    /**
     * Get the routes to the JSON API which is associated with the
     * {@link DynamicSingleItemCustomAttributeValue}. (see corresponding
     * documentation)
     * 
     * @return a route
     */
    Call getRouteForDynamicSingleCustomAttributeApi();

    /**
     * Get the route to download an attachment associated with the specified id
     */
    Call getRouteForDownloadAttachedFile(Long attachmentId);

    /**
     * Get the route to delete an attachment associated with the specified id
     */
    Call getRouteForDeleteAttachedFile(Long attachmentId);

    /**
     * Return true if the save capability is active for the filter config tables
     * (the corresponding "getRouteForFilterConfiguration" should return the
     * filterConfigController actions).
     */
    boolean isFilterConfigurationActive();

    /**
     * Get the route for the create action of filter configuration.
     */
    Call getRouteForFilterConfigurationCreate();

    /**
     * Get the route for the save action of filter configuration.
     */
    Call getRouteForFilterConfigurationSave();

    /**
     * Get the route for the change action of filter configuration.
     */
    Call getRouteForFilterConfigurationChange();

    /**
     * Get the route for the edit action of filter configuration.
     */
    Call getRouteForFilterConfigurationEdit();

    /**
     * Get the route for the delete action of filter configuration.
     */
    Call getRouteForFilterConfigurationDelete();

    /**
     * Get the route for the share action of filter configuration.
     */
    Call getRouteForFilterConfigurationShare();

    /**
     * Get the route for the search principal action of filter configuration.
     */
    Call getRouteForFilterConfigurationSearchPrincipal();

    /**
     * Get the route for the delete action of filter configuration.
     */
    Call getRouteForFilterConfigurationClear();

    /**
     * Render an object.<br/>
     * This method can make use of advanced rendering (using views parts) or
     * simply a toString
     */
    String renderObject(Object object);
}
