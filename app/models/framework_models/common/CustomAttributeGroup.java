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
import framework.utils.DefaultSelectableValueHolderCollection;
import framework.utils.ISelectableValueHolder;
import framework.utils.ISelectableValueHolderCollection;
import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

/**
 * The definition of a group of {@link CustomAttributeDefinition}
 *
 * @author Guillaume Petit
 */
@Entity
public class CustomAttributeGroup extends Model implements IModel, ISelectableValueHolder<Long> {

    public static final String DEFAULT_NAME = "DEFAULT";
    public static final String DEFAULT_LABEL = "object.custom_attribute_group.label";

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String objectType;

    @Column(length = IModelConstants.MEDIUM_STRING)
    public String name = DEFAULT_NAME;

    @Column(length = IModelConstants.LARGE_STRING)
    public String label = DEFAULT_LABEL;

    @Column(name = "`order`", scale = 5)
    public int order = 0;

    @OneToMany(mappedBy = "customAttributeGroup", cascade = CascadeType.ALL)
    public List<CustomAttributeDefinition> customAttributeDefinitions;

    public static Finder<Long, CustomAttributeGroup> find = new Finder<>(CustomAttributeGroup.class);

    public CustomAttributeGroup(String objectType) {
        this.objectType = objectType;
    }

    public CustomAttributeGroup() {
    }

    /**
     * Get a Custom Attribute Group by its id
     *
     * @param id the group id
     */
    public static CustomAttributeGroup getById(Long id) {
        return find.where().eq("deleted", false).eq("id", id).findUnique();
    }

    /**
     * Get custom attribute groups as value holder collection by object type
     *
     * @param objectType the custom attribute object type
     */
    public static ISelectableValueHolderCollection<Long> getOrderedCustomAttributeGroupsByObjectTypeAsVH(String objectType) {
        return new DefaultSelectableValueHolderCollection<>(getOrderedCustomAttributeGroupsByObjectType(objectType));
    }

    /**
     * Get all custom attribute groups for the given object type sorted by order
     *
     * @param objectType the custom attribute object type
     */
    public static List<CustomAttributeGroup> getOrderedCustomAttributeGroupsByObjectType(String objectType) {
        return find.where()
                .eq("deleted", false)
                .eq("objectType", objectType)
                .orderBy("order")
                .findList();
    }

    /**
     * Get the last order for a custom attribute group
     *
     * @param objectType the custom attribute object type
     */
    public static int getLastOrder(String objectType) {
        CustomAttributeGroup group = find.where()
                .eq("deleted", false)
                .eq("objectType", objectType)
                .orderBy("order DESC")
                .setMaxRows(1)
                .findUnique();

        return group == null ? -1 : group.order;
    }

    /**
     * Get the previous group in the list or null if there is none.
     */
    public CustomAttributeGroup previous() {
        return find.orderBy("order DESC")
                .where()
                .eq("deleted", false)
                .eq("objectType", this.objectType)
                .lt("order", this.order)
                .setMaxRows(1)
                .findUnique();
    }

    /**
     * Get the next group in the list or null if there is none.
     */
    public CustomAttributeGroup next() {
        return find.orderBy("order ASC")
                .where()
                .eq("deleted", false)
                .eq("objectType", this.objectType)
                .gt("order", this.order)
                .setMaxRows(1)
                .findUnique();
    }

    /**
     * Creates a default group when there are none for the given object type
     *
     * @param objectType the custom attribute object type
     */
    public static CustomAttributeGroup getOrCreateDefaultGroup(String objectType) {
        List<CustomAttributeGroup> groups = getOrderedCustomAttributeGroupsByObjectType(objectType);
        if (groups != null && groups.size() > 0) {
            return groups.get(0);
        }
        CustomAttributeGroup group = new CustomAttributeGroup(objectType);
        try {
            List<CustomAttributeDefinition> customAttributeDefinitions = CustomAttributeDefinition.getOrderedCustomAttributeDefinitions(Class.forName(objectType));
            customAttributeDefinitions.stream().forEach(definition -> {
                group.customAttributeDefinitions.add(definition);
                definition.customAttributeGroup = group;
                definition.save();
            });
        } catch (ClassNotFoundException ignored) {}
        group.save();
        return group;
    }

    @Override
    public String toString() {
        return "CustomAttributeGroup{" +
                "label='" + label + '\'' +
                ", id=" + id +
                ", deleted=" + deleted +
                ", lastUpdate=" + lastUpdate +
                ", objectType='" + objectType + '\'' +
                ", name='" + name + '\'' +
                ", order=" + order +
                '}';
    }

    @Override
    public String audit() {
        return "CustomAttributeGroup [id=\" + id + \", label=" + label + ", deleted=" + deleted +
                ", lastUpdate=" + lastUpdate + ", objectType=" + objectType +
                ", name=" + name + ", order=" + order + "]";
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
    public String getName() {
        return this.name;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Long getValue() {
        return this.id;
    }

    @Override
    public boolean isSelectable() {
        return true;
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
        CustomAttributeGroup c = (CustomAttributeGroup) o;
        return Integer.compare(this.order, c.order);
    }

}
