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
 * Generic exception for API signature management
 * 
 * @author Pierre-Yves Cloux
 */
public class ApiSignatureException extends Exception {
    private static final long serialVersionUID = -1045038905575563074L;

    public ApiSignatureException() {
    }

    public ApiSignatureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ApiSignatureException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiSignatureException(String message) {
        super(message);
    }

    public ApiSignatureException(Throwable cause) {
        super(cause);
    }
}
