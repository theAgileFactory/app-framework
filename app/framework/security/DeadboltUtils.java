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

import java.util.ArrayList;
import java.util.List;

import models.framework_models.account.SystemLevelRoleType;
import models.framework_models.account.SystemPermission;
import play.Logger;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import be.objectify.deadbolt.core.DeadboltAnalyzer;
import be.objectify.deadbolt.core.models.Role;
import be.objectify.deadbolt.core.models.Subject;
import be.objectify.deadbolt.java.utils.PluginUtils;
import framework.services.ServiceManager;
import framework.services.account.IAccountManagerPlugin;

/**
 * An utility class which performs a test to be used in java code (instead of
 * annotations). It is based upon Deadbolt
 * "DeadboltAnalyzer.hasRole(subject, "admin")".<br/>
 * <b>WARNING:</b> The Deadbolt roles (see {@link Role} are actually
 * {@link SystemPermission}.<br/>
 * They are thus different from {@link SystemLevelRoleType}.
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class DeadboltUtils {
    private static Logger.ALogger log = Logger.of(DeadboltUtils.class);

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
    public static Promise<Result> checkHasSubject(Http.Context ctx, Function0<Result> resultIfHasSubject) {
        try {
            if (PluginUtils.getDeadboltHandler().getSubject(ctx) != null) {
                return Promise.promise(resultIfHasSubject);
            }
            return PluginUtils.getDeadboltHandler().onAuthFailure(ctx, null);
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
     * Check that a subject exists (= a user is logged)
     * 
     * @param ctx
     *            the play context
     * @return a boolean
     */
    public static boolean hasSubject(Http.Context ctx) {
        try {
            return PluginUtils.getDeadboltHandler().getSubject(ctx) != null;
        } catch (Throwable e) {
            log.error("Error while checking if the current context has a subject", e);
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
            Subject subject = PluginUtils.getDeadboltHandler().getSubject(ctx);
            return restrict(deadBoltRoles, subject);
        } catch (Throwable e) {
            log.error("Error while checking restriction for " + deadBoltRoles, e);
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
            IAccountManagerPlugin accountManagerPlugin = ServiceManager.getService(IAccountManagerPlugin.NAME, IAccountManagerPlugin.class);
            Subject subject = accountManagerPlugin.getUserAccountFromUid(uid);
            return restrict(deadBoltRoles, subject);
        } catch (Throwable e) {
            log.error("Error while checking restriction for " + deadBoltRoles, e);
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
                if (DeadboltAnalyzer.hasAllRoles(subject, rolesArray)) {
                    return true;
                }
            }
            return false;
        } catch (Throwable e) {
            log.error("Error while checking restriction for " + deadBoltRoles, e);
            return false;
        }
    }

    /**
     * Deadbolt expects a certain structure for the permissions statements.<br/>
     * The basic structure is a List of array of String (AND between the
     * permissions in an array and OR between the arrays in the list). This
     * method takes an array as a parameter and creates a list of array (one
     * array per value of the array passed as a parameter). This creates a
     * permission statement of ORed permissions.
     * 
     * @param values
     *            an array of permissions (to be associated with or)
     * @return
     */
    public static List<String[]> getListOfArray(String... values) {
        ArrayList<String[]> list = new ArrayList<String[]>();
        for (String value : values) {
            list.add(new String[] { value });
        }
        return list;
    }
}
