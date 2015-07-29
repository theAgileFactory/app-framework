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
package framework.services.ext;

/**
 * The default exception for managing an extension
 * 
 * @author Pierre-Yves Cloux
 */
public class ExtensionManagerException extends Exception {
    private static final long serialVersionUID = 1355783831211433779L;

    public ExtensionManagerException() {
    }

    public ExtensionManagerException(String message) {
        super(message);
    }

    public ExtensionManagerException(Throwable cause) {
        super(cause);
    }

    public ExtensionManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExtensionManagerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
