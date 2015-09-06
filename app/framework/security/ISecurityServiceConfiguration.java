package framework.security;

import java.util.Map;

import be.objectify.deadbolt.java.DynamicResourceHandler;
import play.mvc.Result;

/**
 * The interface which gathers the "configurable" elements of a Security service
 * extending the {@link AbstractSecurityServiceImpl}.<br/>
 * These methods have to be implemented by the explicit implementation.
 * 
 * @author Pierre-Yves Cloux
 */
public interface ISecurityServiceConfiguration {
    /**
     * Return a map of {@link DynamicResourceHandler}.<br/>
     * The id of this map is the name of the dynamic permission associated with
     * this handler.
     * 
     * @return a map
     */
    public Map<String, DynamicResourceHandler> getDynamicResourceHandlers();

    /**
     * Return a view to be displayed to the end user when the access to a
     * resource is forbidden.<br/>
     * 
     * @return a map
     */
    public Result displayAccessForbidden();
}
