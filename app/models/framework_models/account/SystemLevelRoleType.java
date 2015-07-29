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

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;

import org.apache.poi.ss.formula.functions.T;

import play.db.ebean.Model;
import framework.services.account.IUserAccount;
import framework.services.account.IUserAccount.AccountType;
import framework.utils.DefaultSelectableValueHolder;
import framework.utils.DefaultSelectableValueHolderCollection;
import framework.utils.ISelectableValueHolder;
import framework.utils.Msg;

/**
 * A system level role type defines a specified role to be allocated to a
 * {@link Principal}.<br/>
 * A role is associated with one or more {@link SystemPermission}.
 * 
 * The accountTypeDefaults property contains a comma separated list of
 * {@link IUserAccount.AccountType} names. This is used to associate (at
 * creation time) a default set of roles to a defined user account.
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class SystemLevelRoleType extends Model implements IModel, ISelectableValueHolder<Long> {
    private static final long serialVersionUID = -9129168665390850805L;

    public boolean deleted = false;
    public boolean selectable;

    @Version
    public Timestamp lastUpdate;

    @Id
    public Long id;

    @Column(length = IModelConstants.SMALL_STRING)
    public String name;

    @Column(length = IModelConstants.LARGE_STRING)
    public String description;

    @Column(length = IModelConstants.VLARGE_STRING)
    public String accountTypeDefaults;

    @OneToMany(cascade = CascadeType.ALL)
    public List<SystemLevelRole> systemLevelRoles;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "system_level_role_type_has_system_permission")
    public List<SystemPermission> systemPermissions;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, SystemLevelRoleType> find = new Finder<Long, SystemLevelRoleType>(Long.class, SystemLevelRoleType.class);

    /**
     * Retrieve all the roles (selectable or not) which are associated (by
     * default) to the specified account type.<br/>
     * 
     * @param accountType
     *            a type of {@link IUserAccount}.
     */
    public static List<SystemLevelRoleType> getDefaultRolesForAccountType(AccountType accountType) {
        return find.where().eq("deleted", false).contains("accountTypeDefaults", accountType.name()).findList();
    }

    /**
     * Retrieve all the selectable roles
     */
    public static List<SystemLevelRoleType> getAllActiveRoles() {
        return find.where().eq("deleted", false).eq("selectable", true).findList();
    }

    /**
     * Retrieve the selectable role associated with the specified name
     */
    public static SystemLevelRoleType getActiveRoleFromName(String name) {
        return find.where().eq("deleted", false).eq("selectable", true).eq("name", name).findUnique();
    }

    /**
     * Retrieve the selectable role associated with the specified id
     */
    public static SystemLevelRoleType getActiveRoleFromId(Long id) {
        return find.where().eq("deleted", false).eq("selectable", true).eq("id", id).findUnique();
    }

    /**
     * Return true if the specified role contains the specified permission
     * 
     * @param id
     *            a role unique id
     * @param permissionName
     *            a permission name
     * @return a boolean
     */
    public static boolean hasPermission(Long id, String permissionName) {
        return SystemLevelRoleType.find.where().eq("deleted", false).eq("selectable", true).eq("id", id).eq("systemPermissions.name", permissionName)
                .findRowCount() != 0;
    }

    public static DefaultSelectableValueHolderCollection<String> getAllActiveRolesAsValueHolderCollection() {
        DefaultSelectableValueHolderCollection<String> roles = new DefaultSelectableValueHolderCollection<String>();
        List<SystemLevelRoleType> list = getAllActiveRoles();
        for (SystemLevelRoleType role : list) {
            roles.add(new DefaultSelectableValueHolder<String>(role.name, role.name, Msg.get(role.description)));

        }
        return roles;
    }

    public SystemLevelRoleType() {

    }

    /**
     * Return the real text corresponding to the resource stored in the name
     * attribute of the preference
     * 
     * @return
     */
    public String getDescriptionAsMessage() {
        return Msg.get(description);
    }

    @Override
    public String audit() {
        return null;
    }

    @Override
    public void defaults() {

    }

    @Override
    public void doDelete() {
        deleted = true;
        save();
    }

    @Override
    public String getDescription() {
        return Msg.get(this.description);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public void setUrl(String url) {
    }

    @Override
    public Long getValue() {
        return this.id;
    }

    @Override
    public boolean isSelectable() {
        return selectable;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public int compareTo(Object o) {
        @SuppressWarnings("unchecked")
        ISelectableValueHolder<T> v = (ISelectableValueHolder<T>) o;
        return this.getName().compareTo(v.getName());
    }

}
