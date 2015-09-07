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
     * Get a String preference for the specified user
     * 
     * @param uuid
     *            the name of the preference
     * @return a preference value
     * @throws PreferenceManagementException
     */
    public String getPreferenceValueAsString(String uuid) throws PreferenceManagementException;

    /**
     * Get an Integer preference for the specified user
     * 
     * @param uuid
     *            the name of the preference
     * @return a preference value
     * @throws PreferenceManagementException
     */
    public Integer getPreferenceValueAsInteger(String uuid) throws PreferenceManagementException;

    /**
     * Get a Boolean preference for the specified user
     * 
     * @param uuid
     *            the name of the preference
     * @return a preference value
     * @throws PreferenceManagementException
     */
    public Boolean getPreferenceValueAsBoolean(String uuid) throws PreferenceManagementException;

    /**
     * Get a Boolean preference for the specified user
     * 
     * @param uuid
     *            the name of the preference
     * @return a preference value
     * @throws PreferenceManagementException
     */
    public BigDecimal getPreferenceValueAsDecimal(String uuid) throws PreferenceManagementException;

    /**
     * Get a Date preference for the specified user
     * 
     * @param uuid
     *            the name of the preference
     * @return a preference value
     * @throws PreferenceManagementException
     */
    public Date getPreferenceValueAsDate(String uuid) throws PreferenceManagementException;

    /**
     * Update the value for a preference
     * 
     * @param uuid
     *            the name of the preference
     * @param value
     *            a preference value (compatible with this preference)
     * @return a preference value
     * @throws PreferenceManagementException
     */
    public void updatePreferenceValue(String uuid, Object value) throws PreferenceManagementException;

    public String getPropertyAsString(String uuid, String propertyKey) throws PreferenceManagementException;

    public Boolean getPropertyAsBoolean(String uuid, String propertyKey) throws PreferenceManagementException;

    public boolean isPreferenceSystem(String uuid);

}
