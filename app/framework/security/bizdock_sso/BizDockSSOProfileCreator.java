package framework.security.bizdock_sso;

import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileCreator;
import org.pac4j.http.profile.HttpProfile;

public class BizDockSSOProfileCreator implements ProfileCreator<BizDockSSOCrendentials, HttpProfile> {

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpProfile create(BizDockSSOCrendentials credentials) {
        String username = credentials.getUsername();
        final HttpProfile profile = new HttpProfile();
        profile.setId(username);
        profile.addAttribute(CommonProfile.USERNAME, username);
        return profile;
    }
}
