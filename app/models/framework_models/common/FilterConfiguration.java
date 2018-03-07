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

import com.avaje.ebean.Model;
import framework.utils.ISelectableValueHolder;
import models.framework_models.account.Principal;
import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;
import play.Logger;

import javax.persistence.*;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A filter configuration is a stored filter for a user and a data type.
 * 
 * @author Johann Kohler
 */
@Entity
public class FilterConfiguration extends Model implements IModel, ISelectableValueHolder<Long> {

    public static Finder<Long, FilterConfiguration> find = new Finder<>(FilterConfiguration.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @ManyToOne(cascade = CascadeType.ALL)
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
     * Get the link to reach a shared filter.
     * 
     * @param route
     *            the route that contains the filter table
     */
    public String getLink(String route) {

        try {

            URI uri = new URI(route);

            Map<String, String> queryPairs = new LinkedHashMap<>();
            if (uri.getQuery() != null) {
                String[] pairs = uri.getQuery().split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx != -1) {
                        queryPairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                    }
                }
            }
            queryPairs.put("filterSharedKey", this.sharedKey);

            StringBuilder sb = new StringBuilder();
            for (Entry<String, String> e : queryPairs.entrySet()) {
                if (sb.length() > 0) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(e.getValue(), "UTF-8"));
            }

            return uri.getPath() + "?" + sb.toString();

        } catch (Exception e) {
            Logger.error("impossible to construct the link for the filter configuration " + this.id, e);
            Logger.error("the route was: " + route);
            return null;
        }

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
     * The filter could be deleted or not.
     * 
     * @param sharedKey
     *            the share key
     * @param dataType
     *            the data type
     */
    public static FilterConfiguration getFilterConfigurationBySharedKey(String sharedKey, String dataType) {
        return find.where().eq("sharedKey", sharedKey).eq("dataType", dataType).findUnique();
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

    /**
     * Deselect the filter
     */
    public FilterConfiguration deselect() {
        if (this.isSelected) {
            this.isSelected = false;
            save();
        }
        return this;
    }
}
