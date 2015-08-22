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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.RandomStringUtils;

import com.avaje.ebean.Ebean;

import framework.commons.DataType;
import framework.commons.IFrameworkConstants;
import framework.commons.message.EventMessage;
import framework.commons.message.UserEventMessage;
import framework.services.account.IUserAccount.AccountType;
import framework.services.database.IDatabaseDependencyService;
import framework.services.plugins.IEventBroadcastingService;
import models.framework_models.account.Principal;
import models.framework_models.account.SystemLevelRole;
import models.framework_models.account.SystemLevelRoleType;
import models.framework_models.account.SystemPermission;
import play.Configuration;
import play.Logger;
import play.cache.Cache;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;

/**
 * The plugin managing the user accounts.<br/>
 * The plugin is based on a cache.<br/>
 * <b>WARNING</b>: remember to call invalidateUserAccountCache(uid) each time a
 * user account is modified.<br/>
 * <ul>
 * <li>authenticationRepositoryMasterMode : true if the system is configured in
 * "master mode".</li>
 * <li>selfMailUpdateAllowed : true if the user can update his e-mail himself
 * </li>
 * <li>authenticationAccountWriterPlugin : the plugin to be used to update the
 * authentication back-end (if master mode)</li>
 * <li>authenticationAccountReaderPlugin : the plugin to be used to read the
 * authentication back-end</li>
 * <li>eventBroadcastingService : the service that manages the notification of
 * the third party</li> plugins
 * <li>validationKeyValidity : how much time a validation key is valid in
 * minutes (validation for password change, account creation of e-mail update)
 * </li>
 * <li>commonUserAccountClass : the class which implements the
 * {@link ICommonUserAccount} interface and which is to be used as the basis of
 * the {@link IUserAccount}</li>
 * </ul>
 * 
 * 
 * @author Pierre-Yves Cloux
 * 
 */
@Singleton
public class AccountManagerPluginImpl implements IAccountManagerPlugin {
    private static Logger.ALogger log = Logger.of(AccountManagerPluginImpl.class);
    private boolean authenticationRepositoryMasterMode;
    private boolean selfMailUpdateAllowed;
    private IAuthenticationAccountWriterPlugin authenticationAccountWriterPlugin;
    private IAuthenticationAccountReaderPlugin authenticationAccountReaderPlugin;
    private IEventBroadcastingService eventBroadcastingService;
    private int validationKeyValidity;
    private int userAccountCacheDurationInSeconds;
    private Class<?> commonUserAccountClass;

    public enum Config {
        SELF_MAIL_UPDATE_ALLOWED("maf.ic_self_mail_update_allowed"), ACCOUNT_CACHE_DURATION("maf.user_account_cache_duration"), VALIDATION_KEY_VALIDITY(
                "maf.validation.key.validity");

        private String configurationKey;

        private Config(String configurationKey) {
            this.configurationKey = configurationKey;
        }

        public String getConfigurationKey() {
            return configurationKey;
        }
    }

    /**
     * Creates a new AccountManagerPluginImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param commonUserAccountClassName
     *            the class to be used for user accounts
     * @param authenticationRepositoryMasterMode
     *            if true the system is in LDAP master mode (LDAP is writable)
     * @param authenticationAccountWriterPlugin
     * @param authenticationAccountReaderPlugin
     * @param eventBroadcastingService
     * @param databaseDependencyService
     * @throws ClassNotFoundException
     */
    @Inject
    public AccountManagerPluginImpl(ApplicationLifecycle lifecycle, Configuration configuration,
            @Named("UserAccountClassName") String commonUserAccountClassName,
            @Named("AuthenticationRepositoryMasterMode") Boolean authenticationRepositoryMasterMode,
            IAuthenticationAccountWriterPlugin authenticationAccountWriterPlugin, IAuthenticationAccountReaderPlugin authenticationAccountReaderPlugin,
            IEventBroadcastingService eventBroadcastingService, IDatabaseDependencyService databaseDependencyService) throws ClassNotFoundException {
        log.info("SERVICE>>> AccountManagerPluginImpl starting...");
        this.authenticationRepositoryMasterMode = authenticationRepositoryMasterMode;
        this.userAccountCacheDurationInSeconds = configuration.getInt(Config.ACCOUNT_CACHE_DURATION.getConfigurationKey());
        this.selfMailUpdateAllowed = configuration.getBoolean(Config.SELF_MAIL_UPDATE_ALLOWED.getConfigurationKey());
        this.validationKeyValidity = configuration.getInt(Config.VALIDATION_KEY_VALIDITY.getConfigurationKey());

        log.info("LDAP master mode is " + authenticationRepositoryMasterMode);
        log.info("Loading the user account class " + commonUserAccountClassName);
        this.commonUserAccountClass = Class.forName(commonUserAccountClassName);
        if (!ICommonUserAccount.class.isAssignableFrom(commonUserAccountClass) || !hasDefaultConstructor(commonUserAccountClass)) {
            throw new IllegalArgumentException(
                    "The class " + commonUserAccountClass + " cannot implement the ICommonUserAccount interface or has no default constructor");
        }
        this.authenticationAccountWriterPlugin = authenticationAccountWriterPlugin;
        this.authenticationAccountReaderPlugin = authenticationAccountReaderPlugin;
        this.eventBroadcastingService = eventBroadcastingService;
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> AccountManagerPluginImpl stopping...");
            log.info("SERVICE>>> AccountManagerPluginImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> AccountManagerPluginImpl started");
    }

