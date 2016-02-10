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

import framework.services.ServiceStaticAccessor;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.custom_attribute.ICustomAttributeManagerService;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import framework.utils.DefaultSelectableValueHolder;
import framework.utils.ISelectableValueHolder;
import framework.utils.ISelectableValueHolderCollection;
import framework.utils.Msg;
import framework.utils.Utilities;
import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;
import play.api.data.Field;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.twirl.api.Html;

/**
 * The value for an attribute which can be added to any object in the system.
 * Type of the value : {@link CustomAttributeItemOption} that is to say a value
 * taken from a pre-defined list.<br/>
 * Here are the properties supported by this attribute:
 * <ul>
 * <li>default.value : the "name" of an option which id must be used as a
 * default value</li>
 * <li>input.field.type : can be "DROPDOWN" or "AUTOCOMPLETE" (default is
 * AUTOCOMPLETE) and defines the type of input component to be used for edition
 * </li>
 * <li>selection.query : a SQL query<br/>
 * Example : select id as value, name as name from org_unit The query must
 * define two fields "value" and "name" (value is to be the value of the custom
 * attribute and must be a long).<br/>
 * </li>
 * <li>filter.where.clause : a "where clause element" to be added to the
 * selection query to filter the results based upon the search expression.<br/>
 * Example: where name like :searchstring<br/>
 * The where clause is added by String concatenation. If you already have a
 * where clause in your selection query, please add AND instead of WHERE
 * <u>NB</u>: a default parameter named "uid" is available for the query. It is
 * filled automatically with the currently logged Principal uid.</li>
 * <li>value.from.name.where.clause : a "where clause element" to be added to
 * the selection query to find one specific value from the "name" of the value
 * holder.<br/>
 * Example: where name = :nametofind<br/>
 * The where clause is added by String concatenation. If you already have a
 * where clause in your selection query, please add AND instead of WHERE</li>
 * <li>name.from.value.where.clause : a "where clause element" to be added to
 * the selection query to find one specific value from the "value" of the value
 * holder.<br/>
 * Example: where value = :valuetofind<br/>
 * The where clause is added by String concatenation. If you already have a
 * where clause in your selection query, please add AND instead of WHERE</li>
 * <li>max.records : an integrer which defines a limit for the number of
 * returned records (default is 10)</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class DynamicSingleItemCustomAttributeValue extends Model implements IModel, ICustomAttributeValue {

    public static Finder<Long, DynamicSingleItemCustomAttributeValue> find = new Finder<Long, DynamicSingleItemCustomAttributeValue>(
            DynamicSingleItemCustomAttributeValue.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String objectType;

    public Long objectId;

    public Long value;

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
    public DynamicSingleItemCustomAttributeValue() {
    }

    @Override
    public String audit() {
        return "DynamicSingleItemCustomAttributeValue [id=" + id + ", deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", objectType=" + objectType
                + ", objectId=" + objectId + ", value=" + value + "]";
    }

    @Override
    public void defaults() {
        if (customAttributeDefinition != null && customAttributeDefinition.getDefaultValueAsString() != null) {
            this.value = customAttributeDefinition.getDynamicDefaultValueAsLong();
        }
    }

    @Override
    public void doDelete() {
        deleted = true;
        save();
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public CustomAttributeDefinition getDefinition() {
        return this.customAttributeDefinition;
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.DYNAMIC_SINGLE_ITEM;
    }

    public static DynamicSingleItemCustomAttributeValue getOrCreateCustomAttributeValueFromObjectReference(Class<?> objectType, String filter, Long objectId,
            CustomAttributeDefinition customAttributeDefinition) {

        String key = objectType.getName();
        if (filter != null) {
            key += ":" + filter;
        }

        DynamicSingleItemCustomAttributeValue customAttributeValue = find.where().eq("deleted", false).eq("objectType", key).eq("objectId", objectId)
                .eq("customAttributeDefinition.id", customAttributeDefinition.id).findUnique();
        if (customAttributeValue == null) {
            customAttributeValue = new DynamicSingleItemCustomAttributeValue();
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
        return String.valueOf(this.value);
    }

    @Override
    public boolean parse(II18nMessagesPlugin i18nMessagesPlugin, String text) {
        if (StringUtils.isBlank(text) && customAttributeDefinition.isRequired()) {
            this.hasError = true;
            this.errorMessage = Msg.get(customAttributeDefinition.getRequiredMessage());
            return false;
        }
        if (StringUtils.isBlank(text)) {
            this.value = null;
        } else {
            try {
                Long itemId = Long.parseLong(text);
                String name = customAttributeDefinition.getNameFromValue(i18nMessagesPlugin, itemId);
                if (name != null) {
                    this.value = itemId;
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
            this.value = (Long) newValue;
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

        if (!customAttributeDefinition.isAutoComplete()) {
            String uid = ServiceStaticAccessor.getUserSessionManagerPlugin().getUserSessionId(Controller.ctx());
            return views.html.framework_views.parts.dropdownlist.render(field, Msg.get(customAttributeDefinition.name),
                    customAttributeDefinition.getValueHoldersCollectionFromNameForDynamicSingleItemCustomAttribute(i18nMessagesPlugin, "%", uid), description,
                    true, customAttributeDefinition.isRequired());
        }
        IImplementationDefinedObjectService implementationDefinedObjects = ServiceStaticAccessor.getImplementationDefinedObjectService();
        return views.html.framework_views.parts.autocomplete.render(field, Msg.get(customAttributeDefinition.name), description,
                implementationDefinedObjects.getRouteForDynamicSingleCustomAttributeApi().url(),
                customAttributeDefinition.getContextParametersForDynamicApi());
    }

    @Override
    public Html renderDisplay(II18nMessagesPlugin i18nMessagesPlugin) {
        DefaultSelectableValueHolder<Long> valueHolder = null;
        if (value != null) {
            valueHolder = new DefaultSelectableValueHolder<Long>(value, customAttributeDefinition.getNameFromValue(i18nMessagesPlugin, value));
        }
        return views.html.framework_views.parts.formats.display_value_holder.render(valueHolder, true);
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

    /**
     * The implementation of the JSON API to be used to manage the auto-complete
     * associated with this field
     * 
     * @return a JSON response
     */
    public static Result jsonQueryApi(II18nMessagesPlugin i18nMessagesPlugin) {
        // Retrieve the right custom attribute definition
        String customAttributeDefinitionIdAsString = Controller.request().queryString()
                .get(CustomAttributeDefinition.DYNAMIC_SINGLE_CUSTOM_ATTRIBUTE_DEFINITION_ID_CTX_PARAMETER) != null
                        ? Controller.request().queryString().get(CustomAttributeDefinition.DYNAMIC_SINGLE_CUSTOM_ATTRIBUTE_DEFINITION_ID_CTX_PARAMETER)[0]
                        : null;
        CustomAttributeDefinition customAttributeDefinition = null;
        try {
            if (customAttributeDefinitionIdAsString == null) {
                return Controller.badRequest();
            }
            customAttributeDefinition = CustomAttributeDefinition.getCustomAttributeDefinitionFromId(Long.parseLong(customAttributeDefinitionIdAsString));
            if (customAttributeDefinition == null) {
                return Controller.badRequest();
            }
        } catch (NumberFormatException e) {
            return Controller.badRequest();
        }

        String query = Controller.request().queryString().get("query") != null ? Controller.request().queryString().get("query")[0] : null;
        String value = Controller.request().queryString().get("value") != null ? Controller.request().queryString().get("value")[0] : null;

        if (query != null) {
            // Perform a search according to the specified query
            String uid = ServiceStaticAccessor.getUserSessionManagerPlugin().getUserSessionId(Controller.ctx());
            ISelectableValueHolderCollection<Long> valueHolders = customAttributeDefinition
                    .getValueHoldersCollectionFromNameForDynamicSingleItemCustomAttribute(i18nMessagesPlugin, query, uid);
            return Controller.ok(Utilities.marshallAsJson(valueHolders.getValues()));
        }
        if (value != null) {
            try {
                // Find the name associated with the specified value
                Long valueAsLong = Long.parseLong(value);
                String name = customAttributeDefinition.getNameFromValue(i18nMessagesPlugin, valueAsLong);
                ISelectableValueHolder<Long> valueHolder = new DefaultSelectableValueHolder<Long>(valueAsLong, name);
                return Controller.ok(Utilities.marshallAsJson(valueHolder, 0));
            } catch (NumberFormatException e) {
                return Controller.badRequest();
            }
        }
        return Controller.ok(Json.newObject());
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
