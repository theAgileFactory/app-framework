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
package framework.services.account;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import framework.services.configuration.II18nMessagesPlugin;
import framework.services.custom_attribute.ICustomAttributeManagerService;
import framework.services.database.IDatabaseDependencyService;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import framework.utils.Msg;
import models.framework_models.account.Preference;
import models.framework_models.common.CustomAttributeItemOption;
import models.framework_models.common.ICustomAttributeValue;
import play.Configuration;
import play.Logger;
import play.cache.CacheApi;
import play.data.Form;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;

/**
 * The default implementation for the {@link IPreferenceManagerPlugin}.
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class DefaultPreferenceManagementPlugin implements IPreferenceManagerPlugin {
    private static Logger.ALogger log = Logger.of(DefaultPreferenceManagementPlugin.class);

    private Configuration configuration;
    private CacheApi cacheApi;
    private IUserSessionManagerPlugin userSessionManagerPlugin;
    private IAccountManagerPlugin accountManagerPlugin;
    private II18nMessagesPlugin i18nMessagesPlugin;
    private ICustomAttributeManagerService customAttributeManagerService;
    private IAttachmentManagerPlugin attachmentManagerPlugin;

    /**
     * Creates a new DefaultPreferenceManagementPlugin.
     * 
     * @param configuration
     *            the play configuration service
     * @param lifecycle
     *            the play application lifecyle listener
     * @param cacheApi
     *            the play cache API
     * @param userSessionManagerPlugin
     *            the user session manager service
     * @param accountManagerPlugin
     *            the account manager service
     * @param databaseDependencyService
     *            the database dependency service.
     * @param i18nMessagesPlugin
     *            the i18n messages service
     * @param customAttributeManagerService
     *            the custom attribute manager service
     * @param attachmentManagerPlugin
     *            the attachment manager service
     */
    @Inject
    public DefaultPreferenceManagementPlugin(Configuration configuration, ApplicationLifecycle lifecycle, CacheApi cacheApi,
            IUserSessionManagerPlugin userSessionManagerPlugin, IAccountManagerPlugin accountManagerPlugin,
            IDatabaseDependencyService databaseDependencyService, II18nMessagesPlugin i18nMessagesPlugin,
            ICustomAttributeManagerService customAttributeManagerService, IAttachmentManagerPlugin attachmentManagerPlugin) {
        this.configuration = configuration;
        this.cacheApi = cacheApi;
        this.userSessionManagerPlugin = userSessionManagerPlugin;
        this.accountManagerPlugin = accountManagerPlugin;
        this.i18nMessagesPlugin = i18nMessagesPlugin;
        this.customAttributeManagerService = customAttributeManagerService;
        this.attachmentManagerPlugin = attachmentManagerPlugin;
        log.info("SERVICE>>> DefaultPreferenceManagementPlugin starting...");
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> DefaultPreferenceManagementPlugin stopping...");
            log.info("SERVICE>>> DefaultPreferenceManagementPlugin stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> DefaultPreferenceManagementPlugin started");
    }

    /**
     * Get a value from a preference if it exists, else from a play
     * configuration.
     * 
     * Note: this method should be called after the preference manager service
     * has been started.
     * 
     * @param preferenceName
     *            the preference name
     * @param configurationName
     *            the play configuration name
     */
    public String getPreferenceElseConfigurationValue(String preferenceName, String configurationName) {

        String preferenceValue = getPreferenceValueAsString(preferenceName);

        if (preferenceValue != null && !preferenceValue.equals("")) {
            return preferenceValue;
        } else {
            return getConfiguration().getString(configurationName);
        }

    }

    /**
     * Get an Integer value from a preference if it exists, else from a play
     * configuration.
     * 
     * Note: this method should be called after the preference manager service
     * has been started.
     * 
     * @param preferenceName
     *            the preference name
     * @param configurationName
     *            the play configuration name
     */
    public Integer getPreferenceElseConfigurationValueAsInteger(String preferenceName, String configurationName) {

        Integer preferenceValue = getPreferenceValueAsInteger(preferenceName);

        if (preferenceValue != null) {
            return preferenceValue;
        } else {
            return getConfiguration().getInt(configurationName);
        }

    }

    /**
     * Get an Boolean value from a preference if it exists, else from a play
     * configuration.
     * 
     * Note: this method should be called after the preference manager service
     * has been started.
     * 
     * @param preferenceName
     *            the preference name
     * @param configurationName
     *            the play configuration name
     */
    public Boolean getPreferenceElseConfigurationValueAsBoolean(String preferenceName, String configurationName) {

        Boolean preferenceValue = getPreferenceValueAsBoolean(preferenceName);

        if (preferenceValue != null) {
            return preferenceValue;
        } else {
            return getConfiguration().getBoolean(configurationName);
        }

    }

    @Override
    public String getPreferenceValueAsString(String uuid) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid, getCacheApi(), getUserSessionManagerPlugin(),
                getAccountManagerPlugin());
        isAttributeExists(uuid, customAttributeValue);
        if (customAttributeValue.getAttributeType().equals(ICustomAttributeValue.AttributeType.STRING)
                || customAttributeValue.getAttributeType().equals(ICustomAttributeValue.AttributeType.IMAGE)) {
            return (String) customAttributeValue.getValueAsObject();
        }
        if (customAttributeValue.getAttributeType().equals(ICustomAttributeValue.AttributeType.SINGLE_ITEM)) {
            CustomAttributeItemOption customAttributeItemOption = (CustomAttributeItemOption) customAttributeValue.getValueAsObject();
            return customAttributeItemOption != null ? customAttributeItemOption.getNameAsKey() : null;
        }
        throw new PreferenceManagementException(
                String.format("Invalid type for %s String requested but %s found", uuid, customAttributeValue.getAttributeType()));
    }

    @Override
    public Integer getPreferenceValueAsInteger(String uuid) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid, getCacheApi(), getUserSessionManagerPlugin(),
                getAccountManagerPlugin());
        isAttributeExists(uuid, customAttributeValue);
        if (!customAttributeValue.getAttributeType().equals(ICustomAttributeValue.AttributeType.INTEGER)) {
            throw new PreferenceManagementException(
                    String.format("Invalid type for %s Integer requested but %s found", uuid, customAttributeValue.getAttributeType()));
        }
        return (Integer) customAttributeValue.getValueAsObject();
    }

    @Override
    public Boolean getPreferenceValueAsBoolean(String uuid) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid, getCacheApi(), getUserSessionManagerPlugin(),
                getAccountManagerPlugin());
        isAttributeExists(uuid, customAttributeValue);
        if (!customAttributeValue.getAttributeType().equals(ICustomAttributeValue.AttributeType.BOOLEAN)) {
            throw new PreferenceManagementException(
                    String.format("Invalid type for %s Boolean requested but %s found", uuid, customAttributeValue.getAttributeType()));
        }
        return (Boolean) customAttributeValue.getValueAsObject();
    }

    @Override
    public BigDecimal getPreferenceValueAsDecimal(String uuid) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid, getCacheApi(), getUserSessionManagerPlugin(),
                getAccountManagerPlugin());
        isAttributeExists(uuid, customAttributeValue);
        if (!customAttributeValue.getAttributeType().equals(ICustomAttributeValue.AttributeType.DECIMAL)) {
            throw new PreferenceManagementException(
                    String.format("Invalid type for %s Decimal requested but %s found", uuid, customAttributeValue.getAttributeType()));
        }
        return (BigDecimal) customAttributeValue.getValueAsObject();
    }

    @Override
    public Date getPreferenceValueAsDate(String uuid) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid, getCacheApi(), getUserSessionManagerPlugin(),
                getAccountManagerPlugin());
        isAttributeExists(uuid, customAttributeValue);
        if (!customAttributeValue.getAttributeType().equals(ICustomAttributeValue.AttributeType.DATE)) {
            throw new PreferenceManagementException(
                    String.format("Invalid type for %s Date requested but %s found", uuid, customAttributeValue.getAttributeType()));
        }
        return (Date) customAttributeValue.getValueAsObject();
    }

    @Override
    public void updatePreferenceValue(String uuid, Object value) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid, getCacheApi(), getUserSessionManagerPlugin(),
                getAccountManagerPlugin());
        isAttributeExists(uuid, customAttributeValue);
        customAttributeValue.setValueAsObject(value);
        Preference.savePreferenceValue(this.getCustomAttributeManagerService(), this.getUserSessionManagerPlugin(), this.getAttachmentManagerPlugin(),
                customAttributeValue, getCacheApi());
    }

    @Override
    public String getPropertyAsString(String uuid, String propertyKey) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid, getCacheApi(), getUserSessionManagerPlugin(),
                getAccountManagerPlugin());
        isAttributeExists(uuid, customAttributeValue);
        return customAttributeValue.getDefinition().getProperties().getProperty(propertyKey);
    }

    @Override
    public Boolean getPropertyAsBoolean(String uuid, String propertyKey) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid, getCacheApi(), getUserSessionManagerPlugin(),
                getAccountManagerPlugin());
        isAttributeExists(uuid, customAttributeValue);
        String s = customAttributeValue.getDefinition().getProperties().getProperty(propertyKey);
        if (s != null) {
            if (s.equals("true")) {
                return true;
            } else if (s.equals("false")) {
                return false;
            }
        }
        return null;
    }

    @Override
    public boolean isPreferenceSystem(String uuid) {
        return Preference.getPreferenceFromUuid(uuid).systemPreference;
    }

    @Override
    public <T> void fillWithPreference(Form<T> form, String preferenceUuid) {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(preferenceUuid, this.getCacheApi(),
                this.getUserSessionManagerPlugin(), this.getAccountManagerPlugin());
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
            String fieldId = this.getCustomAttributeManagerService().getFieldNameFromDefinitionUuid(customAttributeValue.getDefinition().uuid);
            String customAttributeDisplayedValue = customAttributeValue.print();
            if (log.isDebugEnabled()) {
                log.debug("Preference with uuid " + preferenceUuid + " set in form with fieldName " + fieldId + " with displayed value "
                        + customAttributeDisplayedValue);
            }
            form.data().put(fieldId, customAttributeDisplayedValue);
        }
    }

    @Override
    public <T> boolean validatePreference(Form<T> form, String preferenceUuid) {
        return validateOrSavePreference(form, preferenceUuid, false);
    }

    @Override
    public <T> boolean validateAndSavePreference(Form<T> form, String preferenceUuid) {
        return validateOrSavePreference(form, preferenceUuid, true);
    }

    /**
     * A private implementation of both previous methods.
     * 
     * @param <T>
     *            the object used for the form
     * @param form
     *            the form
     * @param preferenceUuid
     *            the uuid of the preference
     * @param saveValues
     *            true if the value should be saved
     */
    private <T> boolean validateOrSavePreference(Form<T> form, String preferenceUuid, boolean saveValues) {
        if (log.isDebugEnabled()) {
            log.debug("Request validation " + (saveValues ? " and saving" : "") + " for preference with uuid " + preferenceUuid);
        }
        boolean hasErrors = false;
        Map<String, String> data = form.data();
        if (data != null) {
            ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(preferenceUuid, this.getCacheApi(),
                    this.getUserSessionManagerPlugin(), this.getAccountManagerPlugin());
            String fieldName = this.getCustomAttributeManagerService().getFieldNameFromDefinitionUuid(customAttributeValue.getDefinition().uuid);
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
                    customAttributeValue.parse(this.getI18nMessagesPlugin(), formValue);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Readring preference with uuid " + preferenceUuid + " from form, attribute is a file");
                    }
                    customAttributeValue.parseFile(this.getCustomAttributeManagerService());
                }
                if (customAttributeValue.hasError()) {
                    hasErrors = true;
                    form.reject(fieldName, customAttributeValue.getErrorMessage());
                } else {
                    if (saveValues) {
                        if (log.isDebugEnabled()) {
                            log.debug("Readring preference with uuid " + preferenceUuid + " saved to database");
                        }
                        Preference.savePreferenceValue(this.getCustomAttributeManagerService(), this.getUserSessionManagerPlugin(),
                                this.getAttachmentManagerPlugin(), customAttributeValue, this.getCacheApi());
                    }
                }
            }
        }
        return hasErrors;
    }

    /**
     * Throw an exception is the customAttribute is null.
     * 
     * @param uuid
     *            the uuid of the preference
     * @param customAttributeValue
     *            a custom attribute value
     */
    private void isAttributeExists(String uuid, ICustomAttributeValue customAttributeValue) {
        if (customAttributeValue == null) {
            throw new PreferenceManagementException(String.format("Preference %s found", uuid));
        }
    }

    /**
     * Get the play cache API.
     */
    private CacheApi getCacheApi() {
        return cacheApi;
    }

    /**
     * Get the user session manager service.
     */
    private IUserSessionManagerPlugin getUserSessionManagerPlugin() {
        return userSessionManagerPlugin;
    }

    /**
     * Get the account manager service.
     */
    private IAccountManagerPlugin getAccountManagerPlugin() {
        return accountManagerPlugin;
    }

    /**
     * Get the play configuration service.
     */
    private Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Get the i18n messages service.
     */
    private II18nMessagesPlugin getI18nMessagesPlugin() {
        return this.i18nMessagesPlugin;
    }

    /**
     * Get the custom attribute manager service.
     */
    private ICustomAttributeManagerService getCustomAttributeManagerService() {
        return this.customAttributeManagerService;
    }

    /**
     * Get the attachment manager service.
     */
    private IAttachmentManagerPlugin getAttachmentManagerPlugin() {
        return this.attachmentManagerPlugin;
    }

}
