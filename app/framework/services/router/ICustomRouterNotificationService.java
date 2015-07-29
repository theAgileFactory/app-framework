package framework.services.router;

import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Http.Context;

/**
 * The interface to be used to notify a request possibly matching one of the
 * {@link IRequestListener} registered with the {@link ICustomRouterService}
 * 
 * @author Pierre-Yves Cloux
 */
public interface ICustomRouterNotificationService {
    public static final String NAME = "customRouterNotificationService";

    /**
     * Dispatch a notification
     * 
     * @param ctx
     *            the request context
     * @return null if no listener was found a promise of result otherwise
     */
    public Promise<Result> notify(Context ctx);
}
