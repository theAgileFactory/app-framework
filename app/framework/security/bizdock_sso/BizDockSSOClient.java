package framework.security.bizdock_sso;

import java.security.SecureRandom;
import java.util.Random;

import org.pac4j.core.client.Mechanism;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.http.client.BaseHttpClient;

import framework.security.AbstractAuthenticator;
import play.Logger;
import play.cache.CacheApi;

public class BizDockSSOClient extends BaseHttpClient<BizDockSSOCrendentials> {

    private String formClientLoginUrl;

    private String tokenParameter = "token";
    private String redirectParameter = "redirect";

    private CacheApi cache;

    public BizDockSSOClient() {
    }

    public BizDockSSOClient(CacheApi cache, final String formClientLoginUrl, final BizDockSSOAuthenticator bizDockSSOAuthenticator) {
        setFormClientLoginUrl(formClientLoginUrl);
        setAuthenticator(bizDockSSOAuthenticator);
        this.cache = cache;
    }

    public BizDockSSOClient(CacheApi cache, final String formClientLoginUrl, final BizDockSSOAuthenticator bizDockSSOAuthenticator,
            final BizDockSSOProfileCreator profileCreator) {
        this(cache, formClientLoginUrl, bizDockSSOAuthenticator);
        setProfileCreator(profileCreator);
    }

    @Override
    protected BaseHttpClient<BizDockSSOCrendentials> newClient() {
        final BizDockSSOClient newClient = new BizDockSSOClient();
        newClient.setFormClientLoginUrl(this.formClientLoginUrl);
        newClient.setTokenParameter(this.tokenParameter);
        newClient.setRedirectParameter(this.redirectParameter);
        return newClient;
    }

    @Override
    protected void internalInit() {
        super.internalInit();
        CommonHelper.assertNotBlank("formClientLoginUrl", this.formClientLoginUrl);
    }

    @Override
    protected RedirectAction retrieveRedirectAction(final WebContext context) {
        return RedirectAction.redirect(this.formClientLoginUrl);
    }

    @Override
    protected BizDockSSOCrendentials retrieveCredentials(final WebContext context) throws RequiresHttpAction {
        final String token = context.getRequestParameter(this.getTokenParameter());
        final String redirect = context.getRequestParameter(this.getRedirectParameter());
        if (CommonHelper.isNotBlank(token) && CommonHelper.isNotBlank(redirect)) {
            final BizDockSSOCrendentials credentials = new BizDockSSOCrendentials(token, getName());
            try {
                BizDockSSOAuthenticatorImpl authenticator = (BizDockSSOAuthenticatorImpl) getAuthenticator();
                authenticator.setCacheApi(cache);
                authenticator.validate(credentials);

                // set the cookie in the response for the redirect
                String setCookieHeader = context.getRequestHeader("Set-Cookie");
                if (setCookieHeader == null || setCookieHeader.trim().equals("")) {
                    setCookieHeader = "";
                } else {
                    setCookieHeader += "; ";
                }
                setCookieHeader += AbstractAuthenticator.REDIRECT_URL_COOKIE_NAME + "=" + redirect;
                context.setResponseHeader("Set-Cookie", setCookieHeader);

            } catch (final TechnicalException e) {
                Logger.info(e.getMessage());
                final String message = "Credentials validation fails -> return to the form with error";
                throw RequiresHttpAction.redirect(message, context, getFormClientLoginUrl());
            }
            return credentials;
        }
        final String message = "Token cannot be blank -> return to the form with error";
        throw RequiresHttpAction.redirect(message, context, this.getFormClientLoginUrl());
    }

    public String getFormClientLoginUrl() {
        return this.formClientLoginUrl;
    }

    public void setFormClientLoginUrl(final String formClientLoginUrl) {
        this.formClientLoginUrl = formClientLoginUrl;
    }

    public String getTokenParameter() {
        return this.tokenParameter;
    }

    public void setTokenParameter(final String tokenParameter) {
        this.tokenParameter = tokenParameter;
    }

    public String getRedirectParameter() {
        return this.redirectParameter;
    }

    public void setRedirectParameter(final String redirectParameter) {
        this.redirectParameter = redirectParameter;
    }

    @Override
    public String toString() {
        return CommonHelper.toString(this.getClass(), "callbackUrl", this.callbackUrl, "name", getName(), "clientFormLoginUrl", this.getFormClientLoginUrl(),
                "tokenParameter", this.getTokenParameter(), "redirectParameter", this.getRedirectParameter(), "usernamePasswordAuthenticator",
                getAuthenticator(), "profileCreator", getProfileCreator());
    }

    @Override
    protected boolean isDirectRedirection() {
        return true;
    }

    @Override
    public Mechanism getMechanism() {
        return Mechanism.FORM_MECHANISM;
    }

    /**
     * An SSO token is used to auto-login a BizDock user to echannel.
     * 
     * @author Johann Kohler
     *
     */
    public static class SSOToken {

        public static final String CACHE_PREFIX = "sso_token.";

        private String token;
        private String uid;

        /**
         * Construct an SSO token.
         * 
         * @param uid
         *            the username
         */
        public SSOToken(String uid) {
            Random random = new SecureRandom();
            String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            String token = "";
            for (int i = 0; i < 128; i++) {
                int index = (int) (random.nextDouble() * letters.length());
                token += letters.substring(index, index + 1);
            }
            this.token = token;
            this.uid = uid;
        }

        /**
         * Get the token.
         */
        public String getToken() {
            return this.token;
        }

        /**
         * Get the uid.
         */
        public String getUid() {
            return this.uid;
        }

    }
}