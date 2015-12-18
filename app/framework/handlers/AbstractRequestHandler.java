package framework.handlers;

import com.google.inject.Inject;

import framework.services.account.IAccountManagerPlugin;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.configuration.ITopMenuBarService;
import framework.services.kpi.IKpiService;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import play.Configuration;
import play.Logger;
import play.cache.CacheApi;
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
    private static Logger.ALogger log = Logger.of(AbstractRequestHandler.class);

    @Inject(optional = true)
    private Configuration configuration;
    @Inject(optional = true)
    private II18nMessagesPlugin messagesPlugin;
    @Inject(optional = true)
    private IUserSessionManagerPlugin userSessionManagerPlugin;
    @Inject(optional = true)
    private IAttachmentManagerPlugin attachmentManagerPlugin;
    @Inject(optional = true)
    private CacheApi cacheApi;
    @Inject(optional = true)
    private IAccountManagerPlugin accountManagerPlugin;
    @Inject(optional = true)
    private IKpiService kpiService;
    @Inject(optional = true)
    private IImplementationDefinedObjectService implementationDefinedObjectService;
    @Inject(optional = true)
    private ITopMenuBarService topMenuBarService;
    @Inject(optional = true)
    private IPreferenceManagerPlugin preferenceManagerPlugin;

    public AbstractRequestHandler() {
        super();
        log.info("AbstractRequestHandler initialized");
    }

    /**
     * This method injects the commonly used services in the {@link Context}.
     * These common services can then be access from the "args" variable of the
     * current context in the scala templates.
     * 
     * @param context
     */
    protected void injectCommonServicesIncontext(Http.Context context) {
        ContextArgsInjector.injectCommonServicesIncontext(context, getConfiguration(), getMessagesPlugin(), getUserSessionManagerPlugin(),
                getAttachmentManagerPlugin(), getAccountManagerPlugin(), getCacheApi(), getKpiService(), getImplementationDefinedObjectService(),
                getTopMenuBarService(), getPreferenceManagerPlugin());
    }

    protected Configuration getConfiguration() {
        return configuration;
    }

    protected II18nMessagesPlugin getMessagesPlugin() {
        return messagesPlugin;
    }

    protected IUserSessionManagerPlugin getUserSessionManagerPlugin() {
        return userSessionManagerPlugin;
    }

    protected IAttachmentManagerPlugin getAttachmentManagerPlugin() {
        return attachmentManagerPlugin;
    }

    protected CacheApi getCacheApi() {
        return cacheApi;
    }

    protected IAccountManagerPlugin getAccountManagerPlugin() {
        return accountManagerPlugin;
    }

    protected IKpiService getKpiService() {
        return kpiService;
    }

    protected IImplementationDefinedObjectService getImplementationDefinedObjectService() {
        return implementationDefinedObjectService;
    }

    private ITopMenuBarService getTopMenuBarService() {
        return topMenuBarService;
    }

    private IPreferenceManagerPlugin getPreferenceManagerPlugin() {
        return preferenceManagerPlugin;
    }
}
