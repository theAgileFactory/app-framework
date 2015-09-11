package framework.security;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;

import be.objectify.deadbolt.core.models.Permission;
import be.objectify.deadbolt.core.models.Subject;
import be.objectify.deadbolt.java.AbstractDeadboltHandler;
import be.objectify.deadbolt.java.DeadboltHandler;
import be.objectify.deadbolt.java.DynamicResourceHandler;
import be.objectify.deadbolt.java.JavaAnalyzer;
import be.objectify.deadbolt.java.actions.Unrestricted;
import be.objectify.deadbolt.java.cache.HandlerCache;
import be.objectify.deadbolt.java.cache.SubjectCache;
import framework.commons.IFrameworkConstants;
import framework.services.account.AccountManagementException;
import framework.services.account.IAccountManagerPlugin;
import framework.services.account.IUserAccount;
import framework.services.session.IUserSessionManagerPlugin;
import framework.utils.Utilities;
import play.Configuration;
import play.Logger;
import play.cache.CacheApi;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;

/**
 * Handlers cache implementation.<br/>
 * Only one in our current implementation.
 */
@Singleton
public abstract class AbstractSecurityServiceImpl implements HandlerCache, ISecurityService, ISecurityServiceConfiguration {
    private static Logger.ALogger log = Logger.of(AbstractSecurityServiceImpl.class);
    private JavaAnalyzer deadBoltAnalyzer;
    private SubjectCache subjectCache;
    private Configuration configuration;
    private IAccountManagerPlugin accountManagerPlugin;
    private IUserSessionManagerPlugin userSessionManagerPlugin;
    private DefaultDeadboltHandler defaultHandler;
    private CacheApi cacheApi;
    private IAuthenticator authenticator;

    public AbstractSecurityServiceImpl(JavaAnalyzer deadBoltAnalyzer, SubjectCache subjectCache, Configuration configuration,
            IUserSessionManagerPlugin userSessionManagerPlugin, IAccountManagerPlugin accountManagerPlugin, CacheApi cacheApi, IAuthenticator authenticator) {
        this.deadBoltAnalyzer = deadBoltAnalyzer;
        this.subjectCache = subjectCache;
        this.configuration = configuration;
        this.userSessionManagerPlugin = userSessionManagerPlugin;
        this.accountManagerPlugin = accountManagerPlugin;
        this.cacheApi = cacheApi;
        this.authenticator = authenticator;
    }

    /**
     * Return a default instance of {@link DefaultDeadboltHandler}.<br/>
     * This one is lazy initialized.
     * 
     * @return
     */
    protected DefaultDeadboltHandler getDefaultHandler() {
        if (this.defaultHandler == null) {
            log.info(">>>>>>>>>>>>>>>> Lazy initialization of the default deadbolt handler...");
            this.defaultHandler = new DefaultDeadboltHandler(this, getUserSessionManagerPlugin(), getAccountManagerPlugin(), this, getCacheApi(),
                    getAuthenticator());
            log.info(">>>>>>>>>>>>>>>> Lazy initialization of the default deadbolt handler (end)");
        }
        return defaultHandler;
    }

    @Override
    public DeadboltHandler apply(final String key) {
        return getDefaultHandler();
    }

    @Override
    public DeadboltHandler get() {
        return getDefaultHandler();
    }

    @Override
    public IUserAccount getCurrentUser() throws AccountManagementException {
        String currentUserSessionId = getUserSessionManagerPlugin().getUserSessionId(Http.Context.current());
        if (currentUserSessionId == null) {
            return null;
        }
        IUserAccount userAccount = getAccountManagerPlugin().getUserAccountFromUid(currentUserSessionId);
        return userAccount;
    }

    @Override
    public Promise<Result> checkHasSubject(Function0<Result> resultIfHasSubject) {
        Optional<Subject> subjectOption = get().getSubject(Http.Context.current()).get(DEFAULT_TIMEOUT);
        if (subjectOption.isPresent()) {
            return Promise.promise(() -> resultIfHasSubject.apply());
        }
        return get().onAuthFailure(Http.Context.current(), null);
    }

