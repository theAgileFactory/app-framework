package framework.security;

import java.util.Map;

import be.objectify.deadbolt.java.DynamicResourceHandler;
import play.mvc.Result;

public interface ISecurityServiceConfiguration {
    public Map<String, DynamicResourceHandler> getDynamicResourceHandlers();
    public Result displayAccessForbidden();
}
