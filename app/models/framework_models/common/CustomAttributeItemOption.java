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
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;
import play.db.ebean.Model;
import framework.utils.DefaultSelectableValueHolderCollection;
import framework.utils.ISelectableValueHolder;
import framework.utils.ISelectableValueHolderCollection;
import framework.utils.Msg;

/**
 * A possible value for an item (single or multi value) custom attribute.
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class CustomAttributeItemOption extends Model implements IModel, ISelectableValueHolder<Long> {
    private static final long serialVersionUID = -1005716640854077585L;

    public static Finder<Long, CustomAttributeItemOption> find = new Finder<Long, CustomAttributeItemOption>(Long.class, CustomAttributeItemOption.class);

    @Id
    public Long id;

    @Column(length = IModelConstants.LARGE_STRING, nullable = false)
    public String name;

    @Column(length = IModelConstants.VLARGE_STRING)
    public String description;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public CustomAttributeDefinition customAttributeDefinition;

    @OneToMany(mappedBy = "value", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<SingleItemCustomAttributeValue> singleItemCustomAttributeValue;

    public boolean deleted = false;

    @Column(name = "`order`", scale = 5)
    public int order;

    @Version
    public Timestamp lastUpdate;

    public CustomAttributeItemOption() {
    }

    @Override
    public String audit() {
        return "CustomAttributeItemOption [id=" + id + ", name=" + name + ", description=" + description + ", deleted=" + deleted + ", lastUpdate="
                + lastUpdate + "]";
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
    public String toString() {
        return name;
    }

    @Override
    public String getDescription() {
        return Msg.get(this.description);
    }

    @Override
    public String getName() {
        return Msg.get(this.name);
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
        return id;
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public int compareTo(Object o) {
        CustomAttributeItemOption c = (CustomAttributeItemOption) o;
        return this.order > c.order ? +1 : this.order < c.order ? -1 : 0;
    }

    /**
     * Returns the ordered list of values for the specified custom attribute
     * 
     * @param customAttributeDefinitionId
     *            a {@link CustomAttributeDefinition} id
     * @return a value holders collection
     */
    public static ISelectableValueHolderCollection<Long> getSelectableValuesForDefinitionId(Long customAttributeDefinitionId) {
        return new DefaultSelectableValueHolderCollection<Long>(find.where().eq("deleted", false)
                .eq("customAttributeDefinition.id", customAttributeDefinitionId).orderBy("order").findList());
    }

    public static CustomAttributeItemOption getCustomAttributeItemOptionById(Long id) {
        return find.where().eq("deleted", false).eq("id", id).findUnique();
    }

    public static CustomAttributeItemOption getCustomAttributeItemOptionByName(String name) {
        return find.where().eq("deleted", false).eq("name", name).findUnique();
    }

    /**
     * Get the item of a custom attribute with the previous order.
     * 
     * 
     * @param customAttributeDefinitionId
     *            the custom attribute definition id
     * @param order
     *            the current order
     */
    public static CustomAttributeItemOption getPrevious(Long customAttributeDefinitionId, int order) {
        return find.orderBy("order DESC").where().eq("deleted", false).eq("customAttributeDefinition.id", customAttributeDefinitionId).lt("order", order)
                .setMaxRows(1).findUnique();
    }

    /**
     * Get the item of a custom attribute with the next order.
     * 
     * 
     * @param customAttributeDefinitionId
     *            the custom attribute definition id
     * @param order
     *            the current order
     */
    public static CustomAttributeItemOption getNext(Long customAttributeDefinitionId, int order) {
        return find.orderBy("order ASC").where().eq("deleted", false).eq("customAttributeDefinition.id", customAttributeDefinitionId).gt("order", order)
                .setMaxRows(1).findUnique();
    }

    /**
     * Get the last order for a custom attribute.
     * 
     * @param customAttributeDefinitionId
     *            the custom attribute definition id
     * @return
     */
    public static Integer getLastOrder(Long customAttributeDefinitionId) {
        CustomAttributeItemOption last =
                find.orderBy("order DESC").where().eq("deleted", false).eq("customAttributeDefinition.id", customAttributeDefinitionId).setMaxRows(1)
                        .findUnique();
        if (last == null) {
            return -1;
        } else {
            return last.order;
        }

    }

}
