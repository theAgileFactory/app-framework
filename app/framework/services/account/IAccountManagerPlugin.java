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

import java.util.List;

import framework.services.account.IUserAccount.AccountType;
import models.framework_models.account.Principal;

/**
 * The interface to be implemented by the module which manages the user
 * creation/deletion and modification.<br/>
 * The plugin has multiple configuration option (which can be changed in the
 * application.conf file):
 * <ul>
 * <li>maf.ic_ldap_master : set to true if the master mode is activated</li>
 * <li>maf.ic_self_mail_update_allowed : set to true if the user can update his
 * e-mail himself</li>
 * </ul>
 * <p>
 * <strong>Master mode</strong> "Master mode" means that the password is managed
 * by the system while "Slave mode" means that an external authentication system
 * is managing the password.
 * </p>
 * 
 * @author Pierre-Yves Cloux
 */
public interface IAccountManagerPlugin {
    public static final String NAME = "accountManagerPlugin";

    /**
     * Return true if the plugin is configured in "master mode"
     * 
     * @return a boolean
     */
    public boolean isAuthenticationRepositoryMasterMode();

    /**
     * Return true if a user is allowed to change his/her e-mail himself.<br/>
     * If false this means that the e-mail will be retrieved from the
     * authentication back-end and assumed being correct at any time.
     * 
     * @return a boolean
     */
    public boolean isSelfMailUpdateAllowed();

    /**
     * Return true if the e-mail already exists in the database
     * 
     * @param mail
     *            an e-mail address
     * @return true if the specified e-mail address already exists
     * @throws AccountManagementException
     */
    public boolean isMailExistsInAuthenticationBackEnd(String mail) throws AccountManagementException;

    /**
     * Return true if the specified user id already exists in the database as
     * well as in the back-end authentication system
     * 
     * @param uid
     *            a unique Id
     * @return true if the specified uid already exists
     * @throws AccountManagementException
     */
    public boolean isUserIdExists(String uid) throws AccountManagementException;

    /**
     * Return true if the specified user id already exists in the authentication
     * back-end.<br/>
     * Whether the uid exists in the database or not will not change the result.
     * 
     * @param uid
     *            a unique Id
     * @return true if the specified uid already exists
     * @throws AccountManagementException
     */
    public boolean isUserIdExistsInAuthenticationBackEnd(String uid) throws AccountManagementException;

    /**
     * Update the password of the specified user profile
     * 
     * @param uid
     *            the unique user ID
     * @param password
     *            the new password
     * @throws AccountManagementException
     */
    public void updatePassword(String uid, String password) throws AccountManagementException;

    /**
     * Add a system level role type to the specified user
     * 
     * @param uid
     *            the unique user ID
     * @param systemLevelRoleTypeName
     *            the name of a system level role
     * @throws AccountManagementException
     */
    public void addSystemLevelRoleType(String uid, String systemLevelRoleTypeName) throws AccountManagementException;

    /**
     * Remove a system level role type from the specified user
     * 
     * @param uid
     *            the unique user ID
     * @param systemLevelRoleTypeName
     *            the name of a system level role
     * @throws AccountManagementException
     */
    public void removeSystemLevelRoleType(String uid, String systemLevelRoleTypeName) throws AccountManagementException;

    /**
     * Add a list of system level role type to the specified user
     * 
     * @param uid
     *            the unique user ID
     * @param systemLevelRoleTypeNames
     *            a list of system level role type names
     * @throws AccountManagementException
     */
    public void addSystemLevelRoleTypes(String uid, List<String> systemLevelRoleTypeNames) throws AccountManagementException;

    /**
     * Remove a list of system level role type from the specified user
     * 
     * @param uid
     *            the unique user ID
     * @param systemLevelRoleTypeNames
     *            a list of system level role type names
     * @throws AccountManagementException
     */
    public void removeSystemLevelRoleTypes(String uid, List<String> systemLevelRoleTypeNames) throws AccountManagementException;

    /**
     * Add or remove the system level role type from the user account so that
     * the resulting list of system level role type for the specified user is
     * the one passed as a parameter
     * 
     * @param uid
     *            the unique user ID
     * @param systemLevelRoleTypeNames
     *            a list of system level role type names
     * @throws AccountManagementException
     */
    public void overwriteSystemLevelRoleTypes(String uid, List<String> systemLevelRoleTypeNames) throws AccountManagementException;

    /**
     * Creates a new user account.<br/>
     * A new user account is systematically created as "non-active".<br/>
     * 
     * @param uid
     *            the unique user ID
     * @param accountType
     *            the user account type (see {@link IUserAccount})
     * @param firstName
     *            the user first name
     * @param lastName
     *            the user last name
     * @param mail
     *            an e-mail address
     * @param systemLevelRoleTypeNames
     *            a list of system level role type names
     * 
     * @throws AccountManagementException
     */
    public void createNewUserAccount(String uid, AccountType accountType, String firstName, String lastName, String mail,
            List<String> systemLevelRoleTypeNames) throws AccountManagementException;

