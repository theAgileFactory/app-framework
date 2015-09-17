package framework.services.plugins.api;

import framework.services.ext.ILinkGenerationService;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * The controller to be extended by the plugins registration controllers (=
 * attached to a specified object id).
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class AbstractRegistrationConfiguratorController<P> extends AbstractConfiguratorController<P> {

    public AbstractRegistrationConfiguratorController(ILinkGenerationService linkGenerationService, IPluginRunner pluginRunner, IPluginContext pluginContext) {
        super(linkGenerationService, pluginRunner, pluginContext);
    }

    /**
     * This method is the default method for the registration controller.<br/>
     * WARNING: this method must be marked with
     * 
     * <pre>
     * {@code
     * 
     * &#64;WebCommandPath
     * 
     * }
     * </pre>
     * 
     * No parameters are required since it must be the default command.
     * 
     * @param objectId
     *            the object id to be registered with the plugin
     * @return a result
     */
    public abstract Promise<Result> index(Long objectId);
}
