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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Version;

import org.apache.poi.ss.formula.functions.T;

import com.avaje.ebean.Model;

import framework.utils.DefaultSelectableValueHolderCollection;
import framework.utils.ISelectableValueHolder;
import framework.utils.ISelectableValueHolderCollection;
import framework.utils.Msg;
import models.framework_models.parent.IModelConstants;

/**
 * A permission is a right given to a user to perform a certain action.<br/>
 * The permissions are defined by the system level role that a user is
 * associated with.
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class SystemPermission extends Model implements ISelectableValueHolder<Long> {
    private static final long serialVersionUID = -5596128653875507299L;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, SystemPermission> find = new Finder<Long, SystemPermission>(SystemPermission.class);

    /**
     * Return the system permission by name.
     * 
     * @param permissionName
     *            the name of a permission
     * @return a permission
     */
    public static SystemPermission getSystemPermissionByName(String permissionName) {
        return find.where().eq("deleted", false).eq("name", permissionName).findUnique();
    }

    /**
     * Return the system permission by id.
     * 
     * @param id
     *            the system permission id
     */
    public static SystemPermission getById(Long id) {
        return find.where().eq("deleted", false).eq("id", id).findUnique();
    }

    /**
     * Get all selectable system permissions as a value holder collection.
     */
    public static ISelectableValueHolderCollection<Long> getAllSelectableSystemPermissions() {
        return new DefaultSelectableValueHolderCollection<Long>(find.where().eq("deleted", false).eq("selectable", true).findList());
    }

    public boolean deleted = false;
    @Version
    public Timestamp lastUpdate;

    @Id
    public Long id;

    @Column(length = IModelConstants.LARGE_STRING)
    public String name;
    @Column(length = IModelConstants.VLARGE_STRING)
    public String description;

    public Boolean selectable;

    @ManyToMany(cascade = CascadeType.ALL, mappedBy = "systemPermissions")
    public List<SystemLevelRoleType> systemLevelRoleTypes;

    @ManyToMany(cascade = CascadeType.ALL, mappedBy = "systemPermissions")
    public List<Preference> preferences;

    public SystemPermission() {
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
    public String getName() {
        return this.name;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public String getDescription() {
        return this.getDescriptionAsMessage();
    }

    @Override
    public Long getValue() {
        return this.id;
    }

    @Override
    public boolean isSelectable() {
        return this.selectable;
    }

    @Override
    public boolean isDeleted() {
        return this.deleted;
    }

    @Override
    public void setUrl(String url) {
    }

    @Override
    public int compareTo(Object o) {
        @SuppressWarnings("unchecked")
        ISelectableValueHolder<T> v = (ISelectableValueHolder<T>) o;
        return this.getName().compareTo(v.getName());
    }

    /**
     * Check if the permissions stored in the database match the one defined in
     * the class (as a static final variable).<br/>
     * This is required in order to avoid inconsistencies between the code and
     * the database content.
     * 
     * @return a boolean
     */
    public static boolean checkPermissions(Class<?> permissionClass) {
        List<SystemPermission> systemPermissions = find.where().eq("deleted", false).findList();
        List<String> possibleValuesForPermission = new ArrayList<String>();
        for (Field field : permissionClass.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) && field.getName().endsWith("_PERMISSION")
                    && !field.getName().endsWith("_DYNAMIC_PERMISSION")) {
                possibleValuesForPermission.add(field.getName());
            }
        }
        if (systemPermissions != null) {
            for (SystemPermission systemPermission : systemPermissions) {
                try {
                    if (possibleValuesForPermission.contains(systemPermission.name)) {
                        possibleValuesForPermission.remove(systemPermission.name);
                    }
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        }
        return possibleValuesForPermission.size() == 0;
    }

}
