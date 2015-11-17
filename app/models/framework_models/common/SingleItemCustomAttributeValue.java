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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;

import com.avaje.ebean.Model;

import framework.utils.Msg;
import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;
import play.api.data.Field;
import play.twirl.api.Html;

/**
 * The value for an attribute which can be added to any object in the system.
 * Type of the value : {@link CustomAttributeItemOption} that is to say a value
 * taken from a pre-defined list.<br/>
 * Here are the properties supported by this attribute:
 * <ul>
 * <li>default.value : the "name" of an option which id must be used as a
 * default value</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class SingleItemCustomAttributeValue extends Model implements IModel, ICustomAttributeValue {

    public static Finder<Long, SingleItemCustomAttributeValue> find = new Finder<Long, SingleItemCustomAttributeValue>(SingleItemCustomAttributeValue.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String objectType;

    public Long objectId;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public CustomAttributeItemOption value;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public CustomAttributeDefinition customAttributeDefinition;

    @Transient
    private boolean hasError = false;

    @Transient
    private String errorMessage;

    @Transient
    private boolean isNotReadFromDb = false;

    /**
     * Default constructor.
     */
    public SingleItemCustomAttributeValue() {
    }

    @Override
    public String audit() {
        return "SingleItemCustomAttributeValue [id=" + id + ", deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", objectType=" + objectType
                + ", objectId=" + objectId + ", value=" + value + "]";
    }

    @Override
    public void defaults() {
        if (customAttributeDefinition != null && customAttributeDefinition.getDefaultValueAsString() != null) {
            CustomAttributeItemOption customAttributeItemOption = CustomAttributeItemOption
                    .getCustomAttributeItemOptionByName(customAttributeDefinition.getDefaultValueAsString());
            this.value = customAttributeItemOption;
        }
    }

    @Override
    public void doDelete() {
        deleted = true;
        save();
    }

    @Override
    public String toString() {
        return value != null ? value.getName() : String.valueOf(id);
    }

    @Override
    public CustomAttributeDefinition getDefinition() {
        return this.customAttributeDefinition;
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.SINGLE_ITEM;
    }

    public static SingleItemCustomAttributeValue getOrCreateCustomAttributeValueFromObjectReference(Class<?> objectType, String filter, Long objectId,
            CustomAttributeDefinition customAttributeDefinition) {

        String key = objectType.getName();
        if (filter != null) {
            key += ":" + filter;
        }

        SingleItemCustomAttributeValue customAttributeValue = find.where().eq("deleted", false).eq("objectType", key).eq("objectId", objectId)
                .eq("customAttributeDefinition.id", customAttributeDefinition.id).findUnique();
        if (customAttributeValue == null) {
            customAttributeValue = new SingleItemCustomAttributeValue();
            customAttributeValue.objectType = key;
            customAttributeValue.objectId = objectId;
            customAttributeValue.customAttributeDefinition = customAttributeDefinition;
            customAttributeValue.isNotReadFromDb = true;
        }
        return customAttributeValue;
    }

    @Override
    public Object getValueAsObject() {
        return this.value;
    }

    @Override
    public String print() {
        if (this.value == null) {
            return "";
        }
        return this.value.id.toString();
    }

    @Override
    public boolean parse(String text) {
        if (StringUtils.isBlank(text)) {

            if (customAttributeDefinition.isRequired()) {
                this.hasError = true;
                this.errorMessage = Msg.get(customAttributeDefinition.getRequiredMessage());
                return false;
            }

            this.value = null;

        } else {
            try {
                Long itemId = Long.parseLong(text);
                CustomAttributeItemOption itemOption = CustomAttributeItemOption.getCustomAttributeItemOptionById(itemId);
                this.value = itemOption;
            } catch (NumberFormatException e) {
                this.hasError = true;
                this.errorMessage = Msg.get(GENERIC_INVALID_ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean parseFile() {
        return false;
    }

    @Override
    public void setValueAsObject(Object newValue) {
        if (newValue == null) {
            if (customAttributeDefinition.isRequired()) {
                throw new IllegalArgumentException("Null is not a valid value for this custom attribute " + this.customAttributeDefinition.uuid);
            }
            this.value = null;
        } else {
            if (!(newValue instanceof Long)) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid
                        + " is a Long attribute and is not compatible with value : " + newValue);
            }
            CustomAttributeItemOption itemOption = CustomAttributeItemOption.getCustomAttributeItemOptionById((Long) newValue);
            this.value = itemOption;
        }
    }

    @Override
    public boolean hasError() {
        return this.hasError;
    }

    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Override
    public void resetError() {
        this.hasError = false;
        this.errorMessage = null;
    }

    @Override
    public Html renderFormField(Field field, boolean displayDescription) {
        String description = "";
        if (displayDescription) {
            description = Msg.get(customAttributeDefinition.description);
        }
        return views.html.framework_views.parts.dropdownlist.render(field, Msg.get(customAttributeDefinition.name),
                CustomAttributeItemOption.getSelectableValuesForDefinitionId(customAttributeDefinition.id), description, true,
                customAttributeDefinition.isRequired());
    }

    @Override
    public Html renderDisplay() {
        return views.html.framework_views.parts.formats.display_value_holder.render(value, false);
    }

    @Override
    public Html renderDisplayNoDescription() {
        return views.html.framework_views.parts.formats.display_value_holder.render(value, true);
    }

    @Override
    public void performSave() {
        save();
        this.isNotReadFromDb = false;
    }

    @Override
    public boolean isNotReadFromDb() {
        return isNotReadFromDb;
    }

    @Override
    public Object getAsSerializableValue() {
        if (value != null) {
            return value.getName();
        }
        return null;
    }

}
