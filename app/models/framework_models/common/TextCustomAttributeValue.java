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
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;

import com.avaje.ebean.Model;

import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.custom_attribute.ICustomAttributeManagerService;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import framework.utils.Msg;
import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;
import play.api.data.Field;
import play.twirl.api.Html;

/**
 * Custom attribute to be used to store large text values (often not displayed
 * to the end user) The value for an attribute which can be added to any object
 * in the system. Type of the value : {@link String}<br/>
 * Here are the properties supported by this attribute:
 * <ul>
 * <li>default.value : a number to be used as a default value</li>
 * <li>constraint.required : if set to something, then the field is required
 * </li>
 * <li>constraint.required.message : the message to be displayed if the field is
 * not provided</li>
 * </ul>
 * 
 * @author Johann Kohler
 */
@Entity
public class TextCustomAttributeValue extends Model implements IModel, ICustomAttributeValue {

    public static Finder<Long, TextCustomAttributeValue> find = new Finder<Long, TextCustomAttributeValue>(TextCustomAttributeValue.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String objectType;

    public Long objectId;

    @Lob
    private byte[] value;

    /**
     * Get the value.
     */
    public String getValue() {
        try {
            return new String(this.value, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Set the value.
     * 
     * @param value
     *            the value to set
     */
    public void setValue(String value) {
        this.value = value == null ? null : value.getBytes();
    }

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
    public TextCustomAttributeValue() {
    }

    @Override
    public String audit() {
        return "TextCustomAttributeValue [id=" + id + ", deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", objectType=" + objectType + ", objectId="
                + objectId + ", value=" + value + "]";
    }

    @Override
    public void defaults() {
        if (customAttributeDefinition != null && customAttributeDefinition.getDefaultValueAsString() != null) {
            setValue(customAttributeDefinition.getDefaultValueAsString());
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
        return AttributeType.STRING;
    }

    public static TextCustomAttributeValue getOrCreateCustomAttributeValueFromObjectReference(Class<?> objectType, String filter, Long objectId,
            CustomAttributeDefinition customAttributeDefinition) {

        String key = objectType.getName();
        if (filter != null) {
            key += ":" + filter;
        }

        TextCustomAttributeValue customAttributeValue = find.where().eq("deleted", false).eq("objectType", key).eq("objectId", objectId)
                .eq("customAttributeDefinition.id", customAttributeDefinition.id).findUnique();
        if (customAttributeValue == null) {
            customAttributeValue = new TextCustomAttributeValue();
            customAttributeValue.objectType = key;
            customAttributeValue.objectId = objectId;
            customAttributeValue.customAttributeDefinition = customAttributeDefinition;
            customAttributeValue.isNotReadFromDb = true;
        }
        return customAttributeValue;
    }

    @Override
    public Object getValueAsObject() {
        return getValue();
    }

    @Override
    public String print() {
        if (getValue() == null) {
            return "";
        }
        return getValue();
    }

    @Override
    public boolean parse(II18nMessagesPlugin i18nMessagesPlugin, String text) {
        setValue(text);
        if (StringUtils.isBlank(text) && customAttributeDefinition.isRequired()) {
            this.hasError = true;
            this.errorMessage = Msg.get(customAttributeDefinition.getRequiredMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean parseFile(ICustomAttributeManagerService customAttributeManagerService) {
        return false;
    }

    @Override
    public void setValueAsObject(Object newValue) {
        if (newValue == null) {
            if (customAttributeDefinition.isRequired()) {
                throw new IllegalArgumentException("Null is not a valid value for this custom attribute " + this.customAttributeDefinition.uuid);
            }
            setValue(null);
        } else {
            if (!(newValue instanceof String)) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid
                        + " is a text attribute and is not compatible with value : " + newValue);
            }
            setValue((String) newValue);
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
    public Html renderFormField(II18nMessagesPlugin i18nMessagesPlugin, IUserSessionManagerPlugin userSessionManagerPlugin,
            IImplementationDefinedObjectService implementationDefinedObjectService, Field field, boolean displayDescription) {
        String description = "";
        if (displayDescription) {
            description = customAttributeDefinition.description;
        }
        return views.html.framework_views.parts.textarea.render(field, customAttributeDefinition.name, description, customAttributeDefinition.isRequired());
    }

    @Override
    public Html renderDisplay(II18nMessagesPlugin i18nMessagesPlugin) {
        return views.html.framework_views.parts.formats.display_object.render(getValue(), true);
    }

    @Override
    public Html renderDisplayNoDescription(II18nMessagesPlugin i18nMessagesPlugin) {
        return renderDisplay(i18nMessagesPlugin);
    }

    @Override
    public void performSave(IUserSessionManagerPlugin userSessionManagerPlugin, IAttachmentManagerPlugin attachmentManagerPlugin, String fieldName) {
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

    @Override
    public String getLinkedObjectClassName() {
        return objectType;
    }

    @Override
    public Long getLinkedObjectId() {
        return objectId;
    }

    public static void cloneInDB(Class<?> objectType, Long oldObjectId, Long newObjectId, CustomAttributeDefinition customAttributeDefinition) {
        TextCustomAttributeValue oldCustomAttributeValue = getOrCreateCustomAttributeValueFromObjectReference(objectType, null, oldObjectId, customAttributeDefinition);

        TextCustomAttributeValue newCustomAttributeValue = new TextCustomAttributeValue();
        newCustomAttributeValue.customAttributeDefinition = customAttributeDefinition;
        newCustomAttributeValue.deleted = oldCustomAttributeValue.deleted;
        newCustomAttributeValue.objectId = newObjectId;
        newCustomAttributeValue.objectType = oldCustomAttributeValue.objectType;
        newCustomAttributeValue.setValue(oldCustomAttributeValue.getValue());
        newCustomAttributeValue.save();
    }
}
