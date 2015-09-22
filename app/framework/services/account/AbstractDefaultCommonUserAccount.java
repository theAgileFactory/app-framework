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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import be.objectify.deadbolt.core.models.Permission;
import be.objectify.deadbolt.core.models.Role;
import framework.commons.IFrameworkConstants;
import models.framework_models.account.Credential;
import models.framework_models.account.SystemLevelRoleType;
import models.framework_models.account.SystemPermission;

public abstract class AbstractDefaultCommonUserAccount implements ICommonUserAccount {
    protected Long mafUid;
    protected String uid;
    protected String firstName;
    protected String lastName;
    protected String mail;
    protected String preferredLanguage;
    protected AccountType accountType;
    protected boolean markedForDeletion;
    protected boolean isActive;
    protected Boolean isDisplayed;
    protected List<Role> roles = Collections.synchronizedList(new ArrayList<Role>());
    protected List<Role> selectableRoles = Collections.synchronizedList(new ArrayList<Role>());
    protected List<String> systemLevelRoleTypes = Collections.synchronizedList(new ArrayList<String>());
    protected List<Permission> permissions = Collections.synchronizedList(new ArrayList<Permission>());

    public AbstractDefaultCommonUserAccount() {
        addGroup(new DefaultRole(IFrameworkConstants.DEFAULT_PERMISSION_PRIVATE));
    }

    @Override
    public String getIdentifier() {
        return getUid();
    }

    @Override
    public String getUid() {
        return this.uid;
    }

    @Override
    public String getFirstName() {
        return this.firstName;
    }

    @Override
    public String getLastName() {
        return this.lastName;
    }

    @Override
    public String getMail() {
        return this.mail;
    }

    @Override
    public boolean isActive() {
        return this.isActive;
    }

    @Override
    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    @Override
    public Boolean isDisplayed() {
        return isDisplayed;
    }

    @Override
    public boolean isMarkedForDeletion() throws AccountManagementException {
        return this.markedForDeletion;
    }

    @Override
    public Long getMafUid() throws AccountManagementException {
        return this.mafUid;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    @Override
    public List<? extends Permission> getPermissions() {
        return this.permissions;
    }

    /**
     * This returns a list of {@link Role} which names are actually based on
     * {@link SystemPermission} names.
     */
    @Override
    public List<? extends Role> getRoles() {
        return this.roles;
    }

    /**
     * This returns a list of {@link Role} which names are actually based on
     * {@link SystemPermission} names.
     */
    @Override
    public List<? extends Role> getSelectableRoles() {
        List<? extends Role> sorterRoles = Collections.synchronizedList(new ArrayList<>(this.selectableRoles));
        Collections.sort(sorterRoles, new Comparator<Role>() {
            @Override
            public int compare(Role role1, Role role2) {
                return role1.getName().compareTo(role2.getName());
            }
        });
        return sorterRoles;
    }

    /**
     * This returns a list of String which are {@link SystemLevelRoleType}
     * names.
     */
    @Override
    public List<String> getSystemLevelRoleTypeNames() {
        return this.systemLevelRoleTypes;
    }

    @Override
    public List<String> getSystemPermissionNames() {
        ArrayList<String> systemPermissions = new ArrayList<String>();
        for (Role deadBoltRole : getRoles()) {
            systemPermissions.add(deadBoltRole.getName());
        }
        return systemPermissions;
    }

    /**
     * Get the user profile attributes from a {@link Credential} object
     */
    void fill(Credential credential) {
        this.firstName = credential.firstName;
        this.lastName = credential.lastName;
        this.mail = credential.mail;
        this.uid = credential.uid;
        this.isActive = credential.isActive;
    }

    @Override
    public void fill(IUserAuthenticationAccount userAuthenticationAccount, Long mafUid, AccountType accountType, String preferredLanguage,
            boolean markedForDeletion, Boolean isDisplayed) {
        this.accountType = accountType;
        this.firstName = userAuthenticationAccount.getFirstName();
        this.lastName = userAuthenticationAccount.getLastName();
        this.mail = userAuthenticationAccount.getMail();
        this.uid = userAuthenticationAccount.getUid();
        this.isActive = userAuthenticationAccount.isActive();
        this.mafUid = mafUid;
        this.preferredLanguage = preferredLanguage;
        this.markedForDeletion = markedForDeletion;
        this.isDisplayed = isDisplayed;
    }

    @Override
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public void addGroup(Role role) {
        if (role != null) {
            this.roles.add(role);
        }
    }

    @Override
    public void addSelectableGroup(Role role) {
        if (role != null) {
            this.selectableRoles.add(role);
        }
    }

    @Override
    public void addSystemLevelRoleType(String systemLevelRoleTypeName) {
        if (systemLevelRoleTypeName != null) {
            this.systemLevelRoleTypes.add(systemLevelRoleTypeName);
        }
    }

    @Override
    public String toString() {
        return getClass() + " [uid=" + uid + ", firstName=" + firstName + ", lastName=" + lastName + ", mail=" + mail + ", roles=" + roles + ", permissions="
                + permissions + "]";
    }

    /**
     * The data structure which holds the role definition
     * 
     * @author Pierre-Yves Cloux
     * 
     */
    public static class DefaultRole implements Role {
        private String name;

        public DefaultRole() {
        }

        public DefaultRole(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "DefaultRole [name=" + name + "]";
        }

    }

    void setUid(String uid) {
        this.uid = uid;
    }

    void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    void setLastName(String lastName) {
        this.lastName = lastName;
    }

    void setMail(String mail) {
        this.mail = mail;
    }
}
