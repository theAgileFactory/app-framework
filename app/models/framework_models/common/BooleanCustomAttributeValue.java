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
 * Type of the value : {@link Boolean}<br/>
 * Here are the properties supported by this attribute:
 * <ul>
 * <li>default.value : the default value as a String (true or false)</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class BooleanCustomAttributeValue extends Model implements IModel, ICustomAttributeValue {

    public static Finder<Long, BooleanCustomAttributeValue> find = new Finder<Long, BooleanCustomAttributeValue>(BooleanCustomAttributeValue.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String objectType;

    public Long objectId;

    public boolean value;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public CustomAttributeDefinition customAttributeDefinition;

    @Transient
    private boolean isNotReadFromDb = false;

    /**
     * Default constructor.
     */
    public BooleanCustomAttributeValue() {
    }

    @Override
    public String audit() {
        return "BooleanCustomAttributeValue [id=" + id + ", deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", objectType=" + objectType + ", objectId="
                + objectId + ", value=" + value + "]";
    }

    @Override
    public void defaults() {
        if (customAttributeDefinition != null && customAttributeDefinition.getDefaultValueAsString() != null) {
            this.value = Boolean.parseBoolean(customAttributeDefinition.getDefaultValueAsString());
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
        return AttributeType.BOOLEAN;
    }

    public static BooleanCustomAttributeValue getOrCreateCustomAttributeValueFromObjectReference(Class<?> objectType, String filter, Long objectId,
            CustomAttributeDefinition customAttributeDefinition) {

        String key = objectType.getName();
        if (filter != null) {
            key += ":" + filter;
        }

        BooleanCustomAttributeValue customAttributeValue = find.where().eq("deleted", false).eq("objectType", key).eq("objectId", objectId)
                .eq("customAttributeDefinition.id", customAttributeDefinition.id).findUnique();
        if (customAttributeValue == null) {
            customAttributeValue = new BooleanCustomAttributeValue();
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
        return String.valueOf(this.value);
    }

    @Override
    public boolean parse(String text) {
        if (StringUtils.isBlank(text)) {
            this.value = false;
            return true;
        }
        this.value = Boolean.parseBoolean(text);
        return true;
    }

    @Override
    public boolean parseFile() {
        return false;
    }

    @Override
    public void setValueAsObject(Object newValue) {
        if (newValue == null) {
            // False by default
            this.value = false;
        } else {
            if (!(newValue instanceof Boolean)) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid
                        + " is a boolean attribute and is not compatible with value : " + newValue);
            }
            this.value = (Boolean) newValue;
        }
    }

    @Override
    public boolean hasError() {
        return false;
    }

    @Override
    public Html renderFormField(Field field) {
        return views.html.framework_views.parts.checkbox.render(field, Msg.get(customAttributeDefinition.name),
                Msg.get(customAttributeDefinition.description));
    }

    @Override
    public Html renderDisplay() {
        return views.html.framework_views.parts.formats.display_boolean.render(value);
    }

    @Override
    public void performSave() {
        save();
        this.isNotReadFromDb = false;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public void resetError() {
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
