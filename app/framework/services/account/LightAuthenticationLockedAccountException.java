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

import org.pac4j.core.exception.CredentialsException;

/**
 * An exception which is raised when an account is locked (in standalone mode)
 * 
 * @author Pierre-Yves Cloux
 */
public class LightAuthenticationLockedAccountException extends CredentialsException {
    private static final long serialVersionUID = -8374868029390161900L;

    public LightAuthenticationLockedAccountException(String message) {
        super(message);
    }

    public LightAuthenticationLockedAccountException(Throwable t) {
        super(t);
    }

}
