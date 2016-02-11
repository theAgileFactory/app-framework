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
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
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
import framework.utils.DefaultSelectableValueHolderCollection;
import framework.utils.Msg;
import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;
import play.Logger;
import play.api.data.Field;
import play.twirl.api.Html;

/**
 * The value for an attribute which can be added to any object in the system.
 * Type of the value : {@link CustomAttributeMultiItemOption} that is to say a
 * value taken from a pre-defined list.<br/>
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class MultiItemCustomAttributeValue extends Model implements IModel, ICustomAttributeValue {

    public static Finder<Long, MultiItemCustomAttributeValue> find = new Finder<Long, MultiItemCustomAttributeValue>(MultiItemCustomAttributeValue.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String objectType;

    public Long objectId;

    @ManyToMany(cascade = CascadeType.ALL, mappedBy = "multiItemCustomAttributeValues")
    public List<CustomAttributeMultiItemOption> values;

    @Transient
    public List<CustomAttributeMultiItemOption> temporaryValues;

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
    public MultiItemCustomAttributeValue() {
    }

    @Override
    public String audit() {
        return "MultiItemCustomAttributeValue [id=" + id + ", deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", objectType=" + objectType
                + ", objectId=" + objectId + "]";
    }

    @Override
    public void defaults() {
        if (customAttributeDefinition != null && customAttributeDefinition.getDefaultValueAsString() != null) {
            String[] defaultValuesAsString = customAttributeDefinition.getDefaultValueAsArrayOfString();
            this.values = new ArrayList<CustomAttributeMultiItemOption>();
            for (String defaultValue : defaultValuesAsString) {
                CustomAttributeMultiItemOption customAttributeMultiItemOption = CustomAttributeMultiItemOption
                        .getCustomAttributeMultiItemOptionByName(defaultValue);
                this.values.add(customAttributeMultiItemOption);
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
        if (values != null) {
            StringBuffer sb = new StringBuffer();
            for (CustomAttributeMultiItemOption value : values) {
                sb.append(value.getName()).append(',');
            }
        }
        return String.valueOf(id);
    }

    @Override
    public CustomAttributeDefinition getDefinition() {
        return this.customAttributeDefinition;
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.MULTI_ITEM;
    }

    public static MultiItemCustomAttributeValue getOrCreateCustomAttributeValueFromObjectReference(Class<?> objectType, String filter, Long objectId,
            CustomAttributeDefinition customAttributeDefinition) {

        String key = objectType.getName();
        if (filter != null) {
            key += ":" + filter;
        }

        MultiItemCustomAttributeValue customAttributeValue = find.where().eq("deleted", false).eq("objectType", key).eq("objectId", objectId)
                .eq("customAttributeDefinition.id", customAttributeDefinition.id).findUnique();
        if (customAttributeValue == null) {
            customAttributeValue = new MultiItemCustomAttributeValue();
            customAttributeValue.objectType = key;
            customAttributeValue.objectId = objectId;
            customAttributeValue.customAttributeDefinition = customAttributeDefinition;
            customAttributeValue.isNotReadFromDb = true;
        }
        return customAttributeValue;
    }

    @Override
    public Object getValueAsObject() {
        return this.values;
    }

    @Override
    public String print() {
        if (this.values == null) {
            return "";
        }
        List<String> stringValues = new ArrayList<String>();
        for (CustomAttributeMultiItemOption multiItemOption : this.values) {
            stringValues.add(String.valueOf(multiItemOption.id));
        }
        return StringUtils.join(stringValues, MULTI_VALUE_SEPARATOR);
    }

    @Override
    public boolean parse(II18nMessagesPlugin i18nMessagesPlugin, String text) {
        if (StringUtils.isBlank(text)) {
            if (customAttributeDefinition.isRequired()) {
                this.hasError = true;
                this.errorMessage = Msg.get(customAttributeDefinition.getRequiredMessage());
                return false;
            }
            this.temporaryValues = new ArrayList<CustomAttributeMultiItemOption>();
        } else {
            try {
                String[] selectedValues = StringUtils.split(text, MULTI_VALUE_SEPARATOR);
                this.temporaryValues = new ArrayList<CustomAttributeMultiItemOption>();
                for (String selectedValue : selectedValues) {
                    Long itemId = Long.parseLong(selectedValue);
                    CustomAttributeMultiItemOption itemOption = CustomAttributeMultiItemOption.getCustomAttributeMultiItemOptionById(itemId);
                    if (itemOption != null) {
                        this.temporaryValues.add(itemOption);
                    }
                }
            } catch (NumberFormatException e) {
                this.hasError = true;
                this.errorMessage = Msg.get(GENERIC_INVALID_ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean parseFile(ICustomAttributeManagerService customAttributeManagerService) {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValueAsObject(Object newValue) {
        // The value is supposed to be an array of
        // Long
        if (newValue == null) {
            if (customAttributeDefinition.isRequired()) {
                throw new IllegalArgumentException(
                        "Null is not aCustomAttributeMultiItemOption valid value for this custom attribute " + this.customAttributeDefinition.uuid);
            }
            this.temporaryValues = null;
        } else {
            if (!(newValue instanceof List<?>)) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid
                        + " is a List<Long> attribute and is not compatible with value : " + newValue);
            }
            List<Long> ids = (List<Long>) newValue;
            this.temporaryValues = new ArrayList<CustomAttributeMultiItemOption>();
            for (Long customAttributeMultiItemOptionId : ids) {
                CustomAttributeMultiItemOption itemOption = CustomAttributeMultiItemOption
                        .getCustomAttributeMultiItemOptionById(customAttributeMultiItemOptionId);
                if (itemOption != null) {
                    this.temporaryValues.add(itemOption);
                }
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
    public Html renderFormField(II18nMessagesPlugin i18nMessagesPlugin, IUserSessionManagerPlugin userSessionManagerPlugin,
            IImplementationDefinedObjectService implementationDefinedObjectService, Field field, boolean displayDescription) {
        String description = "";
        if (displayDescription) {
            description = customAttributeDefinition.description;
        }
        return views.html.framework_views.parts.checkboxlist.render(field, customAttributeDefinition.name, description,
                CustomAttributeMultiItemOption.getSelectableValuesForDefinitionId(customAttributeDefinition.id), true, true,
                customAttributeDefinition.isRequired());
    }

    @Override
    public Html renderDisplay(II18nMessagesPlugin i18nMessagesPlugin) {
        DefaultSelectableValueHolderCollection<Long> selectableValueHolderCollection = new DefaultSelectableValueHolderCollection<Long>(values);
        return views.html.framework_views.parts.formats.display_value_holder_collection.render(selectableValueHolderCollection, false);
    }

    @Override
    public Html renderDisplayNoDescription(II18nMessagesPlugin i18nMessagesPlugin) {
        DefaultSelectableValueHolderCollection<Long> selectableValueHolderCollection = new DefaultSelectableValueHolderCollection<Long>(values);
        return views.html.framework_views.parts.formats.display_value_holder_collection.render(selectableValueHolderCollection, true);
    }

    @Override
    public void performSave(IUserSessionManagerPlugin userSessionManagerPlugin, IAttachmentManagerPlugin attachmentManagerPlugin, String fieldName) {
        save();
        // Convert temporary values into values
        if (this.values != null) {
            // Delete all the values
            for (CustomAttributeMultiItemOption itemOption : this.values) {
                itemOption.multiItemCustomAttributeValues.remove(this);
                itemOption.save();
            }
        } else {
            this.values = new ArrayList<CustomAttributeMultiItemOption>();
        }
        // Add the temporary options and save
        for (CustomAttributeMultiItemOption itemOption : this.temporaryValues) {
            itemOption.multiItemCustomAttributeValues.add(this);
            itemOption.save();
        }
        this.isNotReadFromDb = false;
    }

    @Override
    public boolean isNotReadFromDb() {
        return isNotReadFromDb;
    }

    @Override
    public Object getAsSerializableValue() {

        if (values != null) {
            List<String> stringValues = new ArrayList<String>();
            for (CustomAttributeMultiItemOption multiItemOption : this.values) {
                stringValues.add(multiItemOption.getName());
                Logger.debug("multi item custom trad " + multiItemOption.getName());
            }
            return stringValues;
        }
        return null;
    }

    @Override
    public String getLinkedObjectClassName() {
        return objectType;
    }

    @Override
    public Long getLinkedObjectId() {
        return objectId;
    }
}
