package framework.handlers;

import framework.services.configuration.II18nMessagesPlugin;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import play.Configuration;
import play.mvc.Http;
import play.mvc.Http.Context;

/**
 * This utility class is used to "inject" some standard services into the
 * Context.args map.<br/>
 * This feature is used by the {@link AbstractRequestHandler} and the
 * {@link AbstractErrorHandler} in their injectCommonServicesIncontext method.
 * <br/>
 * At a later stage the ViewInjector scala trait is used to "inject" the
 * services in the views.
 */
abstract class ContextArgsInjector {
    /**
     * This method injects the commonly used services in the {@link Context}.
     * These common services can then be access from the "args" variable of the
     * current context in the scala templates.
     * 
     * @param context
     * @param configuration
     * @param messagesPlugin
     * @param userSessionManagerPlugin
     * @param attachmentManagerPlugin
     */
    public static void injectCommonServicesIncontext(Http.Context context, Configuration configuration, II18nMessagesPlugin messagesPlugin,
            IUserSessionManagerPlugin userSessionManagerPlugin, IAttachmentManagerPlugin attachmentManagerPlugin) {
        if (context == null) {
            return;
        }
        if (configuration != null) {
            context.args.put(Configuration.class.getName(), configuration);
        }
        if (messagesPlugin != null) {
            context.args.put(II18nMessagesPlugin.class.getName(), messagesPlugin);
        }
        if (userSessionManagerPlugin != null) {
            context.args.put(IUserSessionManagerPlugin.class.getName(), userSessionManagerPlugin);
        }
        if (attachmentManagerPlugin != null) {
            context.args.put(IAttachmentManagerPlugin.class.getName(), attachmentManagerPlugin);
        }
    }
}
