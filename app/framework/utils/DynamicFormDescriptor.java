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
package framework.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.mvc.Http.Request;

/**
 * Data structure to be used with the "display_dynamic_form" view.<br/>
 * This data structure allow to create a dynamic form to submit some values to
 * an URL as a POST.
 * 
 * @author Pierre-Yves Cloux
 */
public class DynamicFormDescriptor {
    private List<DynamicFormDescriptor.Field> fields;
    private String name;
    private String label;
    private String url;
    private String title;
    private String description;
    private String successMessage;

    public DynamicFormDescriptor(String url, String name, String label, String title, String description, String successMessage) {
        super();
        this.name = name;
        this.label = label;
        this.url = url;
        this.title = title;
        this.description = description;
        this.fields = new ArrayList<DynamicFormDescriptor.Field>();
        this.successMessage = successMessage;
    }

    /**
     * Extract the submitted values from the request
     * 
     * @param request
     *            a request
     * @return
     */
    public Map<String, Object> getFormContent(Request request) {
        Map<String, Object> map = new HashMap<String, Object>();
        final Map<String, String[]> values = request.body().asFormUrlEncoded();
        for (DynamicFormDescriptor.Field field : getFields()) {
            map.put(field.getName(), field.getValueFromString(values.get(getFieldId(field.getName()))));
        }
        return map;
    }

    /**
     * The HTML id to be used for the modal
     * 
     * @return a String
     */
    public String getModalId() {
        return String.format("_modal_%s", getName());
    }

    /**
     * The HTML id to be used for the form within the modal
     * 
     * @return a String
     */
    public String getFormId() {
        return String.format("_form_%s", getName());
    }

    /**
     * The JavaScript function which is called when a form is submitted
     * 
     * @return a String
     */
    public String getFormSubmitJavascriptFunctionName() {
        return String.format("_form_submit_%s", getName());
    }

    /**
     * The HTML form title Id to be used for the modal
     * 
     * @return a String
     */
    public String getFormTitleId() {
        return String.format("_form_title_%s", getName());
    }

    /**
     * The HTML button id to display the form
     * 
     * @return a String
     */
    public String getDisplayFormButtonId() {
        return String.format("_form_display_button_%s", getName());
    }

    /**
     * The HTML "save" form button id
     * 
     * @return a String
     */
    public String getSaveFormButtonId() {
        return String.format("_form_save_button_%s", getName());
    }

    /**
     * Return the HTML form field id created from the
     * {@link play.data.Form.Field} name
     * 
     * @param fieldName
     *            the name of a form Field
     * @return a String
     */
    public String getFieldId(String fieldName) {
        return String.format("_form_field_%s_%s", getName(), fieldName);
    }

    /**
     * Add a new field to the form
     * 
     * @param field
     */
    public void addField(DynamicFormDescriptor.Field field) {
        getFields().add(field);
    }

    public List<DynamicFormDescriptor.Field> getFields() {
        return fields;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    /**
     * The type of the field in the dynamic form.
     * 
     * @author Pierre-Yves Cloux
     */
    public enum FieldType {
        BOOLEAN, LIST_OF_VALUES, STRING
    }

    /**
     * Description of a dynamic field
     * 
     * @author Pierre-Yves Cloux
     */
    public static class Field {
        private String name;
        private String label;
        private DynamicFormDescriptor.FieldType type;
        private Object defaultValue;
        private Object parameters;

        public Field(String name, String label, DynamicFormDescriptor.FieldType type, Object defaultValue) {
            super();
            this.name = name;
            this.label = label;
            this.type = type;
            this.defaultValue = defaultValue;
        }

        public Field(String name, String label, DynamicFormDescriptor.FieldType type, Object defaultValue, Object parameters) {
            super();
            this.name = name;
            this.label = label;
            this.type = type;
            this.defaultValue = defaultValue;
            this.parameters = parameters;
        }

        public String getName() {
            return name;
        }

        public String getLabel() {
            return label;
        }

        public DynamicFormDescriptor.FieldType getType() {
            return type;
        }

        public Boolean getDefaultValueAsBoolean() {
            return (Boolean) defaultValue;
        }

        public String getDefaultValueAsString() {
            return (String) defaultValue;
        }

        @SuppressWarnings("unchecked")
        public List<String> getParametersAsList() {
            return (List<String>) parameters;
        }

        public Object getValueFromString(String[] value) {
            Object returnValue = null;
            switch (getType()) {
            case BOOLEAN:
                returnValue = (value != null ? Boolean.valueOf(value[0]) : false);
                break;
            case LIST_OF_VALUES:
                returnValue = (value != null ? value[0] : null);
                break;
            case STRING:
                returnValue = (value != null ? value[0] : null);
                break;
            }
            System.out.println(value + "=" + returnValue);
            return returnValue;
        }
    }

    public String getSuccessMessage() {
        return successMessage;
    }
}