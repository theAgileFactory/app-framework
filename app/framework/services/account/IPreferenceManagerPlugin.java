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

import play.data.Form;

/**
 * The interface to be implemented by the plugin which manages the preferences.
 * <br/>
 * A preference is not a configuration parameter since it should be modified by
 * the user itself.<br/>
 * 
 * There are two types of preferences:
 * <ul>
 * <li>user : which can be specified to a user</li>
 * <li>system : which are global for the entire application</li>
 * </ul>
 * 
 * A preference can be updated as wished.
 * 
 * @author Pierre-Yves Cloux
 */
public interface IPreferenceManagerPlugin {

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
    public String getPreferenceElseConfigurationValue(String preferenceName, String configurationName);

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
    public Integer getPreferenceElseConfigurationValueAsInteger(String preferenceName, String configurationName);

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
    public Boolean getPreferenceElseConfigurationValueAsBoolean(String preferenceName, String configurationName);

    /**
     * Get a String preference for the specified user.
     * 
     * @param uuid
     *            the name of the preference
     * @return a preference value
     * @throws PreferenceManagementException
     */
    public String getPreferenceValueAsString(String uuid) throws PreferenceManagementException;

    /**
     * Get an Integer preference for the specified user.
     * 
     * @param uuid
     *            the name of the preference
     * @return a preference value
     * @throws PreferenceManagementException
     */
    public Integer getPreferenceValueAsInteger(String uuid) throws PreferenceManagementException;

    /**
     * Get a Boolean preference for the specified user.
     * 
     * @param uuid
     *            the name of the preference
     * @return a preference value
     * @throws PreferenceManagementException
     */
    public Boolean getPreferenceValueAsBoolean(String uuid) throws PreferenceManagementException;

    /**
     * Get a Boolean preference for the specified user.
     * 
     * @param uuid
     *            the name of the preference
     * @return a preference value
     * @throws PreferenceManagementException
     */
    public BigDecimal getPreferenceValueAsDecimal(String uuid) throws PreferenceManagementException;

    /**
     * Get a Date preference for the specified user.
     * 
     * @param uuid
     *            the name of the preference
     * @return a preference value
     * @throws PreferenceManagementException
     */
    public Date getPreferenceValueAsDate(String uuid) throws PreferenceManagementException;

    /**
     * Update the value for a preference.
     * 
     * @param uuid
     *            the name of the preference
     * @param value
     *            a preference value (compatible with this preference)
     * 
     * @throws PreferenceManagementException
     */
    public void updatePreferenceValue(String uuid, Object value) throws PreferenceManagementException;

    /**
     * Fill the specified form with the specified preference.
     * 
     * @param <T>
     *            the object used for the form
     * @param form
     *            a form of a certain class
     * @param preferenceUuid
     *            a preference unique Id
     */
    public <T> void fillWithPreference(Form<T> form, String preferenceUuid);

    /**
     * Check the value of the preference associated with the specified uuid.
     * 
     * The value is expected to be stored in the form.
     * 
     * @param <T>
     *            the object used for the form
     * @param form
     *            a form of a certain class
     * @param preferenceUuid
     *            the uuid of a preference
     * @return true if an error is found
     */
    public <T> boolean validatePreference(Form<T> form, String preferenceUuid);

    /**
     * Check the value of the preference associated with the specified uuid AND
     * (if it is valid) save it into the database.<br/>
     * The value is expected to be stored in the form.
     * <p>
     * <b>Please wrap this method within a database transaction</b>
     * </p>
     * 
     * @param <T>
     *            the object used for the form
     * @param form
     *            a form of a certain class
     * @param preferenceUuid
     *            the uuid of a preference
     * @return true if an error is found
     */
    public <T> boolean validateAndSavePreference(Form<T> form, String preferenceUuid);

    /**
     * Get a property value as a sting.
     * 
     * @param uuid
     *            the preference uuid
     * @param propertyKey
     *            the property key
     */
    public String getPropertyAsString(String uuid, String propertyKey) throws PreferenceManagementException;

    /**
     * Get a property value as a boolean.
     * 
     * @param uuid
     *            the preference uuid
     * @param propertyKey
     *            the property key
     */
    public Boolean getPropertyAsBoolean(String uuid, String propertyKey) throws PreferenceManagementException;

    /**
     * Return true if the preference is a system preference.
     * 
     * @param uuid
     *            the preference uuid
     */
    public boolean isPreferenceSystem(String uuid);

}
