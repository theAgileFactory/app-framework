package framework.services.router;

/**
 * The interface to be implemented by the custom routing service.<br/>
 * An application can register for being notified with requests associated with
 * a specific route prefix.<br/>
 * These requests will "by-pass" play standard routing.
 * 
 * @author Pierre-Yves Cloux
 */
public interface ICustomRouterService {

    /**
     * Add a request listener for the specified request path prefix.<br/>
     * WARNING: if a path already exists it will be overriden or ignored.
     * 
     * @param pathPrefix
     *            a path prefix (example: /api/test) relative to the application
     *            root
     * @param listener
     *            a listener to be notified
     */
    public void addListener(String pathPrefix, IRequestListener listener);

    /**
     * Remove the specified listener from the service
     * 
     * @param pathPrefix
     *            a path prefix (example: /api/test) relative to the application
     *            root
     */
    public void removeListener(String pathPrefix);
}
