package framework.security;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.pac4j.cas.client.CasClient;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.http.client.FormClient;
import org.pac4j.http.profile.UsernameProfileCreator;
import org.pac4j.play.Config;
import org.pac4j.play.PlayLogoutHandler;
import org.pac4j.play.java.RequiresAuthentication;
import org.pac4j.play.java.SecureController;
import org.pac4j.saml.credentials.Saml2Credentials;

import be.objectify.deadbolt.java.actions.SubjectPresent;
import framework.commons.IFrameworkConstants;
import framework.commons.IFrameworkConstants.AuthenticationMode;
import framework.security.bizdock_sso.BizDockSSOAuthenticatorImpl;
import framework.security.bizdock_sso.BizDockSSOClient;
import framework.security.bizdock_sso.BizDockSSOProfileCreator;
import framework.services.account.IAccountManagerPlugin;
import framework.services.account.IAuthenticationAccountReaderPlugin;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.account.IUserAccount;
import framework.services.account.LightAuthenticationUserPasswordAuthenticator;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.Language;
import framework.services.session.IUserSessionManagerPlugin;
import framework.utils.Utilities;
import play.Configuration;
import play.Logger;
import play.cache.CacheApi;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Call;
import play.mvc.Http.Cookie;
import play.mvc.Result;

public abstract class AbstractAuthenticator extends SecureController implements IAuthenticator {
    public static final String SAML_CLIENT_ID_EXTENTION = "?client_name=Saml2Client";
    public static final String REDIRECT_URL_COOKIE_NAME = "bzr";
    private static Logger.ALogger log = Logger.of(AbstractAuthenticator.class);
    private Configuration configuration;
    private CacheApi cache;
    private IUserSessionManagerPlugin userSessionManagerPlugin;
    private IAccountManagerPlugin accountManagerPlugin;
    private AuthenticationMode authenticationMode;
    private IInstanceAccessSupervisor instanceAccessSupervisor;
    private IPreferenceManagerPlugin preferenceManagerPlugin;
    private IAuthenticationAccountReaderPlugin authenticationAccountReader;
    private II18nMessagesPlugin i18nMessagesPlugin;
    private IAuthenticationLocalRoutes localRoutes;

    /**
     * Creates a new Authenticator
     * 
     * @param configuration
     * @param userSessionManagerPlugin
     * @param accountManagerPlugin
     * @param authenticationAccountReader
     * @param instanceAccessSupervisor
     * @param preferenceManagerPlugin
     * @param i18nMessagesPlugin
     * @param authenticationMode
     * @throws MalformedURLException
     */
    public AbstractAuthenticator(Configuration configuration, CacheApi cache, IUserSessionManagerPlugin userSessionManagerPlugin,
            IAccountManagerPlugin accountManagerPlugin, IAuthenticationAccountReaderPlugin authenticationAccountReader,
            IInstanceAccessSupervisor instanceAccessSupervisor, IPreferenceManagerPlugin preferenceManagerPlugin, II18nMessagesPlugin i18nMessagesPlugin,
            AuthenticationMode authenticationMode, IAuthenticationLocalRoutes localRoutes) throws MalformedURLException {
        super();
        this.configuration = configuration;
        this.cache = cache;
        this.authenticationMode = authenticationMode;
        this.userSessionManagerPlugin = userSessionManagerPlugin;
        this.accountManagerPlugin = accountManagerPlugin;
        this.authenticationAccountReader = authenticationAccountReader;
        this.instanceAccessSupervisor = instanceAccessSupervisor;
        this.preferenceManagerPlugin = preferenceManagerPlugin;
        this.i18nMessagesPlugin = i18nMessagesPlugin;
        this.localRoutes = localRoutes;
        log.info("Initialization based on authentication model " + authenticationMode);
        init(authenticationMode);
    }

    /**
     * Provide a redirect to the login page which is matching with the right
     * authentication mechanism.
     * 
     * @param redirectUrl
     *            the redirect URL
     */
    public Result redirectToLoginPage(String redirectUrl) {
        switch (getAuthenticationMode()) {
        case CAS_MASTER:
            return redirect(getLocalRoutes().getLoginCasRoute(redirectUrl));
        case CAS_SLAVE:
            return redirect(getLocalRoutes().getLoginCasRoute(redirectUrl));
        case STANDALONE: {
            setRedirectUrlInSession(redirectUrl);
            return redirect(getLocalRoutes().getLoginStandaloneRoute(redirectUrl));
        }
        case FEDERATED:
            return redirect(getLocalRoutes().getLoginFederatedRoute(redirectUrl));
        }
        return badRequest();
    }

