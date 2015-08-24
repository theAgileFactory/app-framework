package framework.handlers;

import com.google.inject.Inject;

import framework.services.configuration.II18nMessagesPlugin;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import play.Configuration;
import play.http.DefaultHttpRequestHandler;
import play.mvc.Http;
import play.mvc.Http.Context;

/**
 * Overriding of the default request handler.<br/>
 * The abstract implementation provides a "injectCommonServicesIncontext" which
 * support the injection of commonly used services into the scala templates.
 * 
 * @author Johann Kohler
 *
 */
public abstract class AbstractRequestHandler extends DefaultHttpRequestHandler {
    @Inject(optional = true)
    private Configuration configuration;
    @Inject(optional = true)
    private II18nMessagesPlugin messagesPlugin;
    @Inject(optional = true)
    private IUserSessionManagerPlugin userSessionManagerPlugin;
    @Inject(optional = true)
    private IAttachmentManagerPlugin attachmentManagerPlugin;

    /**
     * This method injects the commonly used services in the {@link Context}.
     * These common services can then be access from the "args" variable of the
     * current context in the scala templates.
     * 
     * @param context
     */
    protected void injectCommonServicesIncontext(Http.Context context) {
        ContextArgsInjector.injectCommonServicesIncontext(context, configuration, messagesPlugin, userSessionManagerPlugin, attachmentManagerPlugin);
    }
}
