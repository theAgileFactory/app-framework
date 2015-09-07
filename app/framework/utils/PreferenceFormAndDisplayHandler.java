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

import java.util.Map;

import framework.services.ServiceStaticAccessor;
import models.framework_models.account.Preference;
import models.framework_models.common.CustomAttributeDefinition;
import models.framework_models.common.ICustomAttributeValue;
import play.Logger;
import play.data.Form;

/**
 * An utility which is to be used with {@link Preference} edition GUI.<br/>
 * <b>NB</b> : The {@link Preference} are actually based upon
 * {@link CustomAttributeDefinition} instances. This explains why this handler
 * actually extends the {@link CustomAttributeFormAndDisplayHandler}
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class PreferenceFormAndDisplayHandler extends CustomAttributeFormAndDisplayHandler {
    private static Logger.ALogger log = Logger.of(PreferenceFormAndDisplayHandler.class);

    /**
     * Fill the specified form with the specified preference
     * 
     * @param form
     *            a form of a certain class
     * @param preferenceUuid
     *            a preference unique Id
     */
    public static <T> void fillWithPreference(Form<T> form, String preferenceUuid) {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(preferenceUuid, ServiceStaticAccessor.getCacheApi(),
                ServiceStaticAccessor.getUserSessionManagerPlugin(), ServiceStaticAccessor.getAccountManagerPlugin());
        if (log.isDebugEnabled()) {
            log.debug("Preference with uuid " + preferenceUuid + " is " + (customAttributeValue != null ? "not null" : "null"));
        }
        if (customAttributeValue != null) {
            if (customAttributeValue.isNotReadFromDb()) {
                if (log.isDebugEnabled()) {
                    log.debug("Preference with uuid " + preferenceUuid + " is not read from db, loading default value");
                }
                customAttributeValue.defaults();
            }
            String fieldId = getFieldNameFromDefinitionUuid(customAttributeValue.getDefinition().uuid);
            String customAttributeDisplayedValue = customAttributeValue.print();
            if (log.isDebugEnabled()) {
                log.debug("Preference with uuid " + preferenceUuid + " set in form with fieldName " + fieldId + " with displayed value "
                        + customAttributeDisplayedValue);
            }
            form.data().put(fieldId, customAttributeDisplayedValue);
        }
    }

    /**
     * Check the value of the preference associated with the specified uuid.
     * <br/>
     * The value is expected to be stored in the form.
     * 
     * @param form
     *            a form of a certain class
     * @param preferenceUuid
     *            the uuid of a preference
     * @return true if an error is found
     */
    public static <T> boolean validatePreference(Form<T> form, String preferenceUuid) {
        return validateOrSavePreference(form, preferenceUuid, false);
    }

    /**
     * Check the value of the preference associated with the specified uuid AND
     * (if it is valid) save it into the database.<br/>
     * The value is expected to be stored in the form.
     * <p>
     * <b>Please wrap this method within a database transaction</b>
     * </p>
     * 
     * @param form
     *            a form of a certain class
     * @param preferenceUuid
     *            the uuid of a preference
     * @return true if an error is found
     */
    public static <T> boolean validateAndSavePreference(Form<T> form, String preferenceUuid) {
        return validateOrSavePreference(form, preferenceUuid, true);
    }

    /**
     * A private implementation of both previous methods
     * <p>
     * <b>Please wrap this method within a database transaction</b>
     * </p>
     */
    private static <T> boolean validateOrSavePreference(Form<T> form, String preferenceUuid, boolean saveValues) {
        if (log.isDebugEnabled()) {
            log.debug("Request validation " + (saveValues ? " and saving" : "") + " for preference with uuid " + preferenceUuid);
        }
        boolean hasErrors = false;
        Map<String, String> data = form.data();
        if (data != null) {
            ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(preferenceUuid, ServiceStaticAccessor.getCacheApi(),
                    ServiceStaticAccessor.getUserSessionManagerPlugin(), ServiceStaticAccessor.getAccountManagerPlugin());
            String fieldName = getFieldNameFromDefinitionUuid(customAttributeValue.getDefinition().uuid);
            if (log.isDebugEnabled()) {
                log.debug("Readring preference with uuid " + preferenceUuid + " from form with field name " + fieldName);
            }
            if (!customAttributeValue.getAttributeType().isFileType() && data.get(fieldName) != null && data.get(fieldName).equals("")) {
                hasErrors = true;
                form.reject(fieldName, Msg.get("error.required"));
            } else {
                if (!customAttributeValue.getAttributeType().isFileType()) {
                    String formValue = data.get(fieldName);
                    if (log.isDebugEnabled()) {
                        log.debug("Readring preference with uuid " + preferenceUuid + " from form, value is " + formValue);
                    }
                    customAttributeValue.parse(formValue);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Readring preference with uuid " + preferenceUuid + " from form, attribute is a file");
                    }
                    customAttributeValue.parseFile();
                }
                if (customAttributeValue.hasError()) {
                    hasErrors = true;
                    form.reject(fieldName, customAttributeValue.getErrorMessage());
                } else {
                    if (saveValues) {
                        if (log.isDebugEnabled()) {
                            log.debug("Readring preference with uuid " + preferenceUuid + " saved to database");
                        }
                        Preference.savePreferenceValue(customAttributeValue, ServiceStaticAccessor.getCacheApi());
                    }
                }
            }
        }
        return hasErrors;
    }
}
