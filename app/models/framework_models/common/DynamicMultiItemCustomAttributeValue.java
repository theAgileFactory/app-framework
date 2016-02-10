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
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;

import com.avaje.ebean.Model;

import framework.services.ServiceStaticAccessor;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.custom_attribute.ICustomAttributeManagerService;
import framework.utils.DefaultSelectableValueHolderCollection;
import framework.utils.Msg;
import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;
import play.Logger;
import play.api.data.Field;
import play.mvc.Controller;
import play.twirl.api.Html;

/**
 * Same than DynamicSingleItemCustomAttributeValue except the use can chose
 * multi values (thanks a checkbox list).
 * 
 * @author Johann Kohler
 */
@Entity
public class DynamicMultiItemCustomAttributeValue extends Model implements IModel, ICustomAttributeValue {

    public static Finder<Long, DynamicMultiItemCustomAttributeValue> find = new Finder<Long, DynamicMultiItemCustomAttributeValue>(
            DynamicMultiItemCustomAttributeValue.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String objectType;

    public Long objectId;

    public String values;

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
    public DynamicMultiItemCustomAttributeValue() {
    }

    @Override
    public String audit() {
        return "DynamicMultiItemCustomAttributeValue [id=" + id + ", deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", objectType=" + objectType
                + ", objectId=" + objectId + ", values=" + values + "]";
    }

    @Override
    public void defaults() {
        if (customAttributeDefinition != null) {
            this.values = customAttributeDefinition.getDefaultValueAsString();
        }
    }

    @Override
    public void doDelete() {
        deleted = true;
        save();
    }

    @Override
    public String toString() {
        return values;
    }

    @Override
    public CustomAttributeDefinition getDefinition() {
        return this.customAttributeDefinition;
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.DYNAMIC_MULTI_ITEM;
    }

    /**
     * Get or creation a value.
     * 
     * @param objectType
     *            the object type
     * @param filter
     *            the possible filter
     * @param objectId
     *            the object id
     * @param customAttributeDefinition
     *            the custom attribute definition
     */
    public static DynamicMultiItemCustomAttributeValue getOrCreateCustomAttributeValueFromObjectReference(Class<?> objectType, String filter, Long objectId,
            CustomAttributeDefinition customAttributeDefinition) {

        String key = objectType.getName();
        if (filter != null) {
            key += ":" + filter;
        }

        DynamicMultiItemCustomAttributeValue customAttributeValue = find.where().eq("deleted", false).eq("objectType", key).eq("objectId", objectId)
                .eq("customAttributeDefinition.id", customAttributeDefinition.id).findUnique();
        if (customAttributeValue == null) {
            customAttributeValue = new DynamicMultiItemCustomAttributeValue();
            customAttributeValue.objectType = key;
            customAttributeValue.objectId = objectId;
            customAttributeValue.customAttributeDefinition = customAttributeDefinition;
            customAttributeValue.isNotReadFromDb = true;
        }
        return customAttributeValue;
    }

    @Override
    public Object getValueAsObject() {
        return convertToList(this.values);
    }

    /**
     * Convert a string (1,4,5) to a list.
     * 
     * @param valuesAsString
     *            the string to convert
     */
    private static List<Long> convertToList(String valuesAsString) {
        List<Long> values = new ArrayList<>();
        if (valuesAsString != null) {
            String[] valuesAsArray = StringUtils.split(valuesAsString, MULTI_VALUE_SEPARATOR);
            for (String valueAsString : valuesAsArray) {
                try {
                    Long value = Long.parseLong(valueAsString);
                    if (value != null) {
                        values.add(value);
                    }
                } catch (Exception e) {
                    Logger.warn("impossible to parse as Long the value " + valueAsString);
                }
            }
        }
        return values;
    }

    /**
     * Convert a list to a string.
     * 
     * @param values
     *            the list of values to convert
     */
    private static String convertToString(List<Long> values) {
        List<String> valuesAsStringList = new ArrayList<>();
        if (values != null) {
            for (Long value : values) {
                if (value != null) {
                    valuesAsStringList.add(String.valueOf(value));
                }
            }
        }
        return String.join(String.valueOf(MULTI_VALUE_SEPARATOR), valuesAsStringList);
    }

    @Override
    public String print() {
        if (this.values == null) {
            return "";
        }
        return this.values;
    }

    @Override
    public boolean parse(II18nMessagesPlugin i18nMessagesPlugin, String text) {
        if (StringUtils.isBlank(text)) {
            if (customAttributeDefinition.isRequired()) {
                this.hasError = true;
                this.errorMessage = Msg.get(customAttributeDefinition.getRequiredMessage());
                return false;
            }
            this.values = null;
        } else {
            this.values = text;
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

        if (newValue == null) {
            if (customAttributeDefinition.isRequired()) {
                throw new IllegalArgumentException(
                        "Null is not aCustomAttributeMultiItemOption valid value for this custom attribute " + this.customAttributeDefinition.uuid);
            }
            this.values = null;
        } else {
            if (!(newValue instanceof List<?>)) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid
                        + " is a List<Long> attribute and is not compatible with value : " + newValue);
            }
            this.values = convertToString((List<Long>) newValue);
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
    public Html renderFormField(II18nMessagesPlugin i18nMessagesPlugin, Field field, boolean displayDescription) {

        String description = "";
        if (displayDescription) {
            description = Msg.get(customAttributeDefinition.description);
        }

        String uid = ServiceStaticAccessor.getUserSessionManagerPlugin().getUserSessionId(Controller.ctx());

        return views.html.framework_views.parts.checkboxlist.render(field, Msg.get(customAttributeDefinition.name), description,
                customAttributeDefinition.getValueHoldersCollectionFromNameForDynamicMultiItemCustomAttribute(i18nMessagesPlugin, uid), true, false,
                customAttributeDefinition.isRequired());

    }

    @Override
    public Html renderDisplay(II18nMessagesPlugin i18nMessagesPlugin) {
        DefaultSelectableValueHolderCollection<Long> selectableValueHolderCollection = new DefaultSelectableValueHolderCollection<Long>(
                convertToList(this.values));
        return views.html.framework_views.parts.formats.display_value_holder_collection.render(selectableValueHolderCollection, false);
    }

    @Override
    public Html renderDisplayNoDescription(II18nMessagesPlugin i18nMessagesPlugin) {
        return renderDisplay(i18nMessagesPlugin);
    }

    @Override
    public void performSave(ICustomAttributeManagerService customAttributeManagerService) {
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

}
