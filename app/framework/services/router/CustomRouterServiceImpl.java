package framework.services.router;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import play.mvc.Http.Context;
import play.mvc.Result;
import framework.services.database.IDatabaseDependencyService;

/**
 * The implementation for the {@link ICustomRouterService} and
 * {@link ICustomRouterNotificationService} interfaces
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class CustomRouterServiceImpl implements ICustomRouterNotificationService, ICustomRouterService {
    private static Logger.ALogger log = Logger.of(CustomRouterServiceImpl.class);
    private Map<String, IRequestListener> registeredListeners = Collections.synchronizedMap(new HashMap<String, IRequestListener>());

    /**
     * Create a new KpiServiceImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param databaseDependencyService
     */
    @Inject
    public CustomRouterServiceImpl(ApplicationLifecycle lifecycle, Configuration configuration, IDatabaseDependencyService databaseDependencyService) {
        log.info("SERVICE>>> CustomRouterServiceImpl starting...");
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> CustomRouterServiceImpl stopping...");
            log.info("SERVICE>>> CustomRouterServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> CustomRouterServiceImpl started");
    }

    @Override
    public void addListener(String pathPrefix, IRequestListener listener) {
        registeredListeners.put(pathPrefix, listener);
        log.info("Custom router registration [" + pathPrefix + "]");
    }

    @Override
    public void removeListener(String pathPrefix) {
        registeredListeners.remove(pathPrefix);
        log.info("Custom router UNregistration [" + pathPrefix + "]");
    }

    @Override
    public Promise<Result> notify(Context ctx) {
        if (log.isDebugEnabled()) {
            log.debug("Notification for request " + ctx.request().path());
        }
        for (String pathPrefix : getRegisteredListeners().keySet()) {
            if (ctx.request().path().startsWith(pathPrefix)) {
                if (log.isDebugEnabled()) {
                    log.debug("Notification for request " + ctx.request().path() + " found matching listener " + pathPrefix);
                }
                return getRegisteredListeners().get(pathPrefix).notifyRequest(ctx);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("No matching listener for " + ctx.request().path());
        }
        return null;
    }

    private Map<String, IRequestListener> getRegisteredListeners() {
        return registeredListeners;
    }

}