    private boolean hasDefaultConstructor(Class<?> aClass) {
        try {
            aClass.getConstructor(new Class<?>[] {});
        } catch (NoSuchMethodException e) {
            return false;
        } catch (SecurityException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isAuthenticationRepositoryMasterMode() {
        return this.authenticationRepositoryMasterMode;
    }

    @Override
    public boolean isSelfMailUpdateAllowed() {
        return this.selfMailUpdateAllowed;
    }

    @Override
    public boolean isMailExistsInAuthenticationBackEnd(String mail) throws AccountManagementException {
        boolean exists = getAuthenticationAccountReaderPlugin().isMailAlreadyExist(mail);
        if (log.isDebugEnabled()) {
            log.debug("Check if user mail " + mail + " exists in back-end and result is " + exists);
        }
        return exists;
    }

    @Override
    public boolean isUserIdExists(String uid) throws AccountManagementException {
        Principal userPrincipal = findPrincipalFromUid(uid);
        if (userPrincipal == null) {
            return false;
        }
        if (!getAuthenticationAccountReaderPlugin().isUidAlreadyExist(uid)) {
            throwInconsistencyException(uid);
        }
        return true;
    }

    @Override
    public boolean isUserIdExistsInAuthenticationBackEnd(String uid) throws AccountManagementException {
        boolean exists = getAuthenticationAccountReaderPlugin().isUidAlreadyExist(uid);
        if (log.isDebugEnabled()) {
            log.debug("Check if user " + uid + " exists in back-end and result is " + exists);
        }
        return exists;
    }

    @Override
    public void updatePassword(String uid, String password) throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Changing the password of the account " + uid);
        }
        invalidateUserAccountCache(uid);
        if (isAuthenticationRepositoryMasterMode()) {
            getAuthenticationAccountWriterPlugin().changePassword(uid, password);
        }
    }

