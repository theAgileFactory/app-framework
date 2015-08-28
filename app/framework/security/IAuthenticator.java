package framework.security;

import play.mvc.Result;

public interface IAuthenticator {
    /**
     * Provide a redirect to the login page which is matching with the right
     * authentication mechanism.
     * 
     * @param redirectUrl
     *            the redirect URL
     */
    public Result redirectToLoginPage(String redirectUrl);
}