    /**
     * Set a redirection URL in a cookie.<br/>
     * WARNING: this is used with the STANDALONE authentication mode since it
     * seems the redirect is not working
     * 
     * @param redirectUrl
     *            the redirect URL
     */
    public void setRedirectUrlInSession(String redirectUrl) {
        response().setCookie(REDIRECT_URL_COOKIE_NAME, redirectUrl);
    }

    /**
     * Get the previously set redirection URL.<br/>
     * WARNING: this is used with the STANDALONE authentication mode since it
     * seems the redirect is not working
     * 
     * @return
     */
    public String getRedirectUrlInSession() {
        Cookie redirectUrlCookie = request().cookie(REDIRECT_URL_COOKIE_NAME);
        if (redirectUrlCookie != null && redirectUrlCookie.value() != null) {
            response().discardCookie(REDIRECT_URL_COOKIE_NAME);
            return redirectUrlCookie.value();
        }
        // If no redirect URL then redirect to the public URL
        return getPreferenceManagerPlugin().getPreferenceElseConfigurationValue(IFrameworkConstants.PUBLIC_URL_PREFERENCE, "maf.public.url");
    }

    /**
     * Perform a login, if successful then redirect to the home page.<br/>
     * This redirect to the FEDERATED login page.
     * 
     * @param redirectUrl
     *            the redirect url
     * 
     * @return redirectUrl to the home page or display an error (access to a
     *         protected area)
     */
    @RequiresAuthentication(clientName = "Saml2Client")
    public Result loginFederated(String redirectUrl) {
        return loginCode(redirectUrl);
    }

    /**
     * Perform a login, if successful then redirect to the home page.<br/>
     * This redirect to the CAS_MASTER or CAS_SLAVE login page.
     * 
     * @param redirectUrl
     *            the redirect url
     * 
     * @return redirectUrl to the home page or display an error (access to a
     *         protected area)
     */
    @RequiresAuthentication(clientName = "CasClient")
    public Result loginCasMaster(String redirectUrl) {
        return loginCode(redirectUrl);
    }

    /**
     * Perform a login, if successful then redirect to the home page. This
     * redirect to the STANDALONE login page.
     * 
     * @param redirectUrl
     *            the redirect url
     * 
     * @return redirectUrl to the home page or display an error (access to a
     *         protected area)
     */
    @RequiresAuthentication(clientName = "FormClient")
    public Result loginStandalone(String redirectUrl) {
        return loginCode(redirectUrl);
    }

    /**
     * Redirect the user to the previous saved URL.
     */
    @SubjectPresent
    public Result redirectToThePreviouslySavedUrl() {
        return loginCode(getRedirectUrlInSession());
    }

