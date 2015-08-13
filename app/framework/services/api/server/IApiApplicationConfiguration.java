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

import framework.services.api.client.ISignatureGenerator;
import framework.services.api.commons.ApiMethod;
import framework.services.api.commons.ApiSignatureException;

/**
 * The interface to be implemented by the object which holds the details of an
 * application (allowed to call some APIs)
 * 
 * @author Pierre-Yves Cloux
 */
public interface IApiApplicationConfiguration {
    /**
     * Check if the URL is compatible with at least one of the application
     * authorizations
     * 
     * @param method
     *            a request method
     * @param url
     *            an URL
     * @throws ApiSignatureException
     */
    public void checkUrl(ApiMethod method, String url) throws ApiSignatureException;

    /**
     * Return a signature generator
     * 
     * @return
     */
    public ISignatureGenerator getSignatureGenerator();

    /**
     * Return the application name
     * 
     * @return
     */
    public String getApplicationName();

    /**
     * Return a description for this application
     * 
     * @return
     */
    public String getDescription();

    /**
     * Return the API authorizations as a String (to be displayed and edited)
     * 
     * @return
     */
    public String getApiAuthorizationsAsString();

    /**
     * Return true if the API can be tested (using the simulator mode)
     * 
     * @return
     */
    public boolean isTestable();

    /**
     * Return true if the application key should be displayed in BizDock.
     */
    public boolean isDisplayed();
}