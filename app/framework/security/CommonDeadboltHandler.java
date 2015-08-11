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

import java.util.Optional;

import play.Logger;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Http;
import play.mvc.Result;
import be.objectify.deadbolt.core.models.Subject;
import be.objectify.deadbolt.java.AbstractDeadboltHandler;
import framework.services.account.AccountManagementException;
import framework.services.account.IAccountManagerPlugin;
import framework.services.account.IUserAccount;
import framework.services.session.IUserSessionManagerPlugin;

/**
 * The abstract basis for the authorization handler. This one is to be extended
 * into the application.
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class CommonDeadboltHandler extends AbstractDeadboltHandler {
    private static Logger.ALogger log = Logger.of(CommonDeadboltHandler.class);
    private IUserSessionManagerPlugin userSessionManagerPlugin;
    private IAccountManagerPlugin accountManagerPlugin;

    /**
     * Default constructor.
     */
    public CommonDeadboltHandler(IUserSessionManagerPlugin userSessionManagerPlugin, IAccountManagerPlugin accountManagerPlugin) {
        this.accountManagerPlugin = accountManagerPlugin;
        this.userSessionManagerPlugin = userSessionManagerPlugin;
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
                return displayAccessForbidden();
            }
        });
    }

    /**
     * To be implemented in sub class.<br/>
     * Redirect to the login page.
     * 
     * @param redirectUrl
     *            the redirect URL
     * @return
     */
    public abstract Result redirectToLoginPage(String redirectUrl);

    /**
     * Display an ACCESS FORBIDDEN page.
     * 
     * @return
     */
    public abstract Result displayAccessForbidden();

    private IUserSessionManagerPlugin getUserSessionManagerPlugin() {
        return userSessionManagerPlugin;
    }

    private IAccountManagerPlugin getAccountManagerPlugin() {
        return accountManagerPlugin;
    }
}