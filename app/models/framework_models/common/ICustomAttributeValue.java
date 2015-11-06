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

import framework.utils.CustomAttributeFormAndDisplayHandler;
import framework.utils.Msg;
import play.api.data.Field;
import play.twirl.api.Html;

/**
 * Generic interface for each custom attribute object
 * 
 * @author Pierre-Yves Cloux
 */
public interface ICustomAttributeValue {
    public static final char MULTI_VALUE_SEPARATOR = ',';
    public static final String GENERIC_INVALID_ERROR_MESSAGE = "error.invalid";

    /**
     * Return false if the attribute has been extracted from the database and
     * true if it is not yet saved
     */
    public boolean isNotReadFromDb();

    /**
     * Initialze the custom attribute value with the configured default value
     */
    public void defaults();

    /**
     * Save the object into the db
     */
    public void performSave();

    /**
     * Return the definition for a custom attribute
     * 
     * @return a custom attribute definition
     */
    public CustomAttributeDefinition getDefinition();

    /**
     * Returns the type of the attribute
     * 
     * @return an attribute type
     */
    public AttributeType getAttributeType();

    /**
     * Returns an Object representation of the value
     */

    public Object getValueAsObject();

    /**
     * Update the value for the custom attribute using an object.<br/>
     * If the object is not of a compatible type, an exception is thrown.
     */
    public void setValueAsObject(Object newValue);

    /**
     * Returns a String representation of the value.<br/>
     * This method is to be used by the
     * {@link CustomAttributeFormAndDisplayHandler}.<br/>
     * <p>
     * <b>WARNING</b><br/>
     * In case of multi-valued attributes, the print method should return a
     * comma separated list of options Ids.
     * </p>
     */
    public String print();

    /**
     * Read the value of the object from the specified String representation
     * This method is to be used by the
     * {@link CustomAttributeFormAndDisplayHandler}.
     * <p>
     * <b>WARNING</b><br/>
     * In case of multi-valued attributes, provided value is a comma separated
     * list of option ids.
     * </p>
     * 
     * @return true if parsing was successful, false otherwise (this means that
     *         hasError() will return true)
     */
    public boolean parse(String text);

    /**
     * Read the value of the object from the specified File representation
     * 
     * @return true if parsing was successful, false otherwise (this means that
     *         hasError() will return true)
     */
    public boolean parseFile();

    /**
     * Return true if the custom attribute is in error
     * 
     * @return a boolean
     */
    public boolean hasError();

    /**
     * Return the error message associated with the error (if hasError is true).
     * <br/>
     * <b>What is expected is a "rendered" message and not an i18n key</b>
     * 
     * @return a String
     */
    public String getErrorMessage();

    /**
     * Reset the transient error variables. This method is called if the data is
     * read from the cache to be sure the error variables are reseted.
     */
    public void resetError();

    /**
     * Render a form field.
     * 
     * @param field
     *            a form field
     * @param displayDescription
     *            true if the field description (help) should be displayed if it
     *            exists
     * @return an Html display of the form field attached to the specified value
     */
    public Html renderFormField(Field field, boolean displayDescription);

    /**
     * Render a display.
     * 
     * @return an Html "read-only" display of the value
     */
    public Html renderDisplay();

    /**
     * An enumeration which tells about the type of the attribute.
     * 
     * @author Pierre-Yves Cloux
     */
    public static enum AttributeType {
        BOOLEAN, INTEGER, DECIMAL, STRING, TEXT, DATE, SINGLE_ITEM, DYNAMIC_SINGLE_ITEM, IMAGE, MULTI_ITEM, URL;

        private boolean isFileType = false;
        private boolean isMultiValued = false;

        static {
            IMAGE.isFileType = true;
            MULTI_ITEM.isMultiValued = true;
        }

        public boolean isFileType() {
            return isFileType;
        }

        public boolean isMultiValued() {
            return isMultiValued;
        }

        public String getLabel() {
            return Msg.get("custom_attribute.type." + name() + ".label");
        }

    }

    public Object getAsSerializableValue();
}
