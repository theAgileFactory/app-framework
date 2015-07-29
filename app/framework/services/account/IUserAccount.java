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
import java.util.List;

import models.framework_models.account.SystemLevelRoleType;
import models.framework_models.account.SystemPermission;
import be.objectify.deadbolt.core.models.Role;
import be.objectify.deadbolt.core.models.Subject;
import framework.utils.DefaultSelectableValueHolder;
import framework.utils.DefaultSelectableValueHolderCollection;
import framework.utils.ISelectableValueHolderCollection;

/**
 * The interface to be implemented by the object holding the user profile,
 * namely :<br/>
 * <ul>
 * <li>The user attributes : firstName, lastName, mail</li>
 * <li>The user groups</li>
 * <li>The user permissions</li>
 * </ul>
 * 
 * <p>
 * <b>WARNING</b><br/>
 * The deadbolt {@link Role} are not mapped to {@link SystemLevelRoleType}.
 * Actually they are mapped to {@link SystemPermission} which rather represents
 * the permissions that a user is allocated.<br/>
 * We have thus added a "getSystemRoles" property which returns the
 * {@link SystemLevelRoleType} to which a user is associated.
 * </p>
 * 
 * WARNING: each user is associated with the
 * SystemLevelRoleType.DEFAULT_GROUP_NAME system role. Any implementation of
 * this interface <b>must</b> take care of it.
 * 
 * @author Pierre-Yves Cloux
 */
public interface IUserAccount extends Subject, IUserAuthenticationAccount {
    /**
     * The different types of users.<br/>
     * Here are the attributes for this object:
     * <ul>
     * <li>label : a label describing the type of account</li>
     * <li>rolesEditable : true if the roles for this user account can be edited
     * </li>
     * <li>contractUser : true if the user must be taken into account for
     * subscription</li>
     * </ul>
     * 
     * @author Pierre-Yves Cloux
     */
    public enum AccountType {
        STANDARD("STANDARD", true, true), VIEWER("VIEWER", false, false);

        private String label;
        private boolean rolesEditable;
        private boolean contractUser;

        private AccountType(String label, boolean rolesEditable, boolean contractUser) {
            this.label = label;
            this.rolesEditable = rolesEditable;
            this.contractUser = contractUser;
        }

        public boolean isRolesEditable() {
            return rolesEditable;
        }

        public boolean isContractUser() {
            return contractUser;
        }

        public String getLabel() {
            return label;
        }

        /**
         * Get the AccountTypes for which contractUser is true.
         */
        public static List<String> getIsContractUserAsString() {
            List<String> r = new ArrayList<>();
            for (AccountType accountType : AccountType.values()) {
                if (accountType.isContractUser()) {
                    r.add(accountType.name());
                }
            }
            return r;
        }

        public static ISelectableValueHolderCollection<String> getValueHolder() {
            DefaultSelectableValueHolderCollection<String> valueHolders = new DefaultSelectableValueHolderCollection<String>();
            for (AccountType accountType : AccountType.values()) {
                valueHolders.add(new DefaultSelectableValueHolder<String>(accountType.name(), accountType.getLabel()));
            }
            return valueHolders;
        }
    }

    /**
     * Return the type for this account
     * 
     * @return
     */
    public AccountType getAccountType();

    /**
     * return the preferred language (ISO - 2 letters)
     * 
     * @return String
     */
    public String getPreferredLanguage();

    /**
     * return true if the account should be displayed in BizDock
     */
    public Boolean isDisplayed();

    /**
     * Return true if the user account is marked for deletion
     * 
     * @return a boolean
     */
    public boolean isMarkedForDeletion() throws AccountManagementException;

    /**
     * Return the unique identifier of the account in MAF
     * 
     * @return a unique identifier
     */
    public Long getMafUid() throws AccountManagementException;

    /**
     * Return the list of system level role types (see
     * {@link SystemLevelRoleType}) to which the specified user is associated
     */
    public List<String> getSystemLevelRoleTypeNames();

    /**
     * Return the list of system system permissions (see
     * {@link SystemPermission}) names to which the specified user is associated
     */
    public List<String> getSystemPermissionNames();

    /**
     * Get the selectable roles.
     */
    public List<? extends Role> getSelectableRoles();
}
