package framework.security.bizdock_sso;

import org.pac4j.core.credentials.Authenticator;

public interface BizDockSSOAuthenticator extends Authenticator<BizDockSSOCrendentials> {

    /**
     * {@inheritDoc}
     */
    @Override
    void validate(BizDockSSOCrendentials credentials);
}
