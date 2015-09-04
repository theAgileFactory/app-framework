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

import javax.inject.Inject;
import javax.inject.Singleton;

import framework.services.database.IDatabaseDependencyService;
import models.framework_models.account.Preference;
import models.framework_models.common.CustomAttributeItemOption;
import models.framework_models.common.ICustomAttributeValue;
import play.Logger;
import play.cache.CacheApi;
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

    /**
     * Creates a new DefaultPreferenceManagementPlugin
     * 
     * @param lifecycle
     *            the play application lifecyle listener
     * @param databaseDependencyService
     */
    @Inject
    public DefaultPreferenceManagementPlugin(ApplicationLifecycle lifecycle, CacheApi cache, IDatabaseDependencyService databaseDependencyService) {
        log.info("SERVICE>>> DefaultPreferenceManagementPlugin starting...");
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> DefaultPreferenceManagementPlugin stopping...");
            log.info("SERVICE>>> DefaultPreferenceManagementPlugin stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> DefaultPreferenceManagementPlugin started");
    }

    @Override
    public String getPreferenceValueAsString(String uuid) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid);
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
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid);
        isAttributeExists(uuid, customAttributeValue);
        if (!customAttributeValue.getAttributeType().equals(ICustomAttributeValue.AttributeType.INTEGER)) {
            throw new PreferenceManagementException(
                    String.format("Invalid type for %s Integer requested but %s found", uuid, customAttributeValue.getAttributeType()));
        }
        return (Integer) customAttributeValue.getValueAsObject();
    }

    @Override
    public Boolean getPreferenceValueAsBoolean(String uuid) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid);
        isAttributeExists(uuid, customAttributeValue);
        if (!customAttributeValue.getAttributeType().equals(ICustomAttributeValue.AttributeType.BOOLEAN)) {
            throw new PreferenceManagementException(
                    String.format("Invalid type for %s Boolean requested but %s found", uuid, customAttributeValue.getAttributeType()));
        }
        return (Boolean) customAttributeValue.getValueAsObject();
    }

    @Override
    public BigDecimal getPreferenceValueAsDecimal(String uuid) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid);
        isAttributeExists(uuid, customAttributeValue);
        if (!customAttributeValue.getAttributeType().equals(ICustomAttributeValue.AttributeType.DECIMAL)) {
            throw new PreferenceManagementException(
                    String.format("Invalid type for %s Decimal requested but %s found", uuid, customAttributeValue.getAttributeType()));
        }
        return (BigDecimal) customAttributeValue.getValueAsObject();
    }

    @Override
    public Date getPreferenceValueAsDate(String uuid) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid);
        isAttributeExists(uuid, customAttributeValue);
        if (!customAttributeValue.getAttributeType().equals(ICustomAttributeValue.AttributeType.DATE)) {
            throw new PreferenceManagementException(
                    String.format("Invalid type for %s Date requested but %s found", uuid, customAttributeValue.getAttributeType()));
        }
        return (Date) customAttributeValue.getValueAsObject();
    }

    @Override
    public void updatePreferenceValue(String uuid, Object value) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid);
        isAttributeExists(uuid, customAttributeValue);
        customAttributeValue.setValueAsObject(value);
        Preference.savePreferenceValue(customAttributeValue);
    }

    @Override
    public String getPropertyAsString(String uuid, String propertyKey) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid);
        isAttributeExists(uuid, customAttributeValue);
        return customAttributeValue.getDefinition().getProperties().getProperty(propertyKey);
    }

    @Override
    public Boolean getPropertyAsBoolean(String uuid, String propertyKey) throws PreferenceManagementException {
        ICustomAttributeValue customAttributeValue = Preference.getPreferenceValueFromUuid(uuid);
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

    /**
     * Throw an exception is the customAttribute is null
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

}