    /**
     * If a user account exists in the authentication back-end as well as the
     * user database this method attempt to "repair" any issues with the
     * provisioning links (either missing links or links in error).<br/>
     * 
     * @param uid
     *            the unique user ID
     * @return true if the system was able to repair the missing links with
     *         third party systems
     * @throws AccountManagementException
     */
    public void resync(String uid) throws AccountManagementException;

    /**
     * Update the basic data of a user account.<br/>
     * If you don't want to update the e-mail address then you could simply put
     * it as null. If you want to update "only" the e-mail address you can set
     * the first/last name as null.
     * 
     * @param uid
     *            the unique user ID
     * @param firstName
     *            the user first name
     * @param lastName
     *            the user last name
     * @throws AccountManagementException
     */
    public void updateBasicUserData(String uid, String firstName, String lastName) throws AccountManagementException;

    /**
     * Update the e-mail address of a user account
     * 
     * @param uid
     *            the unique user ID
     * @param mail
     *            an e-mail address
     * @throws AccountManagementException
     */
    public void updateMail(String uid, String mail) throws AccountManagementException;

    /**
     * Update the activation status for this user.<br/>
     * WARNING: if MAF is configured in "master mode" the status change is
     * propagated to the authentication backend
     * 
     * @param uid
     *            the unique user ID
     * @param isActive
     *            true if the user is active
     * @throws AccountManagementException
     */
    public void updateActivationStatus(String uid, boolean isActive) throws AccountManagementException;

    /**
     * Update the user account type for the specified user.<br/>
     * 
     * @param uid
     *            the unique user ID
     * @param accountType
     *            the type of the user account
     * 
     * @throws AccountManagementException
     */
    public void updateUserAccountType(String uid, AccountType accountType) throws AccountManagementException;

    /**
     * update the preferred language of a user (principal table)
     * 
     * @param uid
     *            the the unique user ID
     * @param preferredLanguage
     *            the preferred language (ISO 2 letters)
     * @throws AccountManagementException
     */
    void updatePreferredLanguage(String uid, String preferredLanguage) throws AccountManagementException;

    /**
     * Delete the account associated with the specified user id.<br/>
     * The plugins are notified with the deletion.
     * 
     * @param uid
     *            the unique user ID
     */
    public void deleteAccount(String uid) throws AccountManagementException;

    /**
     * Generate a new validation key stored in the user profile.<br/>
     * This key can be subsequently checked before performing some actions such
     * as mail update or password change.<br/>
     * The provided "validationData" contains a string which is to be used once
     * the validation completes (such as an email address or a ciphered
     * password).
     * 
     * @param uid
     *            the unique user ID
     * @param validationData
     *            the data stored to a later use and associated with this
     *            validation key
     * @return an uuid
     */
    public String getValidationKey(String uid, String validationData) throws AccountManagementException;

    /**
     * Check if the validation key is valid for the specified user and return
     * the associated validation data.
     * 
     * @param uid
     *            the unique user ID
     * @param validationKey
     *            an uuid
     * @return the validation data associated with this validation key if it is
     *         valid
     * @throws AccountManagementException
     */
    public String checkValidationKey(String uid, String validationKey) throws AccountManagementException;

    /**
     * Reset any existing validation key
     * 
     * @param uid
     *            the unique user ID
     * @throws AccountManagementException
     */
    public void resetValidationKey(String uid) throws AccountManagementException;

    /**
     * Retrieve the user profile from the user authentication back-end
     * 
     * @param mafUid
     *            the unique user Id which is actually the {@link Principal} id
     * @return a IUserProfile instance
     * @throws AccountManagementException
     */
    public IUserAccount getUserAccountFromMafUid(Long mafUid) throws AccountManagementException;

    /**
     * Retrieve the user profile from the user authentication back-end
     * 
     * @param uid
     *            the unique user Id
     * @return a IUserProfile instance
     * @throws AccountManagementException
     */
    public IUserAccount getUserAccountFromUid(String uid) throws AccountManagementException;

    /**
     * Retrieve the user profile associated with the specified e-mail
     * 
     * @param mail
     *            an e-mail address
     * @return a IUserProfile instance
     * @throws AccountManagementException
     */
    public IUserAccount getUserAccountFromEmail(String mail) throws AccountManagementException;

    /**
     * Retrieve the user profiles from the name of the user
     * 
     * @param nameCriteria
     *            the name of a user (you may use jokers "*")
     * @return a IUserProfile instance
     * @throws AccountManagementException
     */
    public List<IUserAccount> getUserAccountsFromName(String nameCriteria) throws AccountManagementException;

    /**
     * When a modification occurs, invalidate the user account cache
     */
    public void invalidateUserAccountCache(String uid);

    /**
     * Invalidate the cache for all the user accounts
     */
    public void invalidateAllUserAccountsCache() throws AccountManagementException;

}
