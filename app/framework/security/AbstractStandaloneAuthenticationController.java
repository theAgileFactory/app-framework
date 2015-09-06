/*! LICENSE
 *
 * Copyright (c) 2015, The Agile Factory SA and/or its affiliates. All rights
 * reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package framework.security;

import org.apache.commons.lang3.StringUtils;
import org.pac4j.http.client.FormClient;
import org.pac4j.play.Config;

import framework.security.AbstractAuthenticator.IAuthenticationLocalRoutes;
import framework.services.account.LightAuthenticationLockedAccountException;
import models.framework_models.account.Credential;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * The abstract controller to be implemented by the controller which provides
 * GUI management for the standalone login mode based on the {@link Credential}
 * object.
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class AbstractStandaloneAuthenticationController extends Controller {
    public static final String ERROR_PARAMETER = "error";

    /**
     * Default constructor.
     */
    public AbstractStandaloneAuthenticationController() {
    }

    /**
     * Return a login info object to be used by the display login method.
     * 
     * @return
     */
    public LoginInfo getLoginInfo() {
        FormClient formClient = (FormClient) Config.getClients().findClient("FormClient");
        String errorParameter = request().getQueryString(ERROR_PARAMETER);
        boolean hasError = false;
        boolean accountLocked = false;
        if (!StringUtils.isBlank(errorParameter)) {
            hasError = true;
            if (errorParameter.equals(LightAuthenticationLockedAccountException.class.getSimpleName())) {
                accountLocked = true;
            }
        }
        return new LoginInfo(formClient.getUsernameParameter(), hasError, accountLocked, errorParameter, formClient.getCallbackUrl());
    }

    /**
     * A data structure which contains some information about the login
     * authentication
     * <ul>
     * <li>userLogin : the login which was entered by the user in the "login"
     * field (if this is a new login attempt)</li>
     * <li>hasErrors : an an error occurred after the login process</li>
     * <li>accountLocked : the account is locked after a too high number of
     * attempts</li>
     * <li>errorMessage : some technical details about the error</li>
     * <li>loginFormActionUrl : the URL to be used by the login form as an
     * action</li>
     * </ul>
     * 
     * @author Pierre-Yves Cloux
     */
    public static class LoginInfo {
        private String userLogin;
        private boolean hasErrors;
        private boolean accountLocked;
        private String errorMessage;
        private String loginFormActionUrl;

        public LoginInfo(String userLogin, boolean hasErrors, boolean accountLocked, String errorMessage, String loginFormActionUrl) {
            super();
            this.userLogin = userLogin;
            this.hasErrors = hasErrors;
            this.accountLocked = accountLocked;
            this.errorMessage = errorMessage;
            this.loginFormActionUrl = loginFormActionUrl;
        }

        public String getUserLogin() {
            return userLogin;
        }

        public void setUserLogin(String userLogin) {
            this.userLogin = userLogin;
        }

        public boolean hasErrors() {
            return hasErrors;
        }

        public boolean isAccountLocked() {
            return accountLocked;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getLoginFormActionUrl() {
            return loginFormActionUrl;
        }
    }

    /**
     * Display the authentication form. This method is to be provided as a route
     * to the {@link AbstractAuthenticator} using the
     * {@link IAuthenticationLocalRoutes} interface implementation.
     */
    public abstract Result displayLoginForm();
}
