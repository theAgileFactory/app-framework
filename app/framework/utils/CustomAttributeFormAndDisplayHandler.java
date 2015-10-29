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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import models.framework_models.common.CustomAttributeDefinition;
import models.framework_models.common.ICustomAttributeValue;
import play.data.Form;

/**
 * An utility class which manage the view and edition of custom attributes
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class CustomAttributeFormAndDisplayHandler {
    public static String CUSTOM_ATTRIBUTE_FORM_FIELD_NAME_EXTENSION = "_custattr_";

    /**
     * The extended attribute form field names are derivated from the
     * {@link CustomAttributeDefinition} uuid.<br/>
     * A standard extension is added.
     * 
     * @param attributeDefinitionUuid
     *            the custom attribute definition uuid
     * @return a form field name
     */
    public static String getFieldNameFromDefinitionUuid(String attributeDefinitionUuid) {
        return CUSTOM_ATTRIBUTE_FORM_FIELD_NAME_EXTENSION + attributeDefinitionUuid;
    }

    /**
     * The extended attribute form field names are derivated from the
     * {@link CustomAttributeDefinition} uuid.<br/>
     * A standard extension is added.
     * 
     * @param fieldName
     *            a form field name
     * @return the corresponding custom attribute definition uuid
     */
    public static String getDefinitionUuidFromFieldName(String fieldName) {
        if (fieldName.startsWith(CUSTOM_ATTRIBUTE_FORM_FIELD_NAME_EXTENSION) && fieldName.length() > CUSTOM_ATTRIBUTE_FORM_FIELD_NAME_EXTENSION.length()) {
            return fieldName.substring(CUSTOM_ATTRIBUTE_FORM_FIELD_NAME_EXTENSION.length());
        }
        return null;
    }

    /**
     * Return true if the specified type of object has one or more associated
     * custom attributes
     * 
     * @param clazz
     *            the same class
     * @return
     */
    public static boolean hasCustomAttributes(Class<?> clazz) {
        List<CustomAttributeDefinition> customAttributeDefinitions = CustomAttributeDefinition.getOrderedCustomAttributeDefinitions(clazz);
        return customAttributeDefinitions != null && customAttributeDefinitions.size() > 0;
    }

    public static boolean hasCustomAttributes(Class<?> clazz, String filter) {
        List<CustomAttributeDefinition> customAttributeDefinitions = CustomAttributeDefinition.getOrderedCustomAttributeDefinitions(clazz, filter);
        return customAttributeDefinitions != null && customAttributeDefinitions.size() > 0;
    }

    /**
     * Fill the specified form with the found custom attribute values.
     * 
     * @param <T>
     *            The class of the form
     * @param form
     *            The form
     * @param clazz
     *            The class of the custom attribute object
     * @param objectId
     *            The id of the object
     */
    public static <T> void fillWithValues(Form<T> form, Class<?> clazz, Long objectId) {
        fillWithValues(form, clazz, null, objectId, "");
    }

    /**
     * Fill the specified form with the found custom attribute values.
     * 
     * @param <T>
     *            The class of the form
     * @param form
     *            The form
     * @param clazz
     *            The class of the custom attribute object
     * @param filter
     *            the filter
     * @param objectId
     *            The id of the object
     */
    public static <T> void fillWithValues(Form<T> form, Class<?> clazz, String filter, Long objectId) {
        fillWithValues(form, clazz, filter, objectId, "");
    }

    /**
     * Fill a specific list field the specified form with the found custom
     * attribute values.
     * 
     * @param <T>
     *            The class of the form
     * @param form
     *            The form
     * @param clazz
     *            The class of the custom attribute object
     * @param filter
     *            the filter
     * @param listFieldName
     *            The name of the list field
     * @param objectIds
     *            the list of object ids, the indexes of the ids should
     *            correspond to field indexes
     */
    public static <T> void fillWithValues(Form<T> form, Class<?> clazz, String filter, String listFieldName, List<Long> objectIds) {
        for (int i = 0; i < objectIds.size(); i++) {
            fillWithValues(form, clazz, filter, objectIds.get(i), listFieldName + "[" + i + "].");
        }
    }

    private static <T> void fillWithValues(Form<T> form, Class<?> clazz, String filter, Long objectId, String prefix) {

        List<ICustomAttributeValue> values = null;
        if (filter != null) {
            values = CustomAttributeDefinition.getOrderedCustomAttributeValues(clazz, filter, objectId);
        } else {
            values = CustomAttributeDefinition.getOrderedCustomAttributeValues(clazz, objectId);
        }
        if (values != null) {
            for (ICustomAttributeValue value : values) {
                if (value.isNotReadFromDb()) {
                    value.defaults();
                }
                if (value.getAttributeType().isMultiValued()) {
                    // If multi-valued the print value is a comma separated list
                    // of ids
                    String[] stringValues = StringUtils.split(value.print(), ',');
                    int count = 0;
                    for (String stringValue : stringValues) {
                        form.data().put(prefix + getFieldNameFromDefinitionUuid(value.getDefinition().uuid) + "[" + count + "]", stringValue);
                        count++;
                    }
                } else {
                    form.data().put(prefix + getFieldNameFromDefinitionUuid(value.getDefinition().uuid), value.print());
                }
            }
        }
    }

    /**
     * Check the values of the custom attributes associated with the specified
     * class.<br/>
     * The values are expected to be stored in the form.
     * 
     * @param form
     *            a form of a certain class
     * @param clazz
     *            the same class
     */
    public static <T> boolean validateValues(Form<T> form, Class<?> clazz) {
        return validateOrSaveValues(form, clazz, -1L, false, "", false);
    }

    public static <T> boolean validateValues(Form<T> form, Class<?> clazz, String filter) {
        return validateOrSaveValues(form, clazz, filter, -1L, false, "", false);
    }

    public static <T> boolean validateValues(Form<T> form, Class<?> clazz, String filter, String listFieldName, Boolean onlyDisplayedOrRequired) {

        // get the indexes to validate
        Map<String, String> data = form.data();
        Set<Integer> indexes = new HashSet<>();
        for (String key : data.keySet()) {
            if (key.startsWith(listFieldName + "[")) {
                final Pattern pattern = Pattern.compile(listFieldName + "\\[(.+?)\\](.*)");
                final Matcher matcher = pattern.matcher(key);
                matcher.find();
                indexes.add(Integer.valueOf(matcher.group(1)));
            }
        }

        boolean result = false;
        for (Integer index : indexes) {
            result = validateOrSaveValues(form, clazz, filter, -1L, false, listFieldName + "[" + index + "].", onlyDisplayedOrRequired) || result;
        }
        return result;

    }

    /**
     * Check the values of the custom attributes associated with the specified
     * class AND (if they are valid) save them into the database.<br/>
     * The values are expected to be stored in the form.
     * <p>
     * <b>Please wrap this method within a database transaction</b>
     * </p>
     * 
     * @param form
     *            a form of a certain class
     * @param clazz
     *            the same class
     * @param objectId
     *            an object Id
     */
    public static <T> boolean validateAndSaveValues(Form<T> form, Class<?> clazz, Long objectId) {
        return validateOrSaveValues(form, clazz, objectId, true, "", false);
    }

    public static <T> boolean validateAndSaveValues(Form<T> form, Class<?> clazz, String filter, Long objectId) {
        return validateOrSaveValues(form, clazz, filter, objectId, true, "", false);
    }

    public static <T> boolean validateAndSaveValues(Form<T> form, Class<?> clazz, String filter, String listFieldName, List<Long> objectIds,
            Boolean onlyDisplayedOrRequired) {
        boolean result = false;
        for (int i = 0; i < objectIds.size(); i++) {
            result = validateOrSaveValues(form, clazz, filter, objectIds.get(i), true, listFieldName + "[" + i + "].", onlyDisplayedOrRequired) || result;
        }
        return result;
    }

    /**
     * A private implementation of both previous methods
     * <p>
     * <b>Please wrap this method within a database transaction</b>
     * </p>
     */
    private static <T> boolean validateOrSaveValues(Form<T> form, Class<?> clazz, Long objectId, boolean saveValues, String prefix,
            Boolean onlyDisplayedOrRequired) {
        return validateOrSaveValues(form, clazz, null, objectId, saveValues, prefix, onlyDisplayedOrRequired);
    }

    private static <T> boolean validateOrSaveValues(Form<T> form, Class<?> clazz, String filter, Long objectId, boolean saveValues, String prefix,
            Boolean onlyDisplayedOrRequired) {
        boolean hasErrors = false;
        Map<String, String> data = form.data();
        if (data != null) {
            List<ICustomAttributeValue> customAttributeValues = null;
            if (filter != null) {
                customAttributeValues = CustomAttributeDefinition.getOrderedCustomAttributeValues(clazz, filter, objectId);
            } else {
                customAttributeValues = CustomAttributeDefinition.getOrderedCustomAttributeValues(clazz, objectId);
            }
            if (customAttributeValues != null) {
                for (ICustomAttributeValue customAttributeValue : customAttributeValues) {

                    if (!onlyDisplayedOrRequired || customAttributeValue.getDefinition().isDisplayed || customAttributeValue.getDefinition().isRequired()) {

                        String fieldName = CustomAttributeFormAndDisplayHandler.getFieldNameFromDefinitionUuid(customAttributeValue.getDefinition().uuid);
                        if (customAttributeValue.getAttributeType().isMultiValued()) {
                            // Find the multiple values and create a comma
                            // separated
                            // list of values
                            List<String> stringValues = new ArrayList<String>();
                            for (String key : data.keySet()) {
                                if (key.startsWith(prefix + fieldName + "[")) {
                                    String stringValue = data.get(prefix + key);
                                    stringValues.add(stringValue);
                                }
                            }
                            customAttributeValue.parse(StringUtils.join(stringValues, ICustomAttributeValue.MULTI_VALUE_SEPARATOR));
                        } else {
                            customAttributeValue.parse(data.get(prefix + fieldName));
                        }
                        if (customAttributeValue.hasError()) {
                            hasErrors = true;
                            form.reject(prefix + fieldName, customAttributeValue.getErrorMessage());
                        } else {
                            if (saveValues) {
                                customAttributeValue.performSave();
                            }
                        }

                    }
                }
            }
        }
        return hasErrors;
    }
}
