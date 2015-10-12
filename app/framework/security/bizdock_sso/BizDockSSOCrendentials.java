package framework.security.bizdock_sso;

import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.util.CommonHelper;

public class BizDockSSOCrendentials extends Credentials {

    private static final long serialVersionUID = 1L;

    private String username;
    private final String token;

    public BizDockSSOCrendentials(final String token, final String clientName) {
        this.token = token;
        setClientName(clientName);
    }

    public String getToken() {
        return this.token;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }

    @Override
    public String toString() {
        return CommonHelper.toString(this.getClass(), "token", this.getToken(), "clientName", getClientName());
    }
}