    @Override
    public boolean dynamic(String name, String meta) {
        if (log.isDebugEnabled()) {
            log.debug("Check dynamic permission with Handler [" + get() + "]");
            log.debug("Check dynamic permission with Dynamic Handler [" + get().getDynamicResourceHandler(Http.Context.current()) + "]");
        }
        try {
            DeadboltHandler handler = get();
            DynamicResourceHandler dynamicResourceHandler = handler.getDynamicResourceHandler(Http.Context.current()).get(DEFAULT_TIMEOUT).get();
            return dynamicResourceHandler.isAllowed(name, meta, get(), Http.Context.current()).get(DEFAULT_TIMEOUT);
        } catch (Exception e) {
            log.error("Error while trying to check if a user is allowed for the permission name " + name + " and the meta information " + meta, e);
        }
        return false;
    }

    @Override
    public boolean dynamic(String name, String meta, Long id) {
        return getDefaultHandler().isAllowed(name, meta, get(), id).get(DEFAULT_TIMEOUT);
    }

    @Override
    public boolean restrict(List<String[]> deadBoltRoles) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("RESTRICT with handler [" + get() + "] timeout [" + DEFAULT_TIMEOUT + "] for roles " + Utilities.toString(deadBoltRoles));
            }
            Optional<Subject> subjectOption = getSubject(Http.Context.current(), get()).get(DEFAULT_TIMEOUT);
            if (!subjectOption.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.debug("RESTRICT FALSE since no subject found");
                }
                return false;
            }
            Subject subject = subjectOption.get();
            if (log.isDebugEnabled()) {
                log.debug("RESTRICT Subject = " + subject);
            }
            return restrict(deadBoltRoles, subject);
        } catch (Exception e) {
            log.error("Error while checking restriction for " + Utilities.toString(deadBoltRoles), e);
            return false;
        }
    }

    @Override
    public boolean restrict(List<String[]> deadBoltRoles, String uid) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("RESTRICT for uid [" + uid + "] [" + get() + "] timeout [" + DEFAULT_TIMEOUT + "] for roles " + Utilities.toString(deadBoltRoles));
            }
            Subject subject = getAccountManagerPlugin().getUserAccountFromUid(uid);
            return restrict(deadBoltRoles, subject);
        } catch (Exception e) {
            log.error("Error while checking restriction for " + Utilities.toString(deadBoltRoles), e);
            return false;
        }
    }

    @Override
    public boolean restrict(List<String[]> deadBoltRoles, Subject subject) {
        try {
            if (subject == null) {
                return false;
            }
            for (String[] rolesArray : deadBoltRoles) {
                if (getDeadBoltAnalyzer().hasAllRoles(Optional.of(subject), rolesArray)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error while checking restriction for " + Utilities.toString(deadBoltRoles), e);
            return false;
        }
    }

    @Override
    public boolean restrict(final String roleName, final Subject subject) {
        try {
            if (subject == null) {
                return false;
            }
            return getDeadBoltAnalyzer().hasRole(Optional.of(subject), roleName);
        } catch (Exception e) {
            log.error("Error while checking restriction for " + roleName, e);
            return false;
        }
    }

    @Override
    public boolean restrict(final String[] roleNames, final Subject subject) {
        try {
            if (subject == null) {
                return false;
            }
            return getDeadBoltAnalyzer().hasAllRoles(Optional.of(subject), roleNames);
        } catch (Exception e) {
            log.error("Error while checking restriction for " + ArrayUtils.toString(roleNames), e);
            return false;
        }
    }

    @Override
    public boolean restrict(String roleName) {
        try {
            return restrict(roleName, getCurrentUser());
        } catch (Exception e) {
            log.error("Error while checking restriction for current user and " + roleName, e);
            return false;
        }
    }

    @Override
    public boolean restrict(String[] roleNames) {
        try {
            return restrict(roleNames, getCurrentUser());
        } catch (Exception e) {
            log.error("Error while checking restriction for current user and " + ArrayUtils.toString(roleNames), e);
            return false;
        }
    }

    /**
     * Gets the {@link be.objectify.deadbolt.core.models.Subject} from the
     * {@link DeadboltHandler}, and logs an error if it's not present. Note that
     * at least one actions ({@link Unrestricted} does not not require a Subject
     * to be present.
     *
     * @param ctx
     *            the request context
     * @param deadboltHandler
     *            the Deadbolt handler
     * @return the Subject, if any
     */
    private Promise<Optional<Subject>> getSubject(final Http.Context ctx, final DeadboltHandler deadboltHandler) {
        if (log.isDebugEnabled()) {
            log.debug("GET SUBJECT with subject cache [" + getSubjectCache() + "]");
        }
        return getSubjectCache().apply(deadboltHandler, ctx).map(option -> {
            if (!option.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.info("Subject not found in Deadbolt subject cache");
                }
            }
            return option;
        });
    }

    private IAuthenticator getAuthenticator() {
        return authenticator;
    }

    protected void setDefaultHandler(DefaultDeadboltHandler defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    protected JavaAnalyzer getDeadBoltAnalyzer() {
        return deadBoltAnalyzer;
    }

    protected SubjectCache getSubjectCache() {
        return subjectCache;
    }

    protected IAccountManagerPlugin getAccountManagerPlugin() {
        return accountManagerPlugin;
    }

    protected IUserSessionManagerPlugin getUserSessionManagerPlugin() {
        return userSessionManagerPlugin;
    }

    protected CacheApi getCacheApi() {
        return cacheApi;
    }

    protected Configuration getConfiguration() {
        return configuration;
    }

    /**
     * The handler for the authorization mechanism based on Deadbold.<br/>
     * The class also holds a set of static methods which are to manage the user
     * session (basically create, get or delete the session entry containing the
     * unique user id)
     * 
     * @author Pierre-Yves Cloux
     */
    public static class DefaultDeadboltHandler extends AbstractDeadboltHandler {
        private DefaultDynamicResourceHandler dynamicResourceHandler;
        private IAuthenticator authenticator;
        private IUserSessionManagerPlugin userSessionManagerPlugin;
        private IAccountManagerPlugin accountManagerPlugin;
        private ISecurityServiceConfiguration securityServiceConfiguration;

        public DefaultDeadboltHandler(ISecurityServiceConfiguration securityServiceConfiguration, IUserSessionManagerPlugin userSessionManagerPlugin,
                IAccountManagerPlugin accountManagerPlugin, ISecurityService securityService, CacheApi cacheApi, IAuthenticator authenticator) {
            this.authenticator = authenticator;
            this.dynamicResourceHandler = new DefaultDynamicResourceHandler(userSessionManagerPlugin, cacheApi, securityService,
                    securityServiceConfiguration.getDynamicResourceHandlers());
            this.userSessionManagerPlugin = userSessionManagerPlugin;
            this.accountManagerPlugin = accountManagerPlugin;
            this.securityServiceConfiguration = securityServiceConfiguration;
        }

        @Override
        public Promise<Optional<Result>> beforeAuthCheck(final Http.Context ctx) {
            String uid = getUserSessionManagerPlugin().getUserSessionId(ctx);
            if (log.isDebugEnabled()) {
                log.debug("Calling beforeAuthCheck, user in session is " + uid);
            }
            if (uid == null) {
                return Promise.promise(new Function0<Optional<Result>>() {
                    public Optional<Result> apply() throws Throwable {
                        return Optional.of(redirectToLoginPage(ctx.request().uri()));
                    }

                });

            }
            Optional<Result> emptyResult = Optional.empty();
            return Promise.promise(() -> emptyResult);
        }

        @Override
        public Promise<Optional<Subject>> getSubject(Http.Context ctx) {
            Optional<Subject> emptySubject = Optional.empty();
            String uid = getUserSessionManagerPlugin().getUserSessionId(ctx);
            if (log.isDebugEnabled()) {
                log.debug("Looking for a subject (getSubject), user in session is " + uid);
            }
            if (uid != null) {
                IUserAccount userAccount;
                try {
                    userAccount = getAccountManagerPlugin().getUserAccountFromUid(uid);
                    if (log.isDebugEnabled()) {
                        log.debug("User account for " + uid + " retreived " + userAccount);
                    }
                } catch (AccountManagementException e) {
                    log.error("Authorization failed: Unable to get the user profile for " + uid, e);
                    return Promise.promise(() -> emptySubject);
                }
                if (userAccount == null) {
                    log.info("User account for " + uid + " NOT FOUND");
                    return Promise.promise(() -> emptySubject);
                }
                if (userAccount.isActive()) {
                    // if the user is active, we return the profile
                    return Promise.promise(new Function0<Optional<Subject>>() {
                        @Override
                        public Optional<Subject> apply() throws Throwable {
                            if (log.isDebugEnabled()) {
                                log.debug("Returning an optional of userAccount for " + userAccount.getUid());
                            }
                            return Optional.of(userAccount);
                        }
                    });
                } else if (log.isDebugEnabled()) {
                    log.warn("A locked user (" + uid + ") try to access desktop. isActive : " + userAccount.isActive());
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Nothing worked for authenticating the current user, returning an option of null");
            }
            return Promise.promise(() -> emptySubject);
        }

        @Override
        public Promise<Result> onAuthFailure(Http.Context ctx, String content) {
            if (getUserSessionManagerPlugin().getUserSessionId(ctx) == null) {
                final String redirectUrl = ctx.request().uri();

                if (log.isDebugEnabled()) {
                    log.debug("Attempt to access URL " + redirectUrl + " without session, redirecting to login page");
                }

                // If the user seems not to be logged then redirect to the login
                return Promise.promise(new Function0<Result>() {
                    @Override
                    public Result apply() throws Throwable {
                        if (log.isDebugEnabled()) {
                            log.debug("Redirecting to login page");
                        }
                        return redirectToLoginPage(redirectUrl);
                    }
                });
            }
            // If the user is logged, the error should come from an attempt to
            // access a forbidden resource
            return Promise.promise(new Function0<Result>() {
                @Override
                public Result apply() throws Throwable {
                    if (log.isDebugEnabled()) {
                        log.debug("Redirecting to access forbidden");
                    }
                    return getSecurityServiceConfiguration().displayAccessForbidden();
                }
            });
        }

        public Result redirectToLoginPage(String redirectUrl) {
            return getAuthenticator().redirectToLoginPage(redirectUrl);
        }

        @Override
        public Promise<Optional<DynamicResourceHandler>> getDynamicResourceHandler(Http.Context context) {
            // WARNING : context can be null in some cases
            return Promise.promise(() -> Optional.of(getDynamicResourceHandler()));
        }

        /**
         * Check if the dynamic permission is allowed for the specified id
         * 
         * @param name
         *            a dynamic permission name
         * @param meta
         * @param deadboltHandler
         *            a deadbolt handler
         * @param id
         *            a unique id for an object
         * @param context
         *            a context
         * @return a promise of a boolean
         */
        public Promise<Boolean> isAllowed(String name, String meta, DeadboltHandler deadboltHandler, Long id) {
            return getDynamicResourceHandler().isAllowed(name, meta, deadboltHandler, id, Http.Context.current());
        }

        private DefaultDynamicResourceHandler getDynamicResourceHandler() {
            return dynamicResourceHandler;
        }

        private IAuthenticator getAuthenticator() {
            return authenticator;
        }

        private IAccountManagerPlugin getAccountManagerPlugin() {
            return accountManagerPlugin;
        }

        private IUserSessionManagerPlugin getUserSessionManagerPlugin() {
            return userSessionManagerPlugin;
        }

        private ISecurityServiceConfiguration getSecurityServiceConfiguration() {
            return securityServiceConfiguration;
        }

    }

    /**
     * The deadbolt dynamic resource handler.
     * 
     * @author Johann Kohler
     * 
     */
    public static class DefaultDynamicResourceHandler implements DynamicResourceHandler {

        private Map<String, DynamicResourceHandler> dynamicAuthenticationHandlers;

        private IUserSessionManagerPlugin userSessionManagerPlugin;
        private CacheApi cacheApi;
        private ISecurityService securityService;
        private static final Integer CACHE_TTL = 300;

        public DefaultDynamicResourceHandler(IUserSessionManagerPlugin userSessionManagerPlugin, CacheApi cacheApi, ISecurityService securityService,
                Map<String, DynamicResourceHandler> dynamicAuthenticationHandlers) {
            super();
            this.userSessionManagerPlugin = userSessionManagerPlugin;
            this.cacheApi = cacheApi;
            this.securityService = securityService;
            this.dynamicAuthenticationHandlers = dynamicAuthenticationHandlers;
        }

        @Override
        public Promise<Boolean> checkPermission(String permissionValue, DeadboltHandler deadboltHandler, Context ctx) {
            boolean permissionOk = false;

            try {
                IUserAccount userAccount = getSecurityService().getCurrentUser();
                if (userAccount != null) {
                    List<? extends Permission> permissions = userAccount.getPermissions();
                    for (Iterator<? extends Permission> iterator = permissions.iterator(); !permissionOk && iterator.hasNext();) {
                        Permission permission = iterator.next();
                        permissionOk = permission.getValue().contains(permissionValue);
                    }
                }
            } catch (Exception e) {
                log.error("impossible to get the user", e);
            }

            final boolean permissionOkFinal = permissionOk;
            return Promise.promise(() -> permissionOkFinal);
        }

        @Override
        public Promise<Boolean> isAllowed(String name, String meta, DeadboltHandler deadboltHandler, Context context) {
            return isAllowed(name, meta, deadboltHandler, Utilities.getId(context), context);
        }

        /**
         * Check if the dynamic permission is allowed for the specified id
         * 
         * @param name
         *            a dynamic permission name
         * @param meta
         * @param deadboltHandler
         *            a deadbolt handler
         * @param id
         *            a unique id for an object
         * @param context
         *            a context
         * @return a promise of a boolean
         */
        private Promise<Boolean> isAllowed(String name, String meta, DeadboltHandler deadboltHandler, Long id, Http.Context context) {
            String cacheKey = getCacheKey(name, id);
            Boolean isAllowed = (Boolean) getCacheApi().get(cacheKey);
            if (isAllowed != null) {
                if (log.isDebugEnabled()) {
                    log.debug("dynamic permission " + cacheKey + " read from cache, and result is: " + isAllowed);
                }
                final boolean isAllowedFinal = isAllowed;
                return Promise.promise(() -> isAllowedFinal);
            }

            DynamicResourceHandler handler = getDynamicAuthenticationHandlers().get(name);
            Promise<Boolean> result = Promise.promise(() -> false);
            if (handler == null) {
                log.error("No handler available for " + name);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Dynamic permission: " + name);
                }
                result = handler.isAllowed(name, meta, deadboltHandler, context);
            }

            // set the result
            getCacheApi().set(cacheKey, result.get(ISecurityService.DEFAULT_TIMEOUT), CACHE_TTL);

            return result;
        }

        /**
         * Get the cache key depending of the permission name and the object id.
         * 
         * @param name
         *            the permission name
         * @param id
         *            the object id
         */
        private String getCacheKey(String name, Long id) {
            IUserSessionManagerPlugin userSessionManagerPlugin = getUserSessionManagerPlugin();
            String cacheKey = IFrameworkConstants.DYNAMIC_PERMISSION_CACHE_PREFIX + userSessionManagerPlugin.getUserSessionId(Http.Context.current()) + "."
                    + name + "." + String.valueOf(id);
            return cacheKey;

        }

        private IUserSessionManagerPlugin getUserSessionManagerPlugin() {
            return userSessionManagerPlugin;
        }

        private CacheApi getCacheApi() {
            return cacheApi;
        }

        private ISecurityService getSecurityService() {
            return securityService;
        }

        private Map<String, DynamicResourceHandler> getDynamicAuthenticationHandlers() {
            return dynamicAuthenticationHandlers;
        }
    }
}
