package framework.services.plugins.api;

import framework.services.ext.ILinkGenerationService;
import framework.services.ext.api.AbstractExtensionController;
import framework.services.ext.api.WebCommandPath;
import framework.services.ext.api.WebParameter;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * The controller to be extended by the plugins registration controllers (=
 * attached to a specified object id).
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class AbstractRegistrationConfiguratorController<P> extends AbstractExtensionController {
    private IPluginContext pluginContext;
    private P pluginRunner;

    @SuppressWarnings("unchecked")
    public AbstractRegistrationConfiguratorController(ILinkGenerationService linkGenerationService, IPluginRunner pluginRunner, IPluginContext pluginContext) {
        super(linkGenerationService);
        this.pluginContext = pluginContext;
        this.pluginRunner = (P) pluginRunner;
    }

    @WebCommandPath(id = WebCommandPath.DEFAULT_COMMAND_ID, path = WebCommandPath.DEFAULT_COMMAND_PATH + "/:id")
    public Promise<Result> index(@WebParameter(name = "id") Long objectId) {
        return register(objectId);
    }

    /**
     * This method is called by the default method for the registration
     * controller.<br/>
     * <b>No need to mark it with</b>:
     * 
     * <pre>
     * {@code
     * &#64;WebCommandPath
     * }
     * 
     * </pre>
     * 
     * @param objectId
     *            the object id to be registered with the plugin
     * @return a result
     */
    public abstract Promise<Result> register(Long objectId);

    protected IPluginContext getPluginContext() {
        return pluginContext;
    }

    protected P getPluginRunner() {
        return pluginRunner;
    }
}
