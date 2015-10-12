package framework.security.bizdock_sso;

import org.pac4j.core.exception.TechnicalException;

import play.cache.CacheApi;

public class BizDockSSOAuthenticatorImpl implements BizDockSSOAuthenticator {

    private CacheApi cache;

    public BizDockSSOAuthenticatorImpl() {
        super();
    }

    public void setCacheApi(CacheApi cache) {
        this.cache = cache;
    }

    @Override
    public void validate(BizDockSSOCrendentials credentials) {

        try {

            BizDockSSOClient.SSOToken ssoToken = (BizDockSSOClient.SSOToken) cache.get(BizDockSSOClient.SSOToken.CACHE_PREFIX + credentials.getToken());

            if (ssoToken != null) {
                credentials.setUsername(ssoToken.getUid());
            } else {
                throw new TechnicalException("BizDockSSOCrendentials: impossible to find the token");
            }

        } catch (Exception e) {
            throw new TechnicalException("BizDockSSOCrendentials: unexpected error", e);
        }

    }

}
