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
package framework.services.api.server;

import java.util.List;

import framework.services.api.commons.ApiMethod;
import framework.services.api.commons.ApiSignatureException;

/**
 * Interface for the API signature service
 * 
 * @author Pierre-Yves Cloux
 */
public interface IApiSignatureService {

    /**
     * Application which is created for eChannel administration purpose.<br/>
     * This application cannot be deleted by the end user.
     */
    public static final String ROOT_APPLICATION = "_root";

    /**
     * Any line in the API authorizations starting with this character will be
     * ignored
     */
    public static final String IGNORE_LINE_CHARACTER = "#";

    /**
     * Default API authorization to be used when creation a new API registration
     */
    public static final String DEFAULT_AUTHORIZATION = "GET (.*)\n#POST (.*)\n#PUT (.*)\n#DELETE (.*)";

    /**
     * Creates or overwrite an application configuration.<br/>
     * If the registration is new some application/secret keys are generated. If
     * the registration exists, the authorization is updated (keys are kept
     * "as is").
     * 
     * @param applicationName
     *            a unique application name
     * @param description
     *            the description of the application
     * @param testable
     *            true if the application can be tested (using the API
     *            simulator)
     * @param isDisplay
     *            true if the key is displayed in BizDock
     * @param apiAuthorization
     *            some application authorizations
     * @return the generated {@link ApiApplicationConfiguration} object
     */
    public IApiApplicationConfiguration setApplicationConfiguration(String applicationName, String description, boolean testable, boolean isDisplayed,
            String apiAuthorization) throws ApiSignatureException;

    /**
     * Generate a new set of keys (application/secret) for the specified
     * application
     * 
     * @param applicationName
     *            a unique application name
     * @return the generated {@link ApiApplicationConfiguration} object
     * @throws ApiSignatureException
     */
    public IApiApplicationConfiguration resetApplicationConfigurationKeys(String applicationName) throws ApiSignatureException;

    /**
     * Change the name of an application configuration
     * 
     * @param oldApplicationName
     *            the former application name
     * @param newApplicationName
     *            the new application name
     * @return the generated {@link ApiApplicationConfiguration} object
     * @throws ApiSignatureException
     */
    public IApiApplicationConfiguration changeApplicationConfigurationName(String oldApplicationName, String newApplicationName) throws ApiSignatureException;

    /**
     * List the registered application configurations.<br/>
     * <b>WARNING</b> : the ROOT_APPLICATION is excluded from this list
     */
    public List<IApiApplicationConfiguration> listAuthorizedApplications() throws ApiSignatureException;

    /**
     * List the registered application configurations.<br/>
     * <b>WARNING</b> : the ROOT_APPLICATION is excluded from this list
     */
    public List<IApiApplicationConfiguration> listAuthorizedAndTestableApplications() throws ApiSignatureException;

    /**
     * Delete an application configuration
     * 
     * @param applicationName
     *            a unique application name
     */
    public void deleteApplicationConfiguration(String applicationName) throws ApiSignatureException;

    /**
     * Return the application configuration associated with the specified name
     * 
     * @param applicationKey
     *            an application key
     */
    public IApiApplicationConfiguration getApplicationConfigurationFromApplicationKey(String applicationKey) throws ApiSignatureException;

    /**
     * Return the application configuration associated with the specified name
     * 
     * @param applicationName
     *            a unique application name
     */
    public IApiApplicationConfiguration getApplicationConfigurationFromApplicationName(String applicationName) throws ApiSignatureException;

    /**
     * Return true if the specified application name exists
     * 
     * @param applicationName
     *            a unique application name
     * @return
     */
    public boolean isApplicationNameExists(String applicationName);

    /**
     * Check the API signature
     * 
     * @param applicationKey
     *            the application key provided along with the request
     * @param signature
     *            the API signature
     * @param method
     *            the method of the API call
     * @param path
     *            the called URL path (example: /api/shmock)
     * @param body
     *            the body of the request
     * @param timeStamp
     *            the timestamp provided as part of the request
     * @param allowTimeDifference
     *            if true the time difference is not checked (WARNING : this
     *            removes the replay protection.
     * @throws ApiSignatureException
     *             an exception is thrown if the signature is not valid
     */
    public void checkApiSignature(String applicationKey, byte[] signature, ApiMethod method, String path, byte[] body, long timeStamp,
            boolean allowTimeDifference) throws ApiSignatureException;

    /**
     * Check if this API is authorized without using a signature (only the
     * application key).<br/>
     * <b>WARNING: such configuration is low security.</b>
     * 
     * @param applicationKey
     *            the application key provided along with the request
     * @param method
     *            the method of the API call
     * @param path
     *            the called URL path (example: /api/shmock)
     * @throws ApiSignatureException
     *             an exception is thrown if the signature is not valid
     */
    public void checkApiAuthorizations(String applicationKey, ApiMethod method, String path) throws ApiSignatureException;

}