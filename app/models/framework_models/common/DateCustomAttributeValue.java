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
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.persistence.Version;

import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;
import play.api.data.Field;
import play.db.ebean.Model;
import play.twirl.api.Html;
import framework.utils.Msg;
import framework.utils.Utilities;
import framework.utils.formats.DateType;

/**
 * The value for an attribute which can be added to any object in the system.
 * Type of the value : {@link Date}<br/>
 * Here are the properties supported by this attribute:
 * <ul>
 * <li>default.value : the default value (can be "0" which will set the default
 * value to the current date or "n" where n is a number of days in the future or
 * "-n" where n is a number of days in the past)</li>
 * <li>constraint.required : if set to something, then the field is required</li>
 * <li>constraint.required.message : the message to be displayed if the field is
 * not provided</li>
 * <li>date.after : A date after which the date must be set (can be "0" which
 * will set the default value to the current date or "n" where n is a number of
 * days in the future or "-n" where n is a number of days in the past)</li>
 * <li>constraint.after.message : the message to be displayed if the date is not
 * after the specified date</li>
 * <li>date.before : A date before which the date must be set (can be "0" which
 * will set the default value to the current date or "n" where n is a number of
 * days in the future or "-n" where n is a number of days in the past)</li>
 * <li>constraint.before.message : the message to be displayed if the date is
 * not before the specified date</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class DateCustomAttributeValue extends Model implements IModel, ICustomAttributeValue {
    private static final long serialVersionUID = -676104249012732234L;

    public static Finder<Long, DateCustomAttributeValue> find = new Finder<Long, DateCustomAttributeValue>(Long.class, DateCustomAttributeValue.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String objectType;

    public Long objectId;

    @DateType
    public Date value;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public CustomAttributeDefinition customAttributeDefinition;

    @Transient
    private boolean hasError = false;

    @Transient
    private String errorMessage;

    @Transient
    private boolean isNotReadFromDb = false;

    public DateCustomAttributeValue() {
    }

    @Override
    public String audit() {
        return "DateCustomAttributeValue [id=" + id + ", deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", objectType=" + objectType + ", objectId="
                + objectId + ", value=" + value + "]";
    }

    @Override
    public void defaults() {
        if (customAttributeDefinition != null && customAttributeDefinition.getDefaultValueAsString() != null) {
            try {
                this.value = Utilities.getOffSetDate(Integer.parseInt(customAttributeDefinition.getDefaultValueAsString()));
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
        return AttributeType.DATE;
    }

    public static DateCustomAttributeValue getOrCreateCustomAttributeValueFromObjectReference(Class<?> objectType, String filter, Long objectId,
            CustomAttributeDefinition customAttributeDefinition) {

        String key = objectType.getName();
        if (filter != null) {
            key += ":" + filter;
        }

        DateCustomAttributeValue customAttributeValue =
                find.where().eq("deleted", false).eq("objectType", key).eq("objectId", objectId)
                        .eq("customAttributeDefinition.id", customAttributeDefinition.id).findUnique();
        if (customAttributeValue == null) {
            customAttributeValue = new DateCustomAttributeValue();
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
        return Utilities.getDateFormat(null, null).format(this.value);
    }

    @Override
    public boolean parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            this.value = null;
            if (customAttributeDefinition.isRequired()) {
                this.hasError = true;
                this.errorMessage = Msg.get(customAttributeDefinition.getRequiredMessage());
                return false;
            }
        } else {
            DateFormat dateFormat = Utilities.getDateFormat(null, null);
            try {
                this.value = dateFormat.parse(text);
            } catch (ParseException e) {
                this.hasError = true;
                this.errorMessage = Msg.get(GENERIC_INVALID_ERROR_MESSAGE);
                return false;
            }
            if (!customAttributeDefinition.isDateAfter(this.value)) {
                this.hasError = true;
                this.errorMessage =
                        Msg.get(customAttributeDefinition.getDateAfterMessage(),
                                Utilities.getDateFormat(null).format(customAttributeDefinition.getDateAfterBoundary()));
                return false;
            }
            if (!customAttributeDefinition.isDateBefore(this.value)) {
                this.hasError = true;
                this.errorMessage =
                        Msg.get(customAttributeDefinition.getDateBeforeMessage(),
                                Utilities.getDateFormat(null).format(customAttributeDefinition.getDateBeforeBoundary()));
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
                throw new IllegalArgumentException("Null is not a valid date for this custom attribute " + this.customAttributeDefinition.uuid);
            }
            this.value = null;
        } else {
            if (!(newValue instanceof Date)) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid
                        + " is a Date attribute and is not compatible with value : " + newValue);
            }
            this.value = (Date) newValue;
            if (!customAttributeDefinition.isDateAfter(this.value)) {
                throw new IllegalArgumentException("This custom attribute "
                        + this.customAttributeDefinition.uuid
                        + " is invalid : "
                        + Msg.get(customAttributeDefinition.getDateAfterMessage(),
                                Utilities.getDateFormat(null).format(customAttributeDefinition.getDateAfterBoundary())));
            }
            if (!customAttributeDefinition.isDateBefore(this.value)) {
                throw new IllegalArgumentException("This custom attribute "
                        + this.customAttributeDefinition.uuid
                        + " is invalid : "
                        + Msg.get(customAttributeDefinition.getDateBeforeMessage(),
                                Utilities.getDateFormat(null).format(customAttributeDefinition.getDateBeforeBoundary())));
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
    public Html renderFormField(Field field) {
        return views.html.framework_views.parts.dateinput
                .render(field, Msg.get(customAttributeDefinition.name), null, customAttributeDefinition.isRequired());
    }

    @Override
    public Html renderDisplay() {
        return views.html.framework_views.parts.formats.display_date.render(value, null, false);
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
