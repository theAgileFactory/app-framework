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
 * Type of the value : {@link Integer}<br/>
 * Here are the properties supported by this attribute:
 * <ul>
 * <li>default.value : a number to be used as a default value</li>
 * <li>constraint.required : if set to something, then the field is required
 * </li>
 * <li>constraint.required.message : the message to be displayed if the field is
 * not provided</li>
 * <li>constraint.max : a number setting the max value for the field</li>
 * <li>constraint.max.message : an error message to be displayed if the max
 * value is reached</li>
 * <li>constraint.min : a number setting the min value for the field</li>
 * <li>constraint.min.message : an error message to be displayed of the value is
 * lower than the min</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class IntegerCustomAttributeValue extends Model implements IModel, ICustomAttributeValue {
    private static final long serialVersionUID = -676104249012732234L;

    public static Finder<Long, IntegerCustomAttributeValue> find = new Finder<Long, IntegerCustomAttributeValue>(IntegerCustomAttributeValue.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String objectType;

    public Long objectId;

    public Integer value;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public CustomAttributeDefinition customAttributeDefinition;

    @Transient
    private boolean hasError = false;

    @Transient
    private String errorMessage;

    @Transient
    private boolean isNotReadFromDb = false;

    public IntegerCustomAttributeValue() {
    }

    @Override
    public String audit() {
        return "IntegerCustomAttributeValue [id=" + id + ", deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", objectType=" + objectType + ", objectId="
                + objectId + ", value=" + value + "]";
    }

    @Override
    public void defaults() {
        if (customAttributeDefinition != null && customAttributeDefinition.getDefaultValueAsString() != null) {
            try {
                Integer integer = Integer.parseInt(customAttributeDefinition.getDefaultValueAsString());
                this.value = integer;
            } catch (NumberFormatException e) {
            }
        }
    }

    @Override
    public void doDelete() {
        deleted = true;
        save();
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    @Override
    public CustomAttributeDefinition getDefinition() {
        return this.customAttributeDefinition;
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.INTEGER;
    }

    public static IntegerCustomAttributeValue getOrCreateCustomAttributeValueFromObjectReference(Class<?> objectType, String filter, Long objectId,
            CustomAttributeDefinition customAttributeDefinition) {

        String key = objectType.getName();
        if (filter != null) {
            key += ":" + filter;
        }

        IntegerCustomAttributeValue customAttributeValue = find.where().eq("deleted", false).eq("objectType", key).eq("objectId", objectId)
                .eq("customAttributeDefinition.id", customAttributeDefinition.id).findUnique();
        if (customAttributeValue == null) {
            customAttributeValue = new IntegerCustomAttributeValue();
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
        return this.value.toString();
    }

    @Override
    public boolean parse(String text) {
        if (StringUtils.isBlank(text)) {
            this.value = null;
            if (customAttributeDefinition.isRequired()) {
                this.hasError = true;
                this.errorMessage = Msg.get(customAttributeDefinition.getRequiredMessage());
                return false;
            }
        } else {
            try {
                this.value = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                this.hasError = true;
                this.errorMessage = Msg.get(GENERIC_INVALID_ERROR_MESSAGE);
                return false;
            }
            if (this.value > customAttributeDefinition.maxBoundary()) {
                this.hasError = true;
                this.errorMessage = Msg.get(customAttributeDefinition.getMaxBoundaryMessage(), customAttributeDefinition.maxBoundary());
                return false;
            }
            if (this.value < customAttributeDefinition.minBoundary()) {
                this.hasError = true;
                this.errorMessage = Msg.get(customAttributeDefinition.getMinBoundaryMessage(), customAttributeDefinition.minBoundary());
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
                throw new IllegalArgumentException("Null is not a valid number for this custom attribute " + this.customAttributeDefinition.uuid);
            }
            this.value = null;
        } else {
            if (!(newValue instanceof Integer)) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid
                        + " is an Integer attribute and is not compatible with value : " + newValue);
            }
            this.value = (Integer) newValue;
            if (this.value > customAttributeDefinition.maxBoundary()) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid + " is not valid : "
                        + Msg.get(customAttributeDefinition.getMaxBoundaryMessage(), customAttributeDefinition.maxBoundary()));
            }
            if (this.value < customAttributeDefinition.minBoundary()) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid + " is not valid : "
                        + Msg.get(customAttributeDefinition.getMinBoundaryMessage(), customAttributeDefinition.minBoundary()));
            }
        }
    }

    @Override
    public boolean hasError() {
        return hasError;
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
    public Html renderFormField(Field field) {
        return views.html.framework_views.parts.basic_input_text.render(field, customAttributeDefinition.name, customAttributeDefinition.description,
                customAttributeDefinition.isRequired());
    }

    @Override
    public Html renderDisplay() {
        return views.html.framework_views.parts.formats.display_number.render(value, null, false);
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
        return getValueAsObject();
    }

}
