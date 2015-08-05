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

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;

import org.apache.commons.lang3.StringUtils;

import play.api.data.Field;
import play.twirl.api.Html;

import com.avaje.ebean.Model;

import framework.utils.Msg;

/**
 * The url custom attribute value.
 * 
 * <pre>
 * default.value: String
 *     the default value
 * constraint.required: boolean (default false)
 *     set to true if the field is required
 * constraint.required.message: String
 *     override the default required message
 * constraint.new_window: boolean (default false)
 *     set to true to open the link in a new window (target='_blank')
 * constraint.url.message: String
 *     override the default "bad url format" message
 * </pre>
 * 
 * @author Johann Kohler
 */
@Entity
@Table(name = "string_custom_attribute_value")
public class UrlCustomAttributeValue extends Model implements IModel, ICustomAttributeValue {

    private static final long serialVersionUID = -4457221547L;

    public static Finder<Long, UrlCustomAttributeValue> find = new Finder<Long, UrlCustomAttributeValue>(UrlCustomAttributeValue.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String objectType;

    public Long objectId;

    @Column(length = IModelConstants.LARGE_STRING)
    public String value;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public CustomAttributeDefinition customAttributeDefinition;

    @Transient
    private boolean hasError = false;

    @Transient
    private String errorMessage;

    @Transient
    private boolean isNotReadFromDb = false;

    public UrlCustomAttributeValue() {
    }

    @Override
    public String audit() {
        return "UrlCustomAttributeValue [id=" + id + ", deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", objectType=" + objectType + ", objectId="
                + objectId + ", value=" + value + "]";
    }

    @Override
    public void defaults() {
        if (customAttributeDefinition != null && customAttributeDefinition.getDefaultValueAsString() != null) {
            this.value = customAttributeDefinition.getDefaultValueAsString();
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
        return AttributeType.URL;
    }

    public static UrlCustomAttributeValue getOrCreateCustomAttributeValueFromObjectReference(Class<?> objectType, String filter, Long objectId,
            CustomAttributeDefinition customAttributeDefinition) {

        String key = objectType.getName();
        if (filter != null) {
            key += ":" + filter;
        }

        UrlCustomAttributeValue customAttributeValue = find.where().eq("deleted", false).eq("objectType", key).eq("objectId", objectId)
                .eq("customAttributeDefinition.id", customAttributeDefinition.id).findUnique();
        if (customAttributeValue == null) {
            customAttributeValue = new UrlCustomAttributeValue();
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
        return this.value;
    }

    @Override
    public boolean parse(String text) {
        this.value = text;
        if (StringUtils.isBlank(text) && customAttributeDefinition.isRequired()) {
            this.hasError = true;
            this.errorMessage = Msg.get(customAttributeDefinition.getRequiredMessage());
            return false;
        }
        if (text != null && !text.equals("") && !isValidUrl(text)) {
            this.hasError = true;
            this.errorMessage = Msg.get(customAttributeDefinition.getUrlMessage());
            return false;
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
            if (!(newValue instanceof String)) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid
                        + " is an url attribute and is not compatible with value : " + newValue);
            }
            String text = (String) newValue;
            if (text != null && !text.equals("") && !isValidUrl(text)) {
                throw new IllegalArgumentException("This custom attribute " + this.customAttributeDefinition.uuid + " is not valid : "
                        + Msg.get(customAttributeDefinition.getUrlMessage()));
            }
            this.value = text;
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
        return views.html.framework_views.parts.url_input.render(field, Msg.get(customAttributeDefinition.name), customAttributeDefinition.isRequired());
    }

    @Override
    public Html renderDisplay() {

        if (value != null && !value.equals("")) {

            String label = value;
            String[] values = value.split("/");
            if (values.length > 1) {
                label = values[values.length - 1];
            }

            return views.html.framework_views.parts.formats.display_url.render(value, label, this.customAttributeDefinition.isNewWindow());
        }

        return views.html.framework_views.parts.formats.display_object.render(value, false);

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

    private boolean isValidUrl(String urlStr) {
        try {
            new URL(urlStr);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public Object getAsSerializableValue() {
        return getValueAsObject();
    }

}
