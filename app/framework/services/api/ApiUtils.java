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
package framework.services.api;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.Modifier;

import com.fasterxml.jackson.annotation.JsonProperty;

import framework.services.api.commons.JsonPropertyLink;

/**
 * An utility class which gathers some methods used by the API management
 * features
 * 
 * @author Pierre-Yves Cloux
 */
abstract class ApiUtils {
    /**
     * Analyze the structure of an object to identify the values which can be
     * serialized
     * 
     * @param beanClass
     *            the class of the object
     * @param bean
     *            the value of the object (if null only the structure of the
     *            classes is collected)
     * @return a map of description objects
     */
    public static Map<String, SerializationEntry> getSerializationEntries(Class<?> beanClass, Object bean) {
        Map<String, SerializationEntry> entries = new HashMap<String, SerializationEntry>();
        try {
            // Look for annotated getters
            for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(beanClass).getPropertyDescriptors()) {
                if (propertyDescriptor.getReadMethod().isAnnotationPresent(JsonProperty.class)
                        || propertyDescriptor.getReadMethod().isAnnotationPresent(JsonPropertyLink.class)) {
                    SerializationEntry entry = new SerializationEntry();
                    entry.propertyName = propertyDescriptor.getReadMethod().getName();
                    if (propertyDescriptor.getReadMethod().isAnnotationPresent(JsonProperty.class)
                            && !StringUtils.isBlank(propertyDescriptor.getReadMethod().getAnnotation(JsonProperty.class).value())) {
                        entry.propertyName = propertyDescriptor.getReadMethod().getAnnotation(JsonProperty.class).value();
                    }
                    if (propertyDescriptor.getReadMethod().isAnnotationPresent(JsonPropertyLink.class)) {
                        if (!StringUtils.isBlank(propertyDescriptor.getReadMethod().getAnnotation(JsonPropertyLink.class).value())) {
                            entry.propertyName = propertyDescriptor.getReadMethod().getAnnotation(JsonPropertyLink.class).value();
                        }
                        entry.isLink = true;
                        entry.linkfield = propertyDescriptor.getReadMethod().getAnnotation(JsonPropertyLink.class).linkField();
                    }
                    entry.propertyType = propertyDescriptor.getPropertyType();
                    if (bean != null) {
                        entry.propertyValue = PropertyUtils.getProperty(bean, propertyDescriptor.getName());
                    }
                    entries.put(entry.propertyName, entry);
                }
            }
            // Look for annotated public properties
            for (final Field field : beanClass.getDeclaredFields()) {
                if (Modifier.isPublic(field.getModifiers())
                        && (field.isAnnotationPresent(JsonProperty.class) || field.isAnnotationPresent(JsonPropertyLink.class))) {
                    SerializationEntry entry = new SerializationEntry();
                    entry.propertyName = field.getName();
                    if (field.isAnnotationPresent(JsonProperty.class) && !StringUtils.isBlank(field.getAnnotation(JsonProperty.class).value())) {
                        entry.propertyName = field.getAnnotation(JsonProperty.class).value();
                    }
                    if (field.isAnnotationPresent(JsonPropertyLink.class)) {
                        if (!StringUtils.isBlank(field.getAnnotation(JsonPropertyLink.class).value())) {
                            entry.propertyName = field.getAnnotation(JsonPropertyLink.class).value();
                        }
                        entry.isLink = true;
                        entry.linkfield = field.getAnnotation(JsonPropertyLink.class).linkField();
                    }
                    entry.propertyType = field.getType();
                    if (bean != null) {
                        entry.propertyValue = field.get(bean);
                    }
                    entries.put(entry.propertyName, entry);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse the object " + beanClass, e);
        }
        return entries;
    }

    /**
     * An object which describes an entry to be serialized
     *
     * @author Pierre-Yves Cloux
     *
     */
    public static class SerializationEntry {
        public String propertyName;
        public Class<?> propertyType;
        public Object propertyValue;
        public boolean isLink;
        public String linkfield;

        @Override
        public String toString() {
            return "SerializationEntry [propertyName=" + propertyName + ", propertyType=" + propertyType + "]";
        }
    }
}