    @Override
    public void createNewUserAccount(String uid, AccountType accountType, String firstName, String lastName, String mail, List<String> systemLevelRoleTypeNames)
            throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Creating a new user account  " + uid + " with " + firstName + " " + lastName + " " + mail + " " + accountType + " "
                    + systemLevelRoleTypeNames);
        }
        // Check the consistency
        checkPrincipalExistsInDatabase(uid);
        Ebean.beginTransaction();
        try {
            // Create the entry in the database
            Principal userPrincipal = new Principal();
            userPrincipal.uid = uid;
            userPrincipal.accountType = accountType.name();
            userPrincipal.isActive = true;
            userPrincipal.isDisplayed = true;
            userPrincipal.save();
            if (log.isDebugEnabled()) {
                log.debug("Creating a new user account  " + uid + " principal saved");
            }

            addingRolesToPrincipalForAccountType(userPrincipal, accountType, systemLevelRoleTypeNames);
            userPrincipal.save();
            if (log.isDebugEnabled()) {
                log.debug("Creating a new user account  " + uid + " roles added");
            }

            getAuthenticationAccountWriterPlugin().createUserProfile(uid, firstName, lastName, mail, RandomStringUtils.random(10));
            if (log.isDebugEnabled()) {
                log.debug("Authentication backend for  " + uid + " inserted");
            }

            // Notify the plugins of the event
            UserEventMessage eventMessage = new UserEventMessage(userPrincipal.id, DataType.getDataType(IFrameworkConstants.User),
                    UserEventMessage.MessageType.OBJECT_CREATED);
            postToPlugginManagerService(eventMessage);

            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Error during the creation of the account uid=%s", uid);
            log.error(message, e);
            throw new AccountManagementException(message, e);
        } finally {
            Ebean.endTransaction();
        }
        log.info(String.format("User created with uid %s", uid));
    }

    @Override
    public void resync(String uid) throws AccountManagementException {
        invalidateUserAccountCache(uid);
        // Check that the user exists
        if (!isUserIdExists(uid)) {
            String message = String.format("Unable to resync user %s, this one does not exists in the user database", uid);
            log.error(message);
            throw new AccountManagementException(message);
        }

        Principal principal = findPrincipalFromUid(uid);
        // Notify the MAF modules for resync
        UserEventMessage eventMessage = new UserEventMessage(principal.id, DataType.getDataType(IFrameworkConstants.User), UserEventMessage.MessageType.RESYNC);
        postToPlugginManagerService(eventMessage);
    }

    @Override
    public void updateBasicUserData(String uid, String firstName, String lastName) throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Updating the user basic data for account uid  " + uid + " with " + firstName + " " + lastName);
        }
        invalidateUserAccountCache(uid);

        getAuthenticationAccountWriterPlugin().updateUserProfile(uid, firstName, lastName, null);

        Principal principal = findPrincipalFromUid(uid);
        // Notify the MAF modules
        UserEventMessage eventMessage = new UserEventMessage(principal.id, DataType.getDataType(IFrameworkConstants.User),
                UserEventMessage.MessageType.OBJECT_UPDATED);
        postToPlugginManagerService(eventMessage);

        log.info(String.format("User basic data updated with uid %s", uid));
    }

    @Override
    public void updatePreferredLanguage(String uid, String preferredLanguage) throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Changing the user preferred language for account uid  " + uid + " with " + preferredLanguage);
        }
        invalidateUserAccountCache(uid);
        Principal userPrincipal = findPrincipalAndCheckForAuthenticationBackEndConsistency(uid);
        userPrincipal.preferredLanguage = preferredLanguage;
        userPrincipal.save();

        // Notify the MAF modules
        UserEventMessage eventMessage = new UserEventMessage(userPrincipal.id, DataType.getDataType(IFrameworkConstants.User),
                UserEventMessage.MessageType.OBJECT_UPDATED);
        postToPlugginManagerService(eventMessage);

        log.info(String.format("User preferred language updated with uid %s and preferred language %s", uid, preferredLanguage));
    }

    @Override
    public void updateUserAccountType(String uid, AccountType accountType) throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Changing the user account type for account uid  " + uid + " with account type " + accountType);
        }
        invalidateUserAccountCache(uid);
        Principal userPrincipal = findPrincipalAndCheckForAuthenticationBackEndConsistency(uid);
        userPrincipal.accountType = accountType.name();
        userPrincipal.removeAllSystemLevelRoles();
        addingRolesToPrincipalForAccountType(userPrincipal, accountType, null);
        userPrincipal.save();

        // Notify the MAF modules
        UserEventMessage eventMessage = new UserEventMessage(userPrincipal.id, DataType.getDataType(IFrameworkConstants.User),
                UserEventMessage.MessageType.OBJECT_UPDATED);
        postToPlugginManagerService(eventMessage);

        log.info(String.format("User account type updated with uid %s and account type %s", uid, accountType.name()));
    }

    @Override
    public void updateMail(String uid, String mail) throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Changing the mail for account uid  " + uid + " with mail " + mail);
        }
        invalidateUserAccountCache(uid);

        getAuthenticationAccountWriterPlugin().updateUserProfile(uid, null, null, mail);
        if (log.isDebugEnabled()) {
            log.debug("User profile in backend updated for  " + uid + " with mail " + mail);
        }

        // Notify the MAF modules
        Principal principal = findPrincipalFromUid(uid);
        UserEventMessage eventMessage = new UserEventMessage(principal.id, DataType.getDataType(IFrameworkConstants.User),
                UserEventMessage.MessageType.OBJECT_UPDATED);
        postToPlugginManagerService(eventMessage);

        log.info(String.format("User mail updated for user %s with %s", uid, mail));
    }

    @Override
    public void updateActivationStatus(String uid, boolean isActive) throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Updating the activation status for account uid  " + uid + " with " + isActive);
        }
        invalidateUserAccountCache(uid);
        Ebean.beginTransaction();
        try {
            // Create the entry in the database
            Principal userPrincipal = findPrincipalAndCheckForAuthenticationBackEndConsistency(uid);
            userPrincipal.isActive = isActive;
            userPrincipal.save();

            getAuthenticationAccountWriterPlugin().changeActivationStatus(uid, isActive);

            // Notify the MAF modules
            UserEventMessage eventMessage = new UserEventMessage(userPrincipal.id, DataType.getDataType(IFrameworkConstants.User),
                    UserEventMessage.MessageType.OBJECT_STATUS_CHANGED);
            postToPlugginManagerService(eventMessage);

            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Error during the update of the activation status of account uid=%s", uid);
            log.error(message, e);
            throw new AccountManagementException(message, e);
        } finally {
            Ebean.endTransaction();
        }
        log.info(String.format("Activation status changed for user %s with %s", uid, isActive));
    }

    @Override
    public void deleteAccount(final String uid) throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Deleting account uid  " + uid);
        }
        invalidateUserAccountCache(uid);
        Ebean.beginTransaction();
        try {
            Principal userPrincipal = findPrincipalAndCheckForAuthenticationBackEndConsistency(uid);
            Long previousUserId = userPrincipal.id;
            String previousUserUid = userPrincipal.uid;

            // Invalidate the cache
            invalidateUserAccountCache(uid);

            // Delete physically the account
            if (log.isDebugEnabled()) {
                log.debug("Deleting the principal with id  " + previousUserId);
            }
            userPrincipal.delete(); // Physically delete the user
            if (log.isDebugEnabled()) {
                log.debug("Deleting account uid  " + uid);
            }
            getAuthenticationAccountWriterPlugin().deleteUserProfile(uid);

            Ebean.commitTransaction();

            // Notify the MAF modules with the appropriate payload
            UserEventMessage eventMessage = new UserEventMessage(previousUserId, DataType.getDataType(IFrameworkConstants.User),
                    UserEventMessage.MessageType.OBJECT_DELETED);
            UserEventMessage.PayLoad payload = new UserEventMessage.PayLoad();
            payload.setDeletedUid(previousUserUid);
            eventMessage.setPayload(payload);
            postToPlugginManagerService(eventMessage);
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Error during the mark for deletion of the account uid=%s", uid);
            log.error(message, e);
            throw new AccountManagementException(message, e);
        } finally {
            Ebean.endTransaction();
        }
        log.info(String.format("User marked for deletion with uid %s", uid));
    }

    @Override
    public void addSystemLevelRoleTypes(String uid, List<String> systemLevelRoleTypeNames) throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Adding roles " + systemLevelRoleTypeNames + " for user account with uid  " + uid);
        }
        invalidateUserAccountCache(uid);
        Ebean.beginTransaction();
        try {
            Principal userPrincipal = findPrincipalAndCheckForAuthenticationBackEndConsistency(uid);
            if (!userPrincipal.getAccountTypeAsObject().isRolesEditable())
                return;
            for (String systemLevelRoleTypeName : systemLevelRoleTypeNames) {
                userPrincipal.addSystemLevelRole(getAllRoles().get(systemLevelRoleTypeName));
            }

            // Notify the MAF modules
            UserEventMessage eventMessage = new UserEventMessage(userPrincipal.id, DataType.getDataType(IFrameworkConstants.User),
                    UserEventMessage.MessageType.OBJECT_UPDATED);
            postToPlugginManagerService(eventMessage);

            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Error during the addition of a set of groups for uid=%s", uid);
            log.error(message, e);
            throw new AccountManagementException(message, e);
        } finally {
            Ebean.endTransaction();
        }
        log.info(String.format("User groups added for uid %s with groups", uid, systemLevelRoleTypeNames));
    }

    @Override
    public void removeSystemLevelRoleTypes(String uid, List<String> systemLevelRoleTypeNames) throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Removing roles " + systemLevelRoleTypeNames + " for user account with uid  " + uid);
        }
        invalidateUserAccountCache(uid);
        try {
            Principal userPrincipal = findPrincipalAndCheckForAuthenticationBackEndConsistency(uid);
            if (!userPrincipal.getAccountTypeAsObject().isRolesEditable())
                return;
            for (String groupName : systemLevelRoleTypeNames) {
                userPrincipal.removeSystemLevelRole(getAllRoles().get(groupName));
            }

            // Notify the MAF modules
            UserEventMessage eventMessage = new UserEventMessage(userPrincipal.id, DataType.getDataType(IFrameworkConstants.User),
                    UserEventMessage.MessageType.OBJECT_UPDATED);
            postToPlugginManagerService(eventMessage);

        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Error during the removal of a set of groups for uid=%s", uid);
            log.error(message, e);
            throw new AccountManagementException(message, e);
        } finally {
            Ebean.endTransaction();
        }
        log.info(String.format("User groups removed for uid %s with groups", uid, systemLevelRoleTypeNames));
    }

    @Override
    public void addSystemLevelRoleType(String uid, String systemLevelRoleTypeName) throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Adding role " + systemLevelRoleTypeName + " for user account with uid  " + uid);
        }
        invalidateUserAccountCache(uid);
        Principal userPrincipal = findPrincipalAndCheckForAuthenticationBackEndConsistency(uid);
        if (!userPrincipal.getAccountTypeAsObject().isRolesEditable())
            return;
        userPrincipal.addSystemLevelRole(getAllRoles().get(systemLevelRoleTypeName));

        // Notify the MAF modules
        UserEventMessage eventMessage = new UserEventMessage(userPrincipal.id, DataType.getDataType(IFrameworkConstants.User),
                UserEventMessage.MessageType.OBJECT_UPDATED);
        postToPlugginManagerService(eventMessage);

        log.info(String.format("Group %s added for uid %s", systemLevelRoleTypeName, uid));
    }

    @Override
    public void removeSystemLevelRoleType(String uid, String systemLevelRoleTypeName) throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Removing role " + systemLevelRoleTypeName + " for user account with uid  " + uid);
        }
        invalidateUserAccountCache(uid);
        Principal userPrincipal = findPrincipalAndCheckForAuthenticationBackEndConsistency(uid);
        if (!userPrincipal.getAccountTypeAsObject().isRolesEditable())
            return;
        userPrincipal.removeSystemLevelRole(getAllRoles().get(systemLevelRoleTypeName));

        // Notify the MAF modules
        UserEventMessage eventMessage = new UserEventMessage(userPrincipal.id, DataType.getDataType(IFrameworkConstants.User),
                UserEventMessage.MessageType.OBJECT_UPDATED);
        postToPlugginManagerService(eventMessage);

        log.info(String.format("Group %s removed for uid %s", systemLevelRoleTypeName, uid));
    }

    @Override
    public void overwriteSystemLevelRoleTypes(String uid, List<String> systemLevelRoleTypeNames) throws AccountManagementException {
        invalidateUserAccountCache(uid);
        if (log.isDebugEnabled()) {
            log.debug("Overwritting roles for user account with uid  " + uid + " with " + systemLevelRoleTypeNames);
        }
        Ebean.beginTransaction();
        try {
            Principal userPrincipal = findPrincipalAndCheckForAuthenticationBackEndConsistency(uid);
            if (!userPrincipal.getAccountTypeAsObject().isRolesEditable())
                return;

            // Add all the roles
            for (String groupName : systemLevelRoleTypeNames) {
                userPrincipal.addSystemLevelRole(getAllRoles().get(groupName));
            }

            // Check the roles to be deleted
            for (SystemLevelRole systemLevelRole : userPrincipal.systemLevelRoles) {
                if (systemLevelRole.systemLevelRoleType != null && !systemLevelRole.systemLevelRoleType.deleted) {
                    if (!systemLevelRoleTypeNames.contains(systemLevelRole.systemLevelRoleType.getName())) {
                        userPrincipal.removeSystemLevelRole(systemLevelRole.systemLevelRoleType);
                    }
                }
            }

            // Notify the MAF modules
            UserEventMessage eventMessage = new UserEventMessage(userPrincipal.id, DataType.getDataType(IFrameworkConstants.User),
                    UserEventMessage.MessageType.OBJECT_UPDATED);
            postToPlugginManagerService(eventMessage);

            Ebean.commitTransaction();

        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Error during the overwrite of a set of groups for uid=%s", uid);
            log.error(message, e);
            throw new AccountManagementException(message, e);
        } finally {
            Ebean.endTransaction();
        }
        log.info(String.format("User groups overwritten for uid %s with groups", uid, systemLevelRoleTypeNames));
    }

    @Override
    public IUserAccount getUserAccountFromUid(String uid) throws AccountManagementException {
        IUserAuthenticationAccount userAuthenticationAccount = getAuthenticationAccountReaderPlugin().getAccountFromUid(uid);
        if (log.isDebugEnabled()) {
            log.debug("Looking for user account with uid  " + uid + " " + (userAuthenticationAccount != null ? "FOUND" : "NOT FOUND"));
        }
        if (userAuthenticationAccount == null) {
            // Consistency check
            checkPrincipalExistsInDatabase(uid);
            return null;
        }
        ICommonUserAccount userAccount = createUserAccountFromAuthenticationAccount(userAuthenticationAccount);
        return userAccount;
    }

    @Override
    public IUserAccount getUserAccountFromMafUid(Long mafUid) throws AccountManagementException {
        Principal principal = Principal.getPrincipalFromId(mafUid);
        if (log.isDebugEnabled()) {
            log.debug("Looking for user account with maf id  " + mafUid + " " + (principal != null ? "FOUND" : "NOT FOUND"));
        }
        if (principal == null) {
            return null;
        }
        return getUserAccountFromUid(principal.uid);
    }

    @Override
    public IUserAccount getUserAccountFromEmail(String mail) throws AccountManagementException {
        IUserAuthenticationAccount userAuthenticationAccount = getAuthenticationAccountReaderPlugin().getAccountFromEmail(mail);
        if (log.isDebugEnabled()) {
            log.debug("Looking for user account with mail  " + mail + " " + (userAuthenticationAccount != null ? "FOUND" : "NOT FOUND"));
        }
        if (userAuthenticationAccount == null) {
            return null;
        }
        IUserAccount userAccount = createUserAccountFromAuthenticationAccount(userAuthenticationAccount);
        return userAccount;
    }

    @Override
    public List<IUserAccount> getUserAccountsFromName(String nameCriteria) throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Looking for user account with name pattern " + nameCriteria);
        }
        List<IUserAccount> userAccounts = new ArrayList<IUserAccount>();
        List<IUserAuthenticationAccount> authenticationAccounts = getAuthenticationAccountReaderPlugin().getAccountsFromName(nameCriteria);
        for (IUserAuthenticationAccount authenticationAccount : authenticationAccounts) {
            IUserAccount userAccount = createUserAccountFromAuthenticationAccount(authenticationAccount);
            if (userAccount != null && userAccount.isDisplayed()) {
                userAccounts.add(userAccount);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Found " + userAccounts.size() + " account(s)");
        }
        return userAccounts;
    }

    @Override
    public String getValidationKey(String uid, String validationData) throws AccountManagementException {
        Principal userPrincipal = findPrincipalAndCheckForAuthenticationBackEndConsistency(uid);
        if (log.isDebugEnabled()) {
            log.debug("Check the validation key " + validationData + " associated with the principal " + uid);
        }
        return userPrincipal.getValidationKey(validationData);
    }

    @Override
    public String checkValidationKey(String uid, String validationKey) throws AccountManagementException {
        Principal userPrincipal = findPrincipalAndCheckForAuthenticationBackEndConsistency(uid);
        return userPrincipal.checkValidationKey(validationKey, getValidationKeyValidity());
    }

    @Override
    public void resetValidationKey(String uid) throws AccountManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Request reset validation key for " + uid);
        }
        Principal userPrincipal = findPrincipalAndCheckForAuthenticationBackEndConsistency(uid);
        userPrincipal.resetValidationKey();
    }

    @Override
    public void invalidateUserAccountCache(String uid) {
        Cache.remove(IFrameworkConstants.USER_ACCOUNT_CACHE_PREFIX + uid);
        if (log.isDebugEnabled()) {
            log.debug("Cache invalidated for user " + uid);
        }
    }

    @Override
    public void invalidateAllUserAccountsCache() throws AccountManagementException {
        List<IUserAccount> userAccounts = getUserAccountsFromName("*");
        for (IUserAccount userAccount : userAccounts) {
            invalidateUserAccountCache(userAccount.getUid());
        }
    }

    /**
     * Add to the specified principal: - the roles passed as parameters (only if
     * the user is STANDARD) - the default list of roles associated with the
     * specified account type
     * 
     * @param principal
     *            a principal
     * @param accountType
     *            an account type
     * @param systemLevelRoleTypeNames
     *            a list of system level role type names
     */
    private void addingRolesToPrincipalForAccountType(Principal principal, AccountType accountType, List<String> systemLevelRoleTypeNames) {
        if (accountType.isRolesEditable()) {
            if (log.isDebugEnabled()) {
                log.debug("User account roles for account " + principal.uid + " are set to " + systemLevelRoleTypeNames);
            }
            if (systemLevelRoleTypeNames != null) {
                for (String systemLevelRoleTypeName : systemLevelRoleTypeNames) {
                    principal.addSystemLevelRole(getAllRoles().get(systemLevelRoleTypeName));
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("User account roles for account " + principal.uid + " are set to default");
        }
        List<SystemLevelRoleType> defaultSystemLevelRoleTypes = SystemLevelRoleType.getDefaultRolesForAccountType(accountType);
        if (defaultSystemLevelRoleTypes != null) {
            for (SystemLevelRoleType systemLevelRoleType : defaultSystemLevelRoleTypes) {
                principal.addSystemLevelRole(systemLevelRoleType);
            }
        }
    }

    /**
     * Post an event to the plugin manager
     * 
     * @param eventMessage
     */
    private void postToPlugginManagerService(EventMessage eventMessage) {
        if (eventBroadcastingService != null) {
            eventBroadcastingService.postOutMessage(eventMessage);
        }
    }

    /**
     * Creates a {@link DefaultUserAccount} from a
     * {@link IUserAuthenticationAccount}
     * 
     * @param userAuthenticationAccount
     * @return a DefaultUserAccount
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private ICommonUserAccount createUserAccountFromAuthenticationAccount(IUserAuthenticationAccount userAuthenticationAccount) {
        // Get user Account from cache if available
        ICommonUserAccount cachedUserAccount = (ICommonUserAccount) Cache
                .get(IFrameworkConstants.USER_ACCOUNT_CACHE_PREFIX + userAuthenticationAccount.getUid());
        if (log.isDebugEnabled()) {
            log.debug("Look for user account associated with uid " + userAuthenticationAccount.getUid() + " "
                    + (cachedUserAccount != null ? "FOUND" : "NOT FOUND)"));
        }
        if (cachedUserAccount != null) {
            return cachedUserAccount;
        }

        Principal userPrincipal = findPrincipalFromUid(userAuthenticationAccount.getUid());
        if (userPrincipal == null) {
            return null;
        }
        ICommonUserAccount userAccount;
        try {
            userAccount = (ICommonUserAccount) getCommonUserAccountClass().newInstance();
        } catch (Exception e) {
            log.error("Error while creating the user account class, this error should not happen", e);
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("Fill user account " + userAuthenticationAccount.getUid() + " with principal data and set activity status as "
                    + (userPrincipal.isActive && userAccount.isActive()));
        }
        userAccount.fill(userAuthenticationAccount, userPrincipal.id, userPrincipal.getAccountTypeAsObject(), userPrincipal.preferredLanguage,
                userPrincipal.deleted, userPrincipal.isDisplayed);
        userAccount.setActive(userPrincipal.isActive && userAccount.isActive());

        if (userPrincipal.systemLevelRoles != null) {
            Set<String> rolesAsString = new HashSet<String>();
            Set<String> selectableRolesAsString = new HashSet<String>();
            for (SystemLevelRole role : userPrincipal.systemLevelRoles) {
                if (role.systemLevelRoleType != null && !role.systemLevelRoleType.deleted && role.systemLevelRoleType.systemPermissions != null) {
                    userAccount.addSystemLevelRoleType(role.systemLevelRoleType.getName());
                    for (SystemPermission systemPermission : role.systemLevelRoleType.systemPermissions) {
                        rolesAsString.add(systemPermission.name);
                        if (systemPermission.isSelectable()) {
                            selectableRolesAsString.add(systemPermission.name);
                        }
                    }
                }
            }
            for (String roleAsString : rolesAsString) {
                userAccount.addGroup(new DefaultUserAccount.DefaultRole(roleAsString));
            }

            for (String roleAsString : selectableRolesAsString) {
                userAccount.addSelectableGroup(new DefaultUserAccount.DefaultRole(roleAsString));
            }
        }

        if (userAccount != null) {
            if (log.isDebugEnabled()) {
                log.debug("Set user account " + userAuthenticationAccount.getUid() + " in cache");
            }
            Cache.set(IFrameworkConstants.USER_ACCOUNT_CACHE_PREFIX + userAuthenticationAccount.getUid(), userAccount, getUserAccountCacheDurationInSeconds());
        }

        return userAccount;
    }

    /**
     * Find the principal associated with the specified uid.<br/>
     * This method:
     * <ul>
     * <li>Throw an exception if the principal cannot be found</li>
     * <li>Throw an exception if the principal is found in the db but not in the
     * authentication back-end</li>
     * </ul>
     * 
     * @param uid
     * @return
     * @throws AccountManagementException
     */
    private Principal findPrincipalAndCheckForAuthenticationBackEndConsistency(String uid) throws AccountManagementException {
        Principal userPrincipal = findPrincipalFromUid(uid);
        if (userPrincipal == null) {
            throw new AccountManagementException("Unknown user account " + uid);
        }
        if (!getAuthenticationAccountReaderPlugin().isUidAlreadyExist(uid)) {
            throwInconsistencyException(uid);
        }
        return userPrincipal;
    }

    /**
     * Find the principal associated with the specified uid.
     * 
     * @param uid
     *            the user unique Id
     * @return a Principal object or null if the user is not found
     */
    private Principal findPrincipalFromUid(String uid) {
        if (log.isDebugEnabled()) {
            log.debug("Look for principal associated with uid : " + uid);
        }
        Principal userPrincipal = Principal.getPrincipalFromUid(uid);
        return userPrincipal;
    }

    /**
     * This method implements a consistency check.<br/>
     * If the user exists in the database it throws an exception.
     * 
     * @param uid
     *            the unique user Id
     * @throws AccountManagementException
     */
    private void checkPrincipalExistsInDatabase(String uid) throws AccountManagementException {
        // Check if the uid exists in the db (inconsistency)
        if (Principal.isPrincipalExistsForUid(uid)) {
            throwInconsistencyException(uid);
        }
    }

    /**
     * Throw an exception (and log an error) notifying of an inconsistency
     * between the database and the LDAP
     * 
     * @param uid
     *            a unique user Id
     * @throws AccountManagementException
     */
    private void throwInconsistencyException(String uid) throws AccountManagementException {
        // Consistency check (throw an exception)
        String message = String.format("Inconsistency found, the uid=%s exists in db and an attempt to create a new"
                + " user failed or it does not exists in the authentication back-end", uid);
        log.error(message);
        throw new AccountManagementException(message);
    }

    /**
     * Return all the possible roles.<br/>
     * This consists in a Map of association:
     * <ul>
     * <li>Key : a group name</li>
     * <li>Value : a {@link SystemLevelRoleType} instance</li>
     * </ul>
     * 
     * @return a Map
     */
    private Map<String, SystemLevelRoleType> getAllRoles() {
        HashMap<String, SystemLevelRoleType> mapOfRoles = new HashMap<String, SystemLevelRoleType>();
        for (SystemLevelRoleType roleType : SystemLevelRoleType.getAllActiveRoles()) {
            mapOfRoles.put(roleType.getName(), roleType);
        }
        return mapOfRoles;
    }

    private IAuthenticationAccountWriterPlugin getAuthenticationAccountWriterPlugin() {
        return authenticationAccountWriterPlugin;
    }

    private IAuthenticationAccountReaderPlugin getAuthenticationAccountReaderPlugin() {
        return authenticationAccountReaderPlugin;
    }

    private int getValidationKeyValidity() {
        return validationKeyValidity;
    }

    private int getUserAccountCacheDurationInSeconds() {
        return userAccountCacheDurationInSeconds;
    }

    private Class<?> getCommonUserAccountClass() {
        return commonUserAccountClass;
    }
}
