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

import java.math.BigDecimal;
import java.math.BigInteger;
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
 * Type of the value : {@link BigDecimal}<br/>
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
public class DecimalCustomAttributeValue extends Model implements IModel, ICustomAttributeValue {

    public static Finder<Long, DecimalCustomAttributeValue> find = new Finder<Long, DecimalCustomAttributeValue>(DecimalCustomAttributeValue.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String objectType;

    public Long objectId;

    @Column(scale = IModelConstants.BIGNUMBER_SCALE, precision = IModelConstants.BIGNUMBER_PRECISION)
    public BigDecimal value;

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
    public DecimalCustomAttributeValue() {
    }

    @Override
    public String audit() {
        return "DecimalCustomAttributeValue [id=" + id + ", deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", objectType=" + objectType + ", objectId="
                + objectId + ", value=" + value + "]";
    }

    @Override
    public void defaults() {
        if (customAttributeDefinition != null && customAttributeDefinition.getDefaultValueAsString() != null) {
            try {
                BigDecimal bigDecimal = new BigDecimal(customAttributeDefinition.getDefaultValueAsString());
                this.value = bigDecimal;
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
        return AttributeType.DECIMAL;
    }

    public static DecimalCustomAttributeValue getOrCreateCustomAttributeValueFromObjectReference(Class<?> objectType, String filter, Long objectId,
            CustomAttributeDefinition customAttributeDefinition) {

        String key = objectType.getName();
        if (filter != null) {
            key += ":" + filter;
        }

        DecimalCustomAttributeValue customAttributeValue = find.where().eq("deleted", false).eq("objectType", key).eq("objectId", objectId)
                .eq("customAttributeDefinition.id", customAttributeDefinition.id).findUnique();
        if (customAttributeValue == null) {
            customAttributeValue = new DecimalCustomAttributeValue();
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
            return BigDecimal.ZERO.toPlainString();
        }
        return this.value.toPlainString();
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
                this.value = new BigDecimal(text);
            } catch (NumberFormatException e) {
                this.hasError = true;
                this.errorMessage = Msg.get(GENERIC_INVALID_ERROR_MESSAGE);
                return false;
            }
            if (this.value.longValue() > customAttributeDefinition.maxBoundary()) {
                this.hasError = true;
                this.errorMessage = Msg.get(customAttributeDefinition.getMaxBoundaryMessage(), customAttributeDefinition.maxBoundary());
                return false;
            }
            if (this.value.longValue() < customAttributeDefinition.minBoundary()) {
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
            if (!(newValue instanceof Number)) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid
                        + " is a Decimal attribute and is not compatible with value : " + newValue);
            }
            if (newValue instanceof Integer) {
                this.value = BigDecimal.valueOf((Integer) newValue);
            }
            if (newValue instanceof Long) {
                this.value = BigDecimal.valueOf((Long) newValue);
            }
            if (newValue instanceof Float) {
                this.value = BigDecimal.valueOf((Float) newValue);
            }
            if (newValue instanceof Double) {
                this.value = BigDecimal.valueOf((Double) newValue);
            }
            if (newValue instanceof BigInteger) {
                this.value = new BigDecimal((BigInteger) newValue);
            }
            if (newValue instanceof BigDecimal) {
                this.value = (BigDecimal) newValue;
            } else {
                throw new IllegalArgumentException(
                        "This custom attribute " + this.customAttributeDefinition.uuid + " cannot be updated with the value : " + newValue);
            }
            if (this.value.longValue() > customAttributeDefinition.maxBoundary()) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid + " is not valid : "
                        + Msg.get(customAttributeDefinition.getMaxBoundaryMessage(), customAttributeDefinition.maxBoundary()));
            }
            if (this.value.longValue() < customAttributeDefinition.minBoundary()) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid + " is not valid : "
                        + Msg.get(customAttributeDefinition.getMinBoundaryMessage(), customAttributeDefinition.minBoundary()));
            }
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
            description = customAttributeDefinition.description;
        }
        return views.html.framework_views.parts.basic_input_text.render(field, customAttributeDefinition.name, description,
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
