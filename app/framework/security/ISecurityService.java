package framework.security;

import java.util.List;

import be.objectify.deadbolt.core.models.Subject;
import framework.services.account.AccountManagementException;
import framework.services.account.IUserAccount;
import models.framework_models.account.SystemPermission;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * A service which implements various security related features
 * 
 * @author Pierre-Yves Cloux
 */
public interface ISecurityService {
    /**
     * Default timeout used for "blocking actions" such as checking the
     * restrictions
     */
    public static long DEFAULT_TIMEOUT = 5000l;

    /**
     * Return the currently logged user
     * 
     * @return a user account
     */
    public IUserAccount getCurrentUser() throws AccountManagementException;

    /**
     * Return the used associated with the specified uid
     * 
     * @param uid
     *            a unique user id (the user login)
     * @return a user account
     */
    public IUserAccount getUserFromUid(String uid) throws AccountManagementException;

    /**
     * Return the user associated with the specified object id
     * 
     * @param id
     *            a unique id (the BizDock object id)
     * @return a user account
     */
    public IUserAccount getUserFromId(Long id) throws AccountManagementException;

    /**
     * Return the user associated with the specified mail
     * 
     * @param mail
     *            the mail from the user
     * @return a user account
     */
    public IUserAccount getUserFromEmail(String mail) throws AccountManagementException;

    /**
     * Check if there is a subject in the current session.<br/>
     * <ul>
     * <li>If YES : return a promise of Result using the function passed as a
     * parameter</li>
     * <li>If NO : redirect to the login page</li>
     * </ul>
     * 
     * @param resultIfHasSubject
     *            a function to be used to compute a Promise of result
     * @return a promise of result
     */
    public Promise<Result> checkHasSubject(Function0<Promise<Result>> resultIfHasSubject);

    /**
     * Define if the current context (id) is allowed for a dynamic permission.
     * 
     * @param name
     *            the dynamic permission name
     * @param meta
     *            the meta, can be empty
     */
    public boolean dynamic(String name, String meta);

    /**
     * Define if the current context (id) is allowed for a dynamic permission.
     * 
     * @param name
     *            the dynamic permission name
     * @param meta
     *            the meta, can be empty
     * @param id
     *            the id of an object
     */
    public boolean dynamic(String name, String meta, Long id);

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
    public boolean restrict(List<String[]> deadBoltRoles);

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
    public boolean restrict(List<String[]> deadBoltRoles, String uid);

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
    public boolean restrict(List<String[]> deadBoltRoles, Subject subject);

    /**
     * Check if the subject has the given role.
     *
     * @param roleName
     *            the name of the role
     * @param subject
     *            an option for the subject
     * @return true iff the subject has the role represented by the role name
     */
    public boolean restrict(final String roleName, final Subject subject);

    /**
     * Check if the specified subject has all the roles
     * 
     * @param roleNames
     *            an array of role names
     * @param subject
     *            a subject
     * @return true if the user has all the specified roles
     */
    public boolean restrict(final String[] roleNames, final Subject subject);

    /**
     * Check if the current user has the given role.
     *
     * @param roleName
     *            the name of the role
     * @return true iff the subject has the role represented by the role name
     */
    public boolean restrict(String roleName) throws AccountManagementException;

    /**
     * Check if the current user has all the roles
     * 
     * @param roleNames
     *            an array of role names
     * @return true if the user has all the specified roles
     */
    public boolean restrict(String[] roleNames) throws AccountManagementException;

}