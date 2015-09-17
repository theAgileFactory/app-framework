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

import framework.services.ext.ILinkGenerationService;
import framework.services.ext.api.AbstractExtensionController;

/**
 * The root class for the configurator controllers.<br/>
 * 
 * @param
 *            <P>
 *            the {@link IPluginRunner} class associated with this controller
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class AbstractConfiguratorController<P> extends AbstractExtensionController {
    private IPluginContext pluginContext;
    private P pluginRunner;

    @SuppressWarnings("unchecked")
    public AbstractConfiguratorController(ILinkGenerationService linkGenerationService, IPluginRunner pluginRunner, IPluginContext pluginContext) {
        super(linkGenerationService);
        this.pluginContext = pluginContext;
        this.pluginRunner = (P) pluginRunner;
    }

    public void init(IPluginContext pluginContext, P pluginRunner) {
        this.pluginContext = pluginContext;
        this.pluginRunner = pluginRunner;
    }

    protected IPluginContext getPluginContext() {
        return pluginContext;
    }

    protected P getPluginRunner() {
        return pluginRunner;
    }
}
