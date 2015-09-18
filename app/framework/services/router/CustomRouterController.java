package framework.services.router;

import javax.inject.Inject;

import play.Logger;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * The custom router controller.<br/>
 * This one must be registered in the routes file with:
 * 
 * <pre>
 * {@code
 * GET    /ext/*path                  framework.services.router.CustomRouterController.perform(path)
 * POST   /ext/*path                  framework.services.router.CustomRouterController.perform(path)
 * }
 * </pre>
 * 
 * @author Pierre-Yves Cloux
 */
public class CustomRouterController extends Controller {
    private static Logger.ALogger log = Logger.of(CustomRouterController.class);
    @Inject
    private ICustomRouterNotificationService customRouterNotificationService;

    public CustomRouterController() {
    }

    /**
     * The controller "generic method"
     * 
     * @param path
     * @return
     */
    public Promise<Result> perform(String path) {
        if (log.isDebugEnabled()) {
            log.debug("Calling custom router path " + path);
        }
        Promise<Result> result = getCustomRouterNotificationService().notify(ctx());
        if (result != null) {
            return result;
        }
        return Promise.promise(() -> badRequest());
    }

    public ICustomRouterNotificationService getCustomRouterNotificationService() {
        return customRouterNotificationService;
    }
}
