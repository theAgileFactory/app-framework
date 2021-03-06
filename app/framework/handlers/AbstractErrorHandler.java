package framework.handlers;

import javax.inject.Provider;

import com.google.inject.Inject;

import framework.services.account.IAccountManagerPlugin;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.audit.IAuditLoggerService;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.configuration.ITopMenuBarService;
import framework.services.custom_attribute.ICustomAttributeManagerService;
import framework.services.kpi.IKpiService;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.api.OptionalSourceMapper;
import play.api.routing.Router;
import play.cache.CacheApi;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http;
import play.mvc.Http.Context;

public class AbstractErrorHandler extends DefaultHttpErrorHandler {
    private static Logger.ALogger log = Logger.of(AbstractErrorHandler.class);

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
    @Inject(optional = true)
    private ICustomAttributeManagerService customAttributeManagerService;
    @Inject(optional = true)
    private IAuditLoggerService auditLoggerService;

    @Inject
    public AbstractErrorHandler(Configuration configuration, Environment environment, OptionalSourceMapper optionalSourceMapper,
            Provider<Router> providerRouter) {
        super(configuration, environment, optionalSourceMapper, providerRouter);
        this.configuration = configuration;
        log.info("AbstractErrorHandler initialized");
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
                getTopMenuBarService(), getPreferenceManagerPlugin(), this.getCustomAttributeManagerService(), getAuditLoggerService());
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

    private ICustomAttributeManagerService getCustomAttributeManagerService() {
        return this.customAttributeManagerService;
    }

    public IAuditLoggerService getAuditLoggerService() {
        return auditLoggerService;
    }
}
