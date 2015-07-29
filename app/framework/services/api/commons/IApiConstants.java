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
package framework.services.api.commons;

/**
 * Constants for the APIs
 * 
 * @author Pierre-Yves Cloux
 *
 */
public interface IApiConstants {
    /**
     * The name of the HTTP header which should contains the Epoch timestamp for
     * an API call
     */
    public static final String TIMESTAMP_HEADER = "X-bizdock-timestamp";

    /**
     * The name of the HTTP header which should contains the application key
     */
    public static final String APPLICATION_KEY_HEADER = "X-bizdock-application";

    /**
     * The name of the HTTP header which should contains the signature
     */
    public static final String SIGNATURE_HEADER = "X-bizdock-signature";
}