    /**
     * The code executed depending on the login implementation.
     * 
     * @param redirectUrl
     *            the URL to which the user must be redirected after
     *            authentication
     * @return
     */
    private Result loginCode(String redirectUrl) {
        try {
            String uid = getUserSessionManagerPlugin().getUserSessionId(ctx());
            if (log.isDebugEnabled()) {
                log.debug("Here is the user session uid found " + uid);
            }
            IUserAccount userAccount = getAccountManagerPlugin().getUserAccountFromUid(uid);

            // User is not found
            if (userAccount == null) {
                if (log.isDebugEnabled()) {
                    log.debug("The account associated with " + uid + " was not found by the account manager, instance is not accessible");
                }
                getUserSessionManagerPlugin().clearUserSession(ctx());
                return redirect(getLocalRoutes().getNoFederatedAccount());
            }

            // event: success login
            if (userAccount.isDisplayed()) {
                getInstanceAccessSupervisor().logSuccessfulLoginEvent(getUserSessionManagerPlugin().getUserSessionId(ctx()));
            }

            // get the preferred language as object
            Language language = new Language(userAccount.getPreferredLanguage());

            // verify the language is valid
            if (getI18nMessagesPlugin().isLanguageValid(language.getCode())) {
                Logger.debug("change language to: " + language.getCode());
                ctx().changeLang(language.getCode());
                Utilities.setSsoLanguage(ctx(), language.getCode());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return redirect(redirectUrl);
    }

    /**
     * Call back for the SAML implementation
     * 
     * @return
     */
    public Promise<Result> samlCallback() {
        return customCallback();
    }

    /**
     * Clear the user session and logout the user.<br/>
     * The user is then redirected to the login page.
     */
    public Promise<Result> customCallback() {
        if (!getInstanceAccessSupervisor().checkLoginAuthorized()) {
            return Promise.promise(new Function0<Result>() {
                public Result apply() throws Throwable {
                    // redirect to new page
                    return redirect(getLocalRoutes().getNotAccessibleRoute());
                }
            });
        }

        if (log.isDebugEnabled()) {
            log.debug("Received call back : " + ctx().request().toString());
        }
        return callback();
    }

    /**
     * Clear the user session and logout the user.<br/>
     * The user is then redirected to the login page.
     */
    public Result customLogout() {
        if (log.isDebugEnabled()) {
            log.debug("Logout requested");
        }
        // Workaround
        // Clear redmine cookie
        Cookie redmineCookie = ctx().request().cookie("_redmine_session");
        if (redmineCookie != null) {
            ctx().response().discardCookie("_redmine_session");
        }
        if (getAuthenticationMode().equals(AuthenticationMode.FEDERATED)) {
            return getFederatedLogoutDisplay();
        }
        return logoutAndRedirect();
    }

    /**
     * Initialize the SSO according to the configured authentication mode.
     * 
     * @param authenticationMode
     *            the selected authentication mode
     * @throws MalformedURLException
     */
    public void init(IFrameworkConstants.AuthenticationMode authenticationMode) throws MalformedURLException {
        // Initialize the authentication mode
        switch (authenticationMode) {
        case CAS_MASTER:
            initCasSingleSignOn();
            break;
        case CAS_SLAVE:
            initCasSingleSignOn();
            break;
        case STANDALONE:
            initStandaloneAuthentication();
            break;
        case FEDERATED:
            initSAMLv2SingleSignOn();
            break;
        }
    }

    /**
     * Display a screen after the federated logout
     * 
     * @return
     */
    public abstract Result getFederatedLogoutDisplay();

    /**
     * Display a "not accessible" page when the user has no remaining license.
     * 
     * @return
     */
    public abstract Result notAccessible();

    /**
     * This page is displayed in principle in CAS or FEDRATED mode when a
     * session is found but no corresponding user account.
     * 
     * @return
     */
    public abstract Result noFederatedAccount();

    /**
     * Initialize the BizDock SSO module based on CAS.
     */
    private void initCasSingleSignOn() {
        log.info(">>>>>>>>>>>>>>>> Initialize CAS SSO");
        String casLoginUrl = getConfiguration().getString("cas.login.url");
        String casCallbackUrl = getPreferenceManagerPlugin().getPreferenceElseConfigurationValue(IFrameworkConstants.PUBLIC_URL_PREFERENCE, "maf.public.url")
                + getLocalRoutes().getCallbackRoute().url();
        final CasClient casClient = new CasClient();
        casClient.setLogoutHandler(new PlayLogoutHandler());
        casClient.setCasProtocol(CasClient.CasProtocol.SAML);
        casClient.setCasLoginUrl(casLoginUrl);
        casClient.setTimeTolerance(getConfiguration().getLong("cas.time_tolerance"));
        final Clients clients = new Clients(casCallbackUrl, casClient);
        Config.setClients(clients);
        Config.setDefaultLogoutUrl(getConfiguration().getString("cas.logout.url"));
        log.info(">>>>>>>>>>>>>>>> Initialize CAS SSO (end)");
    }

    /**
     * Initialize the SSO module based on SAMLv2.
     * 
     * @throws MalformedURLException
     * 
     * @throws ConfigurationException
     */
    private void initSAMLv2SingleSignOn() throws MalformedURLException {
        log.info(">>>>>>>>>>>>>>>> Initialize SAMLv2 SSO");
        final Saml2Client saml2Client = new Saml2Client(
                preferenceManagerPlugin.getPreferenceElseConfigurationValue(IFrameworkConstants.PUBLIC_URL_PREFERENCE, "maf.public.url"));
        File samlConfigFile = new File(getConfiguration().getString("saml.sso.config"));
        if (!samlConfigFile.exists() || samlConfigFile.isDirectory()) {
            throw new IllegalArgumentException("The authentication mode is FEDERATED but the SAML config file does not exists " + samlConfigFile);
        }
        log.info("SAML configuration found, loading properties");
        try {
            PropertiesConfiguration cfg = new PropertiesConfiguration(samlConfigFile);
            String publicUrl = getPreferenceManagerPlugin().getPreferenceElseConfigurationValue(IFrameworkConstants.PUBLIC_URL_PREFERENCE, "maf.public.url");
            if (cfg.containsKey("maf.saml.entityId")) {
                saml2Client.setSpEntityId(cfg.getString("maf.saml.entityId"));
            } else {
                saml2Client.setSpEntityId(publicUrl);
            }

            // Set the user profile attribute defined in the configuration (if
            // any)
            if (cfg.containsKey("maf.saml.profile.attribute")) {
                this.getUserSessionManagerPlugin().setUserProfileAttributeName(cfg.getString("maf.saml.profile.attribute"));
            }

            File configurationDirectory = samlConfigFile.getParentFile();
            saml2Client.setKeystorePath(new File(configurationDirectory, cfg.getString("maf.saml.keystorefile")).getAbsolutePath());
            saml2Client.setKeystorePassword(cfg.getString("maf.saml.keystore.password"));
            saml2Client.setPrivateKeyPassword(cfg.getString("maf.saml.privatekey.password"));
            saml2Client.setIdpMetadataPath(new File(configurationDirectory, cfg.getString("maf.saml.idpmetadata")).getAbsolutePath());
            saml2Client.setCallbackUrl(publicUrl + getLocalRoutes().getSamlCallbackRoute().url() + SAML_CLIENT_ID_EXTENTION);
            saml2Client.setMaximumAuthenticationLifetime(cfg.getInt("maf.saml.maximum.authentication.lifetime"));

            // Write the client meta data to the file system
            String spMetaDataFileName = cfg.getString("maf.saml.spmetadata");
            FileUtils.write(new File(configurationDirectory, spMetaDataFileName), saml2Client.printClientMetadata());
            log.info("Service Provider meta-data written to the file system in " + spMetaDataFileName);

            final Clients clients = new Clients(publicUrl + getLocalRoutes().getSamlCallbackRoute().url(), saml2Client);
            clients.init();
            Config.setClients(clients);
            if (cfg.containsKey("maf.saml.logout.url")) {
                Config.setDefaultLogoutUrl(cfg.getString("maf.saml.logout.url"));
            } else {
                Config.setDefaultLogoutUrl(getLocalRoutes().getLogoutRoute().url());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to initialize the FEDERATED SSO", e);
        }
        log.info(">>>>>>>>>>>>>>>> Initialize SAMLv2 SSO (end)");
    }

    /**
     * Initialize the BizDock standalone authentication mode.
     */
    private void initStandaloneAuthentication() {
        log.info(">>>>>>>>>>>>>>>> Initialize Standalone Authentication mode");

        final FormClient formClient = new FormClient(getLocalRoutes().getDisplayStandaloneLoginFormRoute().url(),
                new LightAuthenticationUserPasswordAuthenticator(getAuthenticationAccountReader()), new UsernameProfileCreator());
        formClient.setUsernameParameter("username");
        formClient.setPasswordParameter("password");

        BizDockSSOClient bizDockSSOClient = null;
        if (getConfiguration().getBoolean("maf.authentication.bizdock_sso.is_active")) {
            bizDockSSOClient = new BizDockSSOClient(this.cache, getLocalRoutes().getDisplayStandaloneLoginFormRoute().url(),
                    new BizDockSSOAuthenticatorImpl(), new BizDockSSOProfileCreator());
        }

        String casCallbackUrl = getPreferenceManagerPlugin().getPreferenceElseConfigurationValue(IFrameworkConstants.PUBLIC_URL_PREFERENCE, "maf.public.url")
                + getLocalRoutes().getCallbackRoute().url();

        Clients clients = null;
        if (bizDockSSOClient != null) {
            clients = new Clients(casCallbackUrl, formClient, bizDockSSOClient);
        } else {
            clients = new Clients(casCallbackUrl, formClient);
        }

        Config.setClients(clients);
        Config.setProfileTimeout(getConfiguration().getInt("standalone.sso.profile.timeout"));
        Config.setDefaultSuccessUrl(getLocalRoutes().getRedirectToThePreviouslySavedUrl().url());

        log.info(">>>>>>>>>>>>>>>> Initialize Standalone Authentication mode (end)");
    }

    protected IUserSessionManagerPlugin getUserSessionManagerPlugin() {
        return userSessionManagerPlugin;
    }

    protected IAccountManagerPlugin getAccountManagerPlugin() {
        return accountManagerPlugin;
    }

    protected AuthenticationMode getAuthenticationMode() {
        return authenticationMode;
    }

    protected Configuration getConfiguration() {
        return configuration;
    }

    protected IPreferenceManagerPlugin getPreferenceManagerPlugin() {
        return preferenceManagerPlugin;
    }

    protected IAuthenticationAccountReaderPlugin getAuthenticationAccountReader() {
        return authenticationAccountReader;
    }

    protected II18nMessagesPlugin getI18nMessagesPlugin() {
        return i18nMessagesPlugin;
    }

    private IInstanceAccessSupervisor getInstanceAccessSupervisor() {
        return instanceAccessSupervisor;
    }

    private IAuthenticationLocalRoutes getLocalRoutes() {
        return localRoutes;
    }

    /**
     * An alternative implementation of the standard PAC4J
     * {@link org.pac4j.saml.client.Saml2Client}. The purpose of this client is
     * to support the SAML authentication while the server is behind a SSL
     * reverse proxy (thus altering the host name, port and security level)
     */
    public static class Saml2Client extends org.pac4j.saml.client.Saml2Client {
        private URL alternativeHostUrl;

        /**
         * Creates a {@link Saml2Client} with an alternative host URL
         * 
         * @param alternativePublicUrl
         * @throws MalformedURLException
         */
        public Saml2Client(String alternativeHostUrlAsString) throws MalformedURLException {
            super();
            alternativeHostUrl = new URL(alternativeHostUrlAsString);
        }

        @Override
        protected Saml2Credentials retrieveCredentials(final WebContext wc) throws RequiresHttpAction {
            return super.retrieveCredentials(new WebContext() {

                @Override
                public void writeResponseContent(String content) {
                    wc.writeResponseContent(content);
                }

                @Override
                public void setSessionAttribute(String name, Object value) {
                    wc.setSessionAttribute(name, value);
                }

                @Override
                public void setResponseStatus(int code) {
                    wc.setResponseStatus(code);
                }

                @Override
                public void setResponseHeader(String name, String value) {
                    wc.setResponseHeader(name, value);
                }

                @Override
                public Object getSessionAttribute(String name) {
                    return wc.getSessionAttribute(name);
                }

                @Override
                public int getServerPort() {
                    if (getAlternativeHostUrl().getPort() == -1) {
                        return getAlternativeHostUrl().getDefaultPort();
                    }
                    return getAlternativeHostUrl().getPort();
                }

                @Override
                public String getServerName() {
                    return getAlternativeHostUrl().getHost();
                }

                @Override
                public String getScheme() {
                    return getAlternativeHostUrl().getProtocol();
                }

                @Override
                public Map<String, String[]> getRequestParameters() {
                    return wc.getRequestParameters();
                }

                @Override
                public String getRequestParameter(String name) {
                    return wc.getRequestParameter(name);
                }

                @Override
                public String getRequestMethod() {
                    return wc.getRequestMethod();
                }

                @Override
                public String getRequestHeader(String name) {
                    return wc.getRequestHeader(name);
                }

                @Override
                public String getFullRequestURL() {
                    StringBuffer fullRequestUrl = new StringBuffer();
                    fullRequestUrl.append(getScheme());
                    fullRequestUrl.append("://");
                    fullRequestUrl.append(getServerName());
                    if (getAlternativeHostUrl().getPort() != -1) {
                        fullRequestUrl.append(':');
                        fullRequestUrl.append(getServerPort());
                    }
                    fullRequestUrl.append(request().uri());
                    return fullRequestUrl.toString();
                }
            });
        }

        private URL getAlternativeHostUrl() {
            return alternativeHostUrl;
        }

    }

    /**
     * The interface to be implemented by the Authenticator controller explicity
     * implementation.<br/>
     * The methods provides various routes to for the authentication service.
     * <br/>
     * This interface is required because the play routes are compiled and their
     * configuration and path may depends on the application implementing the
     * framework.<br/>
     * Some of the routes should point to the implementation of the
     * {@link AbstractAuthenticator} and some others to a controller providing
     * the GUI for the "standalone" mode (extending
     * {@link AbstractStandaloneAuthenticationController}). Here is an example
     * of routes file for an Authenticator controller extending the
     * {@link AbstractAuthenticator} and a controller implementing the interface
     * for the standalone authentication named
     * "StandaloneAuthenticationController" (this one is implementing a method
     * "displayLoginForm" which is to display the standalone login form):
     * 
     * <pre>
     * #Authentication
     * GET     /auth/displayLoginForm      controllers.sso.StandaloneAuthenticationController.displayLoginForm()
     * GET     /not-accessible             controllers.sso.Authenticator.notAccessible()
     * GET     /loginStandalone            controllers.sso.Authenticator.loginStandalone(redirect)
     * GET     /loginCasMaster             controllers.sso.Authenticator.loginCasMaster(redirect)
     * GET     /loginFederated             controllers.sso.Authenticator.loginFederated(redirect)
     * GET     /redirectToSavedUrl         controllers.sso.Authenticator.redirectToThePreviouslySavedUrl()
     * GET     /callback                   controllers.sso.Authenticator.customCallback()
     * POST    /callback                   controllers.sso.Authenticator.customCallback()
     * GET     /logout                     controllers.sso.Authenticator.customLogout()
     * </pre>
     * 
     * This configuration would then match with the following implementation of
     * the interface:
     * 
     * <pre>
     * new IAuthenticationLocalRoutes() {
     *
     *     &#64;Override
     *     public Call getRedirectToThePreviouslySavedUrl() {
     *         return controllers.sso.routes.Authenticator.redirectToThePreviouslySavedUrl();
     *     }
     * 
     *     &#64;Override
     *     public Call getLogoutRoute() {
     *         return controllers.sso.routes.Authenticator.customLogout();
     *     }
     * 
     *     &#64;Override
     *     public Call getLoginStandaloneRoute(String redirectUrl) {
     *         return controllers.sso.routes.Authenticator.loginStandalone(redirectUrl);
     *     }
     * 
     *     &#64;Override
     *     public Call getLoginFederatedRoute(String redirectUrl) {
     *         return controllers.sso.routes.Authenticator.loginFederated(redirectUrl);
     *     }loginFederated
     * 
     *     &#64;Override
     *     public Call getLoginCasRoute(String redirectUrl) {
     *         return controllers.sso.routes.Authenticator.loginCasMaster(redirectUrl);
     *     }
     * 
     *     &#64;Override
     *     public Call getCallbackRoute() {
     *         return controllers.sso.routes.Authenticator.customCallback();
     *     }
     * 
     *     &#64;Override
     *     public Call getDisplayStandaloneLoginFormRoute() {
     *         return controllers.sso.routes.StandaloneAuthenticationController.displayLoginForm();
     *     }
     * 
     *     &#64;Override
     *     public Call getNotAccessibleRoute() {
     *         return controllers.sso.routes.Authenticator.notAccessible();
     *     }
     * }
     * </pre>
     * 
     * @author Pierre-Yves Cloux
     */
    public static interface IAuthenticationLocalRoutes {
        /**
         * The route to the {@link AbstractAuthenticator} action implemented by
         * the method "loginCasMaster".
         * 
         * Here is an example of route (assuming that "Authenticator" is the
         * controller extending {@link AbstractAuthenticator}:
         * 
         * <pre>
         * GET     /loginCasMaster             controllers.sso.Authenticator.loginCasMaster(redirect)
         * </pre>
         * 
         * @param redirectUrl
         *            the URL to which the user will be redirected after login
         * @return
         */
        public Call getLoginCasRoute(String redirectUrl);

        /**
         * The route to the {@link AbstractAuthenticator} action implemented by
         * the method "loginStandalone".
         * 
         * Here is an example of route (assuming that "Authenticator" is the
         * controller extending {@link AbstractAuthenticator}:
         * 
         * <pre>
         * GET     /loginStandalone             controllers.sso.Authenticator.loginStandalone(redirect)
         * </pre>
         * 
         * @param redirectUrl
         *            the URL to which the user will be redirected after login
         * @return
         */
        public Call getLoginStandaloneRoute(String redirectUrl);

        /**
         * The route to the {@link AbstractAuthenticator} action implemented by
         * the method "loginFederated".
         * 
         * Here is an example of route (assuming that "Authenticator" is the
         * controller extending {@link AbstractAuthenticator}:
         * 
         * <pre>
         * GET     /loginFederated             controllers.sso.Authenticator.loginFederated(redirect)
         * </pre>
         * 
         * @param redirectUrl
         *            the URL to which the user will be redirected after login
         * @return
         */
        public Call getLoginFederatedRoute(String redirectUrl);

        /**
         * The route to the {@link AbstractAuthenticator} action implemented by
         * the method "customCallback".<br/>
         * Such route is used for CAS and STANDALONE authentication callbacks.
         * 
         * Here is an example of routes configuration (assuming that
         * "Authenticator" is the controller extending
         * {@link AbstractAuthenticator}:
         * 
         * <pre>
         * GET     /callback                   controllers.sso.Authenticator.customCallback()
         * POST    /callback                   controllers.sso.Authenticator.customCallback()
         * </pre>
         * 
         * <b>IMPORTANT</b> : a GET and POST routes are required since some
         * {@link AuthenticationMode} can support both methods.
         * 
         * @return
         */
        public Call getCallbackRoute();

        /**
         * The route to the {@link AbstractAuthenticator} action implemented by
         * the method "samlCallback".<br/>
         * Such route is used for FEDERATED authentication callbacks.
         * 
         * Here is an example of routes configuration (assuming that
         * "Authenticator" is the controller extending
         * {@link AbstractAuthenticator}:
         * 
         * <pre>
         * GET     /samlCallback                   controllers.sso.Authenticator.samlCallback()
         * POST    /samlCallback                   controllers.sso.Authenticator.samlCallback()
         * </pre>
         * 
         * <b>IMPORTANT</b> : a GET and POST routes are required since some
         * {@link AuthenticationMode} can support both methods.
         * 
         * @return
         */
        public Call getSamlCallbackRoute();

        /**
         * The route to the {@link AbstractAuthenticator} action implemented by
         * the method "customLogout".
         * 
         * Here is an example of route (assuming that "Authenticator" is the
         * controller extending {@link AbstractAuthenticator}:
         * 
         * <pre>
         * GET     /logout                     controllers.sso.Authenticator.customLogout()
         * </pre>
         * 
         * @return
         */
        public Call getLogoutRoute();

        /**
         * The route to the {@link AbstractAuthenticator} action implemented by
         * the method "redirectToThePreviouslySavedUrl".
         * 
         * Here is an example of route (assuming that "Authenticator" is the
         * controller extending {@link AbstractAuthenticator}:
         * 
         * <pre>
         * GET     /redirectToSavedUrl         controllers.sso.Authenticator.redirectToThePreviouslySavedUrl()
         * </pre>
         * 
         * @return
         */
        public Call getRedirectToThePreviouslySavedUrl();

        /**
         * The route to the {@link AbstractAuthenticator} action implemented by
         * the method "notAccessible".
         * 
         * Here is an example of route (assuming that "Authenticator" is the
         * controller extending {@link AbstractAuthenticator}:
         * 
         * <pre>
         * GET     /not-accessible             controllers.sso.Authenticator.notAccessible()
         * </pre>
         * 
         * @return
         */
        public Call getNotAccessibleRoute();

        /**
         * The route to the {@link AbstractAuthenticator} action implemented by
         * the method "noFederatedAccount".
         * 
         * Here is an example of route (assuming that "Authenticator" is the
         * controller extending {@link AbstractAuthenticator}:
         * 
         * <pre>
         * GET     /no-account             controllers.sso.Authenticator.noFederatedAccount()
         * </pre>
         * 
         * @return
         */
        public Call getNoFederatedAccount();

        /**
         * The only route which is not provided by the
         * {@link AbstractAuthenticator} and must direct to a specific
         * controller implementation for the standalone mode (this one being
         * necessarily specific to the application and extending
         * {@link AbstractStandaloneAuthenticationController}).<br/>
         * This route must display the standalone login form.
         * 
         * @return
         */
        public Call getDisplayStandaloneLoginFormRoute();
    }
}