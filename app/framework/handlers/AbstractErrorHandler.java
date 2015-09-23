package framework.handlers;

import javax.inject.Provider;

import com.google.inject.Inject;

import framework.services.account.IAccountManagerPlugin;
import framework.services.configuration.II18nMessagesPlugin;
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
        ContextArgsInjector.injectCommonServicesIncontext(context, configuration, messagesPlugin, userSessionManagerPlugin, attachmentManagerPlugin,
                accountManagerPlugin, cacheApi, kpiService);
    }
}
