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

import play.mvc.Controller;
import framework.services.plugins.PluginContextImpl;

/**
 * The root class for the configurator controllers
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class AbstractConfiguratorController extends Controller {
    /**
     * The first action call by BizDock on a configuration controller.<br/>
     * This one displays the initial screen
     */
    public static final String INITIAL_ACTION = "_root";
    private Long pluginConfigurationId;

    public AbstractConfiguratorController(IPluginContext pluginContext) {
        this.pluginConfigurationId = ((PluginContextImpl) pluginContext).getPluginConfigurationId();
    }

    protected Long getPluginConfigurationId() {
        return pluginConfigurationId;
    }
}
