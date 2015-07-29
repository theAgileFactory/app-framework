package framework.services.router;

import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Http.Context;

/**
 * The interface which is user to notify of a request.<br/>
 * The listener must have been previously added to the
 * {@link ICustomRouterService}
 * 
 * @author Pierre-Yves Cloux
 */
public interface IRequestListener {
    /**
     * Notify that a request matching the listener path has been received.
     * 
     * @param ctx
     *            the context
     * @return a result
     */
    public Promise<Result> notifyRequest(Context ctx);
}
