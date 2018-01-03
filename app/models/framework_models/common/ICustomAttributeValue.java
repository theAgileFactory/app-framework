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

import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.custom_attribute.ICustomAttributeManagerService;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import framework.utils.Msg;
import play.api.data.Field;
import play.twirl.api.Html;

/**
 * Generic interface for each custom attribute object
 * 
 * @author Pierre-Yves Cloux
 */
public interface ICustomAttributeValue {
    char MULTI_VALUE_SEPARATOR = ',';
    String GENERIC_INVALID_ERROR_MESSAGE = "error.invalid";

    /**
     * Return the object type to which belongs this custom attribute value
     * 
     * @return a java class name
     */
    String getLinkedObjectClassName();

    /**
     * Return the id of the object to which belongs this custom attribute value
     * 
     * @return a java class name
     */
    Long getLinkedObjectId();

    /**
     * Return false if the attribute has been extracted from the database and
     * true if it is not yet saved
     */
    boolean isNotReadFromDb();

    /**
     * Initialze the custom attribute value with the configured default value
     */
    void defaults();

    /**
     * Save the object into the db
     */
    void performSave(IUserSessionManagerPlugin userSessionManagerPlugin, IAttachmentManagerPlugin attachmentManagerPlugin, String fieldName);

    /**
     * Return the definition for a custom attribute
     * 
     * @return a custom attribute definition
     */
    CustomAttributeDefinition getDefinition();

    /**
     * Returns the type of the attribute
     * 
     * @return an attribute type
     */
    AttributeType getAttributeType();

    /**
     * Returns an Object representation of the value
     */

    Object getValueAsObject();

    /**
     * Update the value for the custom attribute using an object.<br/>
     * If the object is not of a compatible type, an exception is thrown.
     */
    void setValueAsObject(Object newValue);

    /**
     * Returns a String representation of the value.<br/>
     * <p>
     * <b>WARNING</b><br/>
     * In case of multi-valued attributes, the print method should return a
     * comma separated list of options Ids.
     * </p>
     */
    String print();

    /**
     * Read the value of the object from the specified String representation
     * <p>
     * <b>WARNING</b><br/>
     * In case of multi-valued attributes, provided value is a comma separated
     * list of option ids.
     * </p>
     * 
     * @return true if parsing was successful, false otherwise (this means that
     *         hasError() will return true)
     */
    boolean parse(II18nMessagesPlugin i18nMessagesPlugin, String text);

    /**
     * Read the value of the object from the specified File representation
     * 
     * @return true if parsing was successful, false otherwise (this means that
     *         hasError() will return true)
     */
    boolean parseFile(ICustomAttributeManagerService customAttributeManagerService);

    /**
     * Return true if the custom attribute is in error
     * 
     * @return a boolean
     */
    boolean hasError();

    /**
     * Return the error message associated with the error (if hasError is true).
     * <br/>
     * <b>What is expected is a "rendered" message and not an i18n key</b>
     * 
     * @return a String
     */
    String getErrorMessage();

    /**
     * Reset the transient error variables. This method is called if the data is
     * read from the cache to be sure the error variables are reseted.
     */
    void resetError();

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
    Html renderFormField(II18nMessagesPlugin i18nMessagesPlugin, IUserSessionManagerPlugin userSessionManagerPlugin,
                         IImplementationDefinedObjectService implementationDefinedObjectService, Field field, boolean displayDescription);

    /**
     * Render a display.
     * 
     * The description (if it exists) is displayed.
     */
    Html renderDisplay(II18nMessagesPlugin i18nMessagesPlugin);

    /**
     * Render a display.
     * 
     * The description (if it exists) is not displayed.
     */
    Html renderDisplayNoDescription(II18nMessagesPlugin i18nMessagesPlugin);

    /**
     * An enumeration which tells about the type of the attribute.
     * 
     * @author Pierre-Yves Cloux
     */
    enum AttributeType {
        BOOLEAN, INTEGER, DECIMAL, STRING, TEXT, DATE, SINGLE_ITEM, DYNAMIC_SINGLE_ITEM, DYNAMIC_MULTI_ITEM, IMAGE, MULTI_ITEM, URL, SCRIPT;

        private boolean isFileType = false;
        private boolean isMultiValued = false;

        static {
            IMAGE.isFileType = true;
            MULTI_ITEM.isMultiValued = true;
            DYNAMIC_MULTI_ITEM.isMultiValued = true;
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

    Object getAsSerializableValue();
}
