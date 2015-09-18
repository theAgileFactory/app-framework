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
import framework.services.ext.api.WebCommandPath;
import play.libs.F.Promise;
import play.mvc.Result;

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

    @WebCommandPath()
    public Promise<Result> index() {
        return configure();
    }

    /**
     * This method is called by the default method associated with the
     * configuration controller.<br/>
     * <b>No need to mark it with</b>:
     * 
     * <pre>
     * {@code
     * &#64;WebCommandPath
     * }
     * 
     * </pre>
     * 
     * @return a result
     */
    public abstract Promise<Result> configure();

    protected IPluginContext getPluginContext() {
        return pluginContext;
    }

    protected P getPluginRunner() {
        return pluginRunner;
    }
}
