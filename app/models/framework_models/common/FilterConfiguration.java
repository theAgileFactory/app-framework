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
package models.framework_models.common;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.avaje.ebean.Model;

import framework.utils.ISelectableValueHolder;
import models.framework_models.account.Principal;
import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;

/**
 * A filter configuration is a stored filter for a user and a data type.
 * 
 * @author Johann Kohler
 */
@Entity
public class FilterConfiguration extends Model implements IModel, ISelectableValueHolder<Long> {

    public static Finder<Long, FilterConfiguration> find = new Finder<Long, FilterConfiguration>(FilterConfiguration.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @ManyToOne(cascade = CascadeType.ALL, optional = true)
    public Principal principal;

    @Column(length = IModelConstants.LARGE_STRING)
    public String dataType;

    @Column(length = IModelConstants.MEDIUM_STRING)
    public String name;

    public String configuration;

    public boolean isSelected;

    public boolean isDefault;

    @Column(length = 12)
    public String sharedKey;

    @Transient
    public boolean isShared = false;

    @Transient
    public boolean sharedNotExisting = false;

    @Transient
    public boolean isNotCompatible = false;

    /**
     * Default constructor.
     */
    public FilterConfiguration() {
        super();
    }

    @Override
    public String audit() {
        return "FilterConfiguration [id=" + id + ", name=" + name + ", isSelected=" + isSelected + ", isDefault=" + isDefault + "]";
    }

    @Override
    public void defaults() {
    }

    @Override
    public void doDelete() {
        this.deleted = true;
        save();
    }

    @Override
    public int compareTo(Object o) {
        @SuppressWarnings("unchecked")
        ISelectableValueHolder<Long> v = (ISelectableValueHolder<Long>) o;
        return this.getName().compareTo(v.getName());
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public Long getValue() {
        return this.id;
    }

    @Override
    public boolean isDeleted() {
        return this.deleted;
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public void setUrl(String url) {
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Get a filter configuration by id.
     * 
     * @param id
     *            the filter configuration id
     */
    public static FilterConfiguration getById(Long id) {
        return find.where().eq("deleted", false).eq("id", id).findUnique();
    }

    /**
     * Get the selected filter configuration for a user and a data type. If
     * doesn't exist, then return the default filter.
     * 
     * @param principalUid
     *            the principal username
     * @param dataType
     *            the data type
     */
    public static FilterConfiguration getSelectedFilterConfiguration(String principalUid, String dataType) {
        FilterConfiguration f = find.where().eq("deleted", false).eq("principal.uid", principalUid).eq("dataType", dataType).eq("isSelected", true)
                .findUnique();
        if (f == null) {
            return getDefaultFilterConfiguration(principalUid, dataType);
        } else {
            return f;
        }
    }

    /**
     * Get the default filter configuration for a user and a data type.
     * 
     * @param principalUid
     *            the principal username
     * @param dataType
     *            the data type
     */
    public static FilterConfiguration getDefaultFilterConfiguration(String principalUid, String dataType) {
        return find.where().eq("deleted", false).eq("principal.uid", principalUid).eq("dataType", dataType).eq("isDefault", true).findUnique();
    }

    /**
     * Get a filter configuration for a shared key.
     * 
     * @param sharedKey
     *            the share key
     * @param dataType
     *            the data type
     */
    public static FilterConfiguration getFilterConfigurationBySharedKey(String sharedKey, String dataType) {
        FilterConfiguration filter = find.where().eq("deleted", false).eq("sharedKey", sharedKey).eq("dataType", dataType).findUnique();
        if (filter != null) {
            filter.isShared = true;
        }
        return filter;
    }

    /**
     * Get the available filter configurations for a user and a data type.
     * 
     * @param principalUid
     *            the principal username
     * @param dataType
     *            the data type
     */
    public static List<FilterConfiguration> getAvailableFilterConfiguration(String principalUid, String dataType) {
        return find.orderBy("isDefault DESC, name ASC").where().eq("deleted", false).eq("principal.uid", principalUid).eq("dataType", dataType).findList();
    }

}
