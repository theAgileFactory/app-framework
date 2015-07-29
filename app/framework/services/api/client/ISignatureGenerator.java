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
package framework.services.api.client;

import java.security.MessageDigest;

import org.apache.commons.lang3.tuple.Pair;

import framework.services.api.commons.ApiMethod;
import framework.services.api.commons.ApiSignatureException;

/**
 * Interface to be implemented by the class which is able to generate a
 * signature
 * 
 * @author Pierre-Yves Cloux
 */
public interface ISignatureGenerator {

    /**
     * Create a signature for a request
     * 
     * @param method
     *            the API method
     * @param url
     *            the URL called
     * @param body
     *            the request body
     * @param timeStamp
     *            a timestamp
     * @return a {@link Pair} which contains the signature and the timestamp
     */
    public abstract byte[] getRequestSignature(ApiMethod method, String url, byte[] body, long timeStamp) throws ApiSignatureException;

    /**
     * Create a signature for a request.<br/>
     * The timestamp is automatically computed applying any specified time
     * correction.
     * 
     * @param method
     *            the API method
     * @param url
     *            the URL called
     * @param body
     *            the request body
     * @return a {@link Pair} which contains the signature and the timestamp
     */
    public abstract Pair<byte[], Long> getRequestSignature(ApiMethod method, String url, byte[] body) throws ApiSignatureException;

    /**
     * Return the time correction to be applied when generating the signature
     */
    public abstract long getTimeCorrection();

    /**
     * Return the shared secret configured for this signature generator
     */
    public abstract String getSharedSecret();

    /**
     * Return the application key configured for this signature generator
     */
    public abstract String getApplicationKey();

    /**
     * Return the version of the BizDock signature algorithm
     */
    public abstract int getProtocolVersion();

    /**
     * Return the name of hash algorithm to be used (see {@link MessageDigest})
     */
    public abstract String getHashAlgorithm();

}