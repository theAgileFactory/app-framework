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

import play.Logger;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Http;
import play.mvc.Result;
import be.objectify.deadbolt.core.models.Subject;
import be.objectify.deadbolt.java.AbstractDeadboltHandler;
import framework.services.ServiceManager;
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

    /**
     * Default constructor.
     */
    public CommonDeadboltHandler() {
        super();
    }

    @Override
    public Promise<Result> beforeAuthCheck(final Http.Context ctx) {

        Logger.debug("beforeAuthCheck");

        String uid = ServiceManager.getService(IUserSessionManagerPlugin.NAME, IUserSessionManagerPlugin.class).getUserSessionId(ctx);

        if (uid == null) {

            return Promise.promise(new Function0<Result>() {

                public Result apply() throws Throwable {
                    return redirectToLoginPage(ctx.request().uri());
                }

            });

        }

        return Promise.pure(null);
    }

    @Override
    public Subject getSubject(Http.Context ctx) {

        String uid = ServiceManager.getService(IUserSessionManagerPlugin.NAME, IUserSessionManagerPlugin.class).getUserSessionId(ctx);
        if (log.isDebugEnabled()) {
            log.debug("Found session for " + uid);
        }
        if (uid != null) {
            IUserAccount userAccount;
            try {
                userAccount = ServiceManager.getService(IAccountManagerPlugin.NAME, IAccountManagerPlugin.class).getUserAccountFromUid(uid);
                if (log.isDebugEnabled()) {
                    log.debug("User account for " + uid + " retreived " + userAccount);
                }
            } catch (AccountManagementException e) {
                log.error("Authorization failed: Unable to get the user profile for " + uid, e);
                return null;
            }
            if (userAccount == null) {
                log.info("User account for " + uid + " NOT FOUND");
                return null;
            }
            if (userAccount.isActive()) {
                // if the user is active, we return the profile
                return userAccount;
            } else if (log.isDebugEnabled()) {
                log.warn("A locked user (" + uid + ") try to access desktop. isActive : " + userAccount.isActive());
            }
        }
        return null;
    }

    @Override
    public Promise<Result> onAuthFailure(Http.Context ctx, String content) {
        if (ServiceManager.getService(IUserSessionManagerPlugin.NAME, IUserSessionManagerPlugin.class).getUserSessionId(ctx) == null) {
            final String redirectUrl = ctx.request().uri();

            if (log.isDebugEnabled()) {
                log.debug("Attempt to access URL " + redirectUrl + " without session, redirecting to login page");
            }

            // If the user seems not to be logged then redirect to the login
            return Promise.promise(new Function0<Result>() {
                @Override
                public Result apply() throws Throwable {
                    return redirectToLoginPage(redirectUrl);
                }
            });
        }
        // If the user is logged, the error should come from an attempt to
        // access a forbidden resource
        return Promise.promise(new Function0<Result>() {
            @Override
            public Result apply() throws Throwable {
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
}