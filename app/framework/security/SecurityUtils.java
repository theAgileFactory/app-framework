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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import be.objectify.deadbolt.core.models.Role;
import be.objectify.deadbolt.core.models.Subject;
import be.objectify.deadbolt.java.DeadboltHandler;
import be.objectify.deadbolt.java.DynamicResourceHandler;
import be.objectify.deadbolt.java.JavaAnalyzer;
import be.objectify.deadbolt.java.actions.SubjectPresent;
import be.objectify.deadbolt.java.actions.SubjectPresentAction;
import be.objectify.deadbolt.java.actions.Unrestricted;
import be.objectify.deadbolt.java.cache.HandlerCache;
import be.objectify.deadbolt.java.cache.SubjectCache;
import framework.commons.IFrameworkConstants;
import framework.services.account.IAccountManagerPlugin;
import framework.services.account.IUserAccount;
import framework.services.session.IUserSessionManagerPlugin;
import framework.utils.Utilities;
import models.framework_models.account.SystemLevelRoleType;
import models.framework_models.account.SystemPermission;
import play.Configuration;
import play.Logger;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;

/**
 * An utility class which performs a test to be used in java code (instead of
 * annotations). It is based upon Deadbolt "DeadboltAnalyzer.hasRole(subject, "
 * admin")".<br/>
 * <b>WARNING:</b> The Deadbolt roles (see {@link Role} are actually
 * {@link SystemPermission}.<br/>
 * They are thus different from {@link SystemLevelRoleType}.
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class SecurityUtils {
    /**
     * Default timeout used for "blocking actions" such as checking the
     * restrictions
     */
    public static long DEFAULT_TIMEOUT = 5000l;

    private static Logger.ALogger log = Logger.of(SecurityUtils.class);
    @Inject
    private static JavaAnalyzer deadBoltAnalyzer;
    @Inject
    private static HandlerCache handlerCache;
    @Inject
    private static SubjectCache subjectCache;
    @Inject
    private static IAccountManagerPlugin accountManagerPlugin;
    @Inject
    private static IUserSessionManagerPlugin userSessionManagerPlugin;

    /**
     * Read the database to find the system preference which contains the
     * authentication mode.<br/>
     * If not found, use the default one configured in the system application
     * file.
     * 
     * @param configuration
     *            the play configuration
     * @return
     */
    public static IFrameworkConstants.AuthenticationMode getAuthenticationMode(Configuration configuration) {
        String authModeAsString = configuration.getString("maf.authentication.mode");
        try {
            // Creating an SQL connection "manually"
            // This is mandatory since the database access system might not be
            // yet activated
        } catch (Exception e) {
            log.error("Unable to read an authentication mode from the database, using the default one " + authModeAsString, e);
        }
        return IFrameworkConstants.AuthenticationMode.valueOf(authModeAsString);
    }

    /**
     * Check if there is a subject in the current session.<br/>
     * <ul>
     * <li>If YES : return a promise of Result using the function passed as a
     * parameter</li>
     * <li>If NO : redirect to the login page</li>
     * </ul>
     * 
     * @param ctx
     *            an http context
     * @param resultIfHasSubject
     *            a function to be used to compute a Promise of result
     * @return a promise of result
     */
    public static Promise<Result> checkHasSubject(final Http.Context ctx, final Function0<Result> resultIfHasSubject) {
        try {
            SubjectPresentAction subjectPresentAction = new SubjectPresentAction(getDeadBoltAnalyzer(), getSubjectCache(), getHandlerCache());
            subjectPresentAction.configuration = new SubjectPresent() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return SubjectPresent.class;
                }

                @Override
                public String handlerKey() {
                    return null;
                }

                @Override
                public boolean forceBeforeAuthCheck() {
                    return false;
                }

                @Override
                public boolean deferred() {
                    return false;
                }

                @Override
                public String content() {
                    return null;
                }
            };
            subjectPresentAction.delegate = new Action<String>() {
                @Override
                public Promise<Result> call(Context arg0) throws Throwable {
                    return Promise.promise(() -> resultIfHasSubject.apply());
                }

            };
            return subjectPresentAction.call(ctx);
        } catch (Throwable e) {
            log.error("Error while checking if the current context has a subject", e);
            return Promise.promise(new Function0<Result>() {
                @Override
                public Result apply() throws Throwable {
                    return Controller.badRequest();
                }
            });
        }
    }

    /**
     * Gets the {@link be.objectify.deadbolt.core.models.Subject} from the
     * {@link DeadboltHandler}, and logs an error if it's not present. Note that
     * at least one actions ({@link Unrestricted} does not not require a Subject
     * to be present.
     *
     * @param ctx
     *            the request context
     * @param deadboltHandler
     *            the Deadbolt handler
     * @return the Subject, if any
     */
    private static Promise<Optional<Subject>> getSubject(final Http.Context ctx, final DeadboltHandler deadboltHandler) {
        if (log.isDebugEnabled()) {
            log.debug("GET SUBJECT with subject cache [" + getSubjectCache() + "]");
        }
        return getSubjectCache().apply(deadboltHandler, ctx).map(option -> {
            if (!option.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.info("Subject not found in Deadbolt subject cache");
                }
            }
            return option;
        });
    }

    /**
     * Define if the current context (id) is allowed for a dynamic permission.
     * 
     * @param name
     *            the dynamic permission name
     * @param meta
     *            the meta, can be empty
     */
    public static boolean dynamic(String name, String meta) {
        if (log.isDebugEnabled()) {
            log.debug("Check dynamic permission with Handler Cache [" + getHandlerCache() + "]");
            log.debug("Check dynamic permission with Handler [" + getHandlerCache().get() + "]");
            log.debug("Check dynamic permission with Dynamic Handler [" + getHandlerCache().get().getDynamicResourceHandler(Http.Context.current()) + "]");
        }
        try {
            DeadboltHandler handler = getHandlerCache().get();
            DynamicResourceHandler dynamicResourceHandler = handler.getDynamicResourceHandler(Http.Context.current()).get(DEFAULT_TIMEOUT).get();
            return dynamicResourceHandler.isAllowed(name, meta, getHandlerCache().get(), Http.Context.current()).get(DEFAULT_TIMEOUT);
        } catch (Exception e) {
            log.error("Error while trying to check if a user is allowed for the permission name " + name + " and the meta information " + meta, e);
        }
        return false;
    }

    /**
     * Return true if the sign-in user is allowed for the given permission.
     * 
     * @param permission
     *            the permission
     */
    public static boolean isAllowed(String permission) {
        try {
            IUserAccount userAccount = getAccountManagerPlugin().getUserAccountFromUid(getUserSessionManagerPlugin().getUserSessionId(Http.Context.current()));
            return userAccount.getSystemPermissionNames().contains(permission);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Return true if the specified roles are part of the current user profile.
     * 
     * @param deadBoltRoles
     *            a list of roles ({@link SystemPermission} names within arrays
     *            are associated by AND, and arrays within the list are
     *            associated with OR)
     * @param ctx
     *            the play context
     * @return a boolean
     */
    public static boolean restrict(List<String[]> deadBoltRoles, Http.Context ctx) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("RESTRICT with handler cache [" + getHandlerCache() + "]");
                log.debug("RESTRICT with handler [" + getHandlerCache().get() + "] timeout [" + DEFAULT_TIMEOUT + "] for roles "
                        + Utilities.toString(deadBoltRoles));
            }
            Optional<Subject> subjectOption = getSubject(ctx, getHandlerCache().get()).get(DEFAULT_TIMEOUT);
            if (!subjectOption.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.debug("RESTRICT FALSE since no subject found");
                }
                return false;
            }
            Subject subject = subjectOption.get();
            if (log.isDebugEnabled()) {
                log.debug("RESTRICT Subject = " + subject);
            }
            return restrict(deadBoltRoles, subject);
        } catch (Throwable e) {
            log.error("Error while checking restriction for " + Utilities.toString(deadBoltRoles), e);
            return false;
        }
    }

    /**
     * Return true if the specified roles are part of the current user profile.
     * 
     * @param deadBoltRoles
     *            a list of roles ({@link SystemPermission} names within arrays
     *            are associated by AND, and arrays within the list are
     *            associated with OR)
     * @param uid
     *            the unique id of a user
     * @return a boolean
     */
    public static boolean restrict(List<String[]> deadBoltRoles, String uid) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("RESTRICT for uid [" + uid + "] [" + getHandlerCache().get() + "] timeout [" + DEFAULT_TIMEOUT + "] for roles "
                        + Utilities.toString(deadBoltRoles));
            }
            Subject subject = getAccountManagerPlugin().getUserAccountFromUid(uid);
            return restrict(deadBoltRoles, subject);
        } catch (Throwable e) {
            log.error("Error while checking restriction for " + Utilities.toString(deadBoltRoles), e);
            return false;
        }
    }

    /**
     * Return true if the specified roles are part of the current user profile.
     * 
     * @param deadBoltRoles
     *            a list of roles ({@link SystemPermission} names within arrays
     *            are associated by AND, and arrays within the list are
     *            associated with OR)
     * @param subject
     *            a {@link Subject}
     * @return a boolean
     */
    public static boolean restrict(List<String[]> deadBoltRoles, Subject subject) {
        try {
            if (subject == null) {
                return false;
            }
            for (String[] rolesArray : deadBoltRoles) {
                if (deadBoltAnalyzer.hasAllRoles(Optional.of(subject), rolesArray)) {
                    return true;
                }
            }
            return false;
        } catch (Throwable e) {
            log.error("Error while checking restriction for " + Utilities.toString(deadBoltRoles), e);
            return false;
        }
    }

    /**
     * Check if the subject has the given role.
     *
     * @param subject
     *            an option for the subject
     * @param roleName
     *            the name of the role
     * @return true iff the subject has the role represented by the role name
     */
    public static boolean hasRole(final Subject subject, final String roleName) {
        return getDeadBoltAnalyzer().hasRole(Optional.of(subject), roleName);
    }

    /**
     * Check if the specified subject has all the roles
     * 
     * @param subject
     *            a subject
     * @param roleNames
     *            an array of role names
     * @return true if the user has all the specified roles
     */
    public static boolean hasAllRoles(final Subject subject, final String[] roleNames) {
        return getDeadBoltAnalyzer().hasAllRoles(Optional.of(subject), roleNames);
    }

    private static JavaAnalyzer getDeadBoltAnalyzer() {
        return deadBoltAnalyzer;
    }

    private static HandlerCache getHandlerCache() {
        return handlerCache;
    }

    private static SubjectCache getSubjectCache() {
        return subjectCache;
    }

    private static IAccountManagerPlugin getAccountManagerPlugin() {
        return accountManagerPlugin;
    }

    private static IUserSessionManagerPlugin getUserSessionManagerPlugin() {
        return userSessionManagerPlugin;
    }
}
