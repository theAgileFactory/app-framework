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
package models.framework_models.account;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;

import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.Model;

import framework.commons.IFrameworkConstants;
import framework.services.account.AccountManagementException;
import framework.services.account.IUserAccount;
import framework.services.account.IUserAccount.AccountType;
import framework.utils.Utilities;
import framework.utils.formats.DateType;

/**
 * This entity represents an user of the system.<br/>
 * The principal object is not deleted "in real-time". It should be deleted
 * asynchronously.
 * 
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class Principal extends Model implements IModel {
    private static final long serialVersionUID = -2917269166923312893L;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, Principal> find = new Finder<Long, Principal>(Principal.class);

    /**
     * Return the number of active principals
     * 
     * @return an integer
     */
    public static int getActivePrincipalCount() {
        return Principal.find.where().eq("deleted", false).eq("isActive", true).findRowCount();
    }

    /**
     * Return the number of non deleted principals
     * 
     * @return an integer
     */
    public static int getRegisteredPrincipalCount() {
        return Principal.find.where().eq("deleted", false).findRowCount();
    }

    /**
     * Return the number of consumed users (in licenses point of view).
     * 
     * Meaning: the non deleted, displayed and with an account type that is
     * enabled for "user contract".
     */
    public static int getConsumedUsers() {
        return find.where().eq("deleted", false).eq("isDisplayed", true).in("accountType", AccountType.getIsContractUserAsString()).findRowCount();
    }

    /**
     * Return true if a Principal exists for the specified uid. Only return
     * principals not marked for deletion.
     * 
     * @param uid
     *            the unique identified of the Principal
     * @return a boolean
     */
    public static boolean isPrincipalExistsForUid(String uid) {
        return Principal.find.where().eq("uid", uid).findRowCount() != 0;
    }

    /**
     * Return the principal associated with the specified uid. Only return
     * principals not marked for deletion.
     * 
     * @param uid
     *            the unique identified of the Principal
     * @return a Principal
     */
    public static Principal getPrincipalFromUid(String uid) {
        return Principal.find.where().eq("uid", uid).findUnique();
    }

    /**
     * Return the principal associated with the specified id
     * 
     * @param id
     *            the primary key of the principal object
     * @return a Principal
     */
    public static Principal getPrincipalFromId(Long id) {
        return Principal.find.byId(id);
    }

    /**
     * Return the list of Principal with the specified permission.
     * 
     * @param permissionName
     *            the name of a permission
     * @return a list of Principal
     */
    public static List<Principal> getPrincipalsWithPermission(String permissionName) {
        return Principal.find.where().eq("systemLevelRoles.systemLevelRoleType.deleted", false)
                .eq("systemLevelRoles.systemLevelRoleType.systemPermissions.name", permissionName).findList();
    }

    /**
     * Return the list of Principal with the specified system level role.
     * 
     * @param roleName
     *            the role name
     * @return a list of Principal
     */
    public static List<Principal> getPrincipalsWithSystemLevelRoleName(String roleName) {
        return Principal.find.where().eq("systemLevelRoles.systemLevelRoleType.deleted", false).eq("systemLevelRoles.systemLevelRoleType.name", roleName)
                .findList();
    }

    /**
     * Return the list of Principal with the specified system level role.
     * 
     * @param roleId
     *            the unique id of a role
     * @return a list of Principal
     */
    public static List<Principal> getPrincipalsWithSystemLevelRoleId(Long roleId) {
        return Principal.find.where().eq("systemLevelRoles.systemLevelRoleType.deleted", false).eq("systemLevelRoles.systemLevelRoleType.id", roleId)
                .findList();
    }

    /**
     * Return the list of uid matching the list of Principals
     * 
     * @param listOfPrincipals
     *            a list of Principals
     * @return a list of uid (of the specified principals)
     */
    public static List<String> getListOfUidFromListOfPrincipal(List<Principal> listOfPrincipals) {
        List<String> listOfUids = new ArrayList<String>();
        if (listOfPrincipals != null) {
            for (Principal principal : listOfPrincipals) {
                listOfUids.add(principal.uid);
            }
        }
        return listOfUids;
    }

    @Id
    public Long id;

    @Column(length = IModelConstants.MEDIUM_STRING)
    public String accountType;

    @Column(length = IModelConstants.LARGE_STRING)
    public String uid;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "principal")
    public List<SystemLevelRole> systemLevelRoles;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "principal")
    public List<Notification> notifications;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "senderPrincipal")
    public List<Notification> sentNotifications;

    public Boolean isActive;

    public Boolean isDisplayed;

    public boolean deleted;

    @Column(length = IModelConstants.LARGE_STRING)
    public String validationKey;

    @Column(length = IModelConstants.LARGE_STRING)
    public String validationData;

    @DateType
    public Date validationKeyCreationDate;

    /**
     * The preferred language ISO639-1 code (2 letters)
     */
    @Column(length = IModelConstants.LANGUAGE_CODE)
    public String preferredLanguage;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "principal")
    public List<Shortcut> shortcuts;

    /**
     * Return the account type for the specified principal.<br/>
     * WARNING: if the type cannot be found or is null the user is assumed to be
     * STANDARD.<br/>
     * See {@link IUserAccount}
     * 
     * @return
     */
    public AccountType getAccountTypeAsObject() {
        if (accountType == null)
            return AccountType.STANDARD;
        try {
            return AccountType.valueOf(accountType);
        } catch (Exception e) {
            return AccountType.STANDARD;
        }
    }

    /**
     * Check if the specified principal has the specified permissions.<br/>
     * 
     * @param uid
     *            the unique id of a user
     * @param permissionNames
     *            a list of {@link SystemPermission} name
     * @return a boolean
     */
    public static boolean hasPermissions(String uid, String[] permissionNames) {
        List<SystemPermission> systemPermissions = getSystemPermissionsList(uid);
        List<String> permissionsList = Arrays.asList(permissionNames);
        // Just in case the default permission is tested
        permissionsList.remove(IFrameworkConstants.DEFAULT_PERMISSION_PRIVATE);
        int count = 0;
        for (SystemPermission systemPermission : systemPermissions) {
            if (permissionsList.contains(systemPermission.name)) {
                count++;
            }
        }
        return (count == permissionsList.size());
    }

    /**
     * Check if the current use has the following permissions
     * 
     * @return true if yes
     */
    public boolean hasPermissions(String[] permissionNames) {
        return Principal.hasPermissions(uid, permissionNames);
    }

    /**
     * Returns the list of permissions for the specified principal
     * 
     * @return a set of String
     */
    public Set<String> getPermissionNames() {
        Set<String> permissionNames = new HashSet<String>();
        for (SystemPermission systemPermission : getSystemPermissionsList(this.uid)) {
            permissionNames.add(systemPermission.name);
        }
        permissionNames.add(IFrameworkConstants.DEFAULT_PERMISSION_PRIVATE);
        return permissionNames;
    }

    /**
     * Performs a select distinct (only with name) retrieving the
     * {@link SystemPermission}
     * 
     * @param uid
     *            the uid of a user
     * @return a list of permissions (only with "name" attribute)
     */
    private static List<SystemPermission> getSystemPermissionsList(String uid) {
        List<SystemPermission> systemPermissions = SystemPermission.find.select("name").setDistinct(true).where().eq("deleted", false)
                .eq("systemLevelRoleTypes.deleted", false).eq("systemLevelRoleTypes.systemLevelRoles.principal.uid", uid).findList();
        return systemPermissions;
    }

    /**
     * Send a notification to a principal.
     * 
     * @param category
     *            the notification category
     * @param title
     *            the notification title
     * @param message
     *            the notification content
     * @param actionLink
     *            the action link
     */
    public void sendNotification(NotificationCategory category, String title, String message, String actionLink) {
        Notification notification = new Notification();
        notification.isMessage = false;
        notification.notificationCategory = category;
        notification.title = title;
        notification.message = message;
        notification.actionLink = actionLink;
        notification.principal = this;
        notification.senderPrincipal = null;
        notification.isRead = false;
        notification.save();
    }

    /**
     * Send a message to a principal.
     * 
     * @param senderUid
     *            the uid of the sender
     * @param title
     *            the notification title
     * @param message
     *            the notification content
     */
    public void sendMessage(String senderUid, String title, String message) {
        Notification notification = new Notification();
        notification.isMessage = true;
        notification.notificationCategory = null;
        notification.title = title;
        notification.message = message;
        notification.actionLink = null;
        notification.principal = this;
        notification.senderPrincipal = Principal.getPrincipalFromUid(senderUid);
        notification.isRead = false;
        notification.save();
    }

    /**
     * Return true if the specified principal has some notifications
     * 
     * @return a boolean
     */
    public boolean hasNotifications() {
        int notificationsCount = Notification.find.where().eq("deleted", false).eq("isMessage", false).eq("principal.id", this.id).findRowCount();
        return notificationsCount != 0;
    }

    /**
     * Return the number of not read notifications
     */
    public int nbNotReadNotifications() {
        return Notification.find.where().eq("deleted", false).eq("isMessage", false).eq("isRead", false).eq("principal.id", this.id).findRowCount();
    }

    /**
     * Return true if the specified principal has some messages
     * 
     * @return a boolean
     */
    public boolean hasMessages() {
        int notificationsCount = Notification.find.where().eq("deleted", false).eq("isMessage", true).eq("principal.id", this.id).findRowCount();
        return notificationsCount != 0;
    }

    /**
     * Return the number of not read messages
     * 
     * @return a boolean
     */
    public int nbNotReadMessages() {
        return Notification.find.where().eq("deleted", false).eq("isMessage", true).eq("isRead", false).eq("principal.id", this.id).findRowCount();
    }

    /**
     * Return the "not deleted" notifications associated with this user.<br/>
     * Sort these notifications from the most recent to the oldest.
     * 
     * @return list of {@link Notification}
     */
    public List<Notification> getNotifications() {
        return Notification.find.where().eq("deleted", false).eq("isMessage", false).eq("principal.id", this.id).orderBy("isRead, creationDate desc")
                .findList();
    }

    /**
     * Get the list of notifications as an expression list.
     */
    public ExpressionList<Notification> getNotificationsAsExpr() {
        return Notification.find.where().eq("deleted", false).eq("isMessage", false).eq("principal.id", this.id);
    }

    /**
     * Return the "not deleted" and not read notifications associated with this
     * user.<br/>
     * Sort these notifications from the most recent to the oldest.<br/>
     * Returns at max 5 entries.
     * 
     * @return list of {@link Notification}
     */
    public List<Notification> getNotReadNotifications() {
        return Notification.find.where().eq("deleted", false).eq("isMessage", false).eq("isRead", false).eq("principal.id", this.id)
                .orderBy("creationDate desc").setMaxRows(5).findList();
    }

    /**
     * Return the "not deleted" messages associated with this user.<br/>
     * Sort these messages from the most recent to the oldest.
     * 
     * @return list of {@link Notification}
     */
    public List<Notification> getMessages() {
        return Notification.find.where().eq("deleted", false).eq("isMessage", true).eq("principal.id", this.id).orderBy("isRead, creationDate desc").findList();
    }

    /**
     * Return the "not deleted" and not read messages associated with this user.<br/>
     * Sort these messages from the most recent to the oldest.<br/>
     * Returns at max 5 entries.
     * 
     * @return list of {@link Notification}
     */
    public List<Notification> getNotReadMessages() {
        return Notification.find.where().eq("deleted", false).eq("isMessage", true).eq("isRead", false).eq("principal.id", this.id)
                .orderBy("creationDate desc").setMaxRows(5).findList();
    }

    /**
     * Delete the notification for the current principal associated with the
     * specified id.<br/>
     * Return false if the notification do not belong to the principal
     * 
     * @return a boolean
     */
    public boolean deleteNotification(Long notificationId) {
        Notification notification = Notification.find.where().eq("deleted", false).eq("principal.id", this.id).eq("id", notificationId).findUnique();
        if (notification == null) {
            return false;
        }
        notification.doDelete();
        return true;
    }

    /**
     * Generate a new validation key stored in the user profile.<br/>
     * This key can be subsequently checked before performing some actions such
     * as mail update or password change.<br/>
     * The provided "validationData" contains a string which is to be used once
     * the validation completes (such as an email address or a ciphered
     * password).
     * 
     * @param validationData
     *            the data stored to a later use and associated with this
     *            validation key
     * @return an uuid
     */
    public String getValidationKey(String validationData) {
        String uuid = Utilities.getRandomID();
        this.validationKey = uuid;
        this.validationData = validationData;
        this.validationKeyCreationDate = new Date();
        this.update();
        return uuid;
    }

    /**
     * Check if the validation key is valid for the specified user and return
     * the associated validation data.
     * 
     * @param validationKey
     *            an uuid
     * @param validityDelay
     *            how much minutes is allowed since the validation key was
     *            created
     * @return the validation data associated with this validation key if it is
     *         valid
     * @throws AccountManagementException
     */
    public String checkValidationKey(String validationKey, int validityDelay) throws AccountManagementException {
        if (this.validationKey != null && this.validationKey.equals(validationKey) && this.validationKeyCreationDate != null) {
            String validationData = this.validationData;

            // Check the validity
            long delay = System.currentTimeMillis() - this.validationKeyCreationDate.getTime();
            if (validityDelay * 60 * 1000 < delay) {
                resetValidationKey();
                return null;
            }

            return validationData;
        }
        return null;
    }

    /**
     * Check if the validation key is valid for the specified user and return
     * the associated validation data.
     * 
     * @param validationKey
     *            an uuid
     * @param validityDelay
     *            how much minutes is allowed since the validation key was
     *            created
     * @return the validation data associated with this validation key if it is
     *         valid
     * @throws AccountManagementException
     */
    public void resetValidationKey() throws AccountManagementException {
        this.validationKeyCreationDate = null;
        this.validationKey = null;
        this.validationData = null;
        this.save();
    }

    /**
     * Add a new {@link SystemLevelRole} to the user if no role with the
     * specified {@link SystemLevelRoleType} is added to the list
     * 
     * @param systemLevelRoleType
     *            a type of role
     */
    public void addSystemLevelRole(SystemLevelRoleType systemLevelRoleType) {
        if (!checkSystemLevelRoleExists(systemLevelRoleType)) {
            SystemLevelRole systemLevelRole = new SystemLevelRole();
            systemLevelRole.systemLevelRoleType = systemLevelRoleType;
            systemLevelRole.principal = this;
            systemLevelRole.isEnabled = true;
            systemLevelRole.save();
        }
    }

    /**
     * Remove a role from a user.
     * 
     * @param systemLevelRoleType
     *            a type of role
     */
    public void removeSystemLevelRole(SystemLevelRoleType systemLevelRoleType) {
        if (this.systemLevelRoles != null) {
            for (SystemLevelRole systemLevelRole : this.systemLevelRoles) {
                if (systemLevelRole.systemLevelRoleType != null && !systemLevelRole.systemLevelRoleType.deleted
                        && systemLevelRole.systemLevelRoleType.name.equals(systemLevelRoleType.name)) {
                    systemLevelRole.delete();
                }
            }
        }
    }

    /**
     * Remove all the roles from the user
     */
    public void removeAllSystemLevelRoles() {
        if (this.systemLevelRoles != null) {
            for (SystemLevelRole systemLevelRole : this.systemLevelRoles) {
                systemLevelRole.delete();
            }
        }
    }

    /**
     * Return true if a {@link SystemLevelRole} with the same
     * {@link SystemLevelRoleType} is already assigned to this {@link Principal}
     * 
     * @param systemLevelRoleType
     *            a role type
     * @return true if the role is already assigned
     */
    private boolean checkSystemLevelRoleExists(SystemLevelRoleType systemLevelRoleType) {
        if (this.systemLevelRoles == null)
            return false;
        for (SystemLevelRole systemLevelRole : this.systemLevelRoles) {
            if (systemLevelRole.systemLevelRoleType != null && !systemLevelRole.systemLevelRoleType.deleted
                    && systemLevelRole.systemLevelRoleType.name.equals(systemLevelRoleType.name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a default person.<br/>
     * Better to proceed like this since "default" values have some side effects
     * with forms (especially booleans and checkboxes)
     */
    public static Principal newPrincipalWithDefaulValues() {
        Principal person = new Principal();
        person.defaults();
        return person;
    }

    @Override
    public String toString() {
        return "Principal [id=" + id + ", uid=" + uid + ", isActive=" + isActive + ", validationKey=" + validationKey + "]";
    }

    @Override
    public String audit() {
        return toString();
    }

    @Override
    public void defaults() {
        isActive = true;
    }

    @Override
    public void doDelete() {
        isActive = false;
        deleted = true;
        save();
    }
}
