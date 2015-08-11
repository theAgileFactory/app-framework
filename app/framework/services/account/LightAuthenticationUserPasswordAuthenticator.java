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
package framework.services.account;

import org.pac4j.core.exception.CredentialsException;
import org.pac4j.http.credentials.UsernamePasswordAuthenticator;
import org.pac4j.http.credentials.UsernamePasswordCredentials;

import play.Logger;

/**
 * An authentication which is used for the standalone authentication.<br/>
 * It is used in combination with Pac4j.
 * 
 * @author Pierre-Yves Cloux
 */
public class LightAuthenticationUserPasswordAuthenticator implements UsernamePasswordAuthenticator {
    private static Logger.ALogger log = Logger.of(LightAuthenticationUserPasswordAuthenticator.class);

    private IAuthenticationAccountReaderPlugin authenticationAccountReader;

    public LightAuthenticationUserPasswordAuthenticator(IAuthenticationAccountReaderPlugin authenticationAccountReader) {
        super();
        this.authenticationAccountReader = authenticationAccountReader;
    }

    @Override
    public void validate(UsernamePasswordCredentials userNamePasswordCredentials) {
        try {
            IUserAuthenticationAccount authAccount = getAuthenticationAccountReader().getAccountFromUid(userNamePasswordCredentials.getUsername());
            if (authAccount != null) {
                if (!authAccount.isActive()) {
                    log.warn("Failed login attempt for " + userNamePasswordCredentials.getUsername() + " : account locked");
                    throw new LightAuthenticationLockedAccountException("Account locked");
                }
                if (!getAuthenticationAccountReader().checkPassword(userNamePasswordCredentials.getUsername(), userNamePasswordCredentials.getPassword())) {
                    log.warn("Failed login attempt for " + userNamePasswordCredentials.getUsername() + " : invalid login or password");
                    throw new CredentialsException("Invalid login or password");
                }
            } else {
                log.warn("Failed login attempt for " + userNamePasswordCredentials.getUsername() + " : user does not exists");
                throw new CredentialsException("User does not exists");
            }
        } catch (AccountManagementException e) {
            throw new CredentialsException(e);
        }
    }

    private IAuthenticationAccountReaderPlugin getAuthenticationAccountReader() {
        return authenticationAccountReader;
    }
}