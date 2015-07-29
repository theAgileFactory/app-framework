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
package framework.services.session;

import play.mvc.Http;

/**
 * The interface to be implemented by the plugin that manages the user session
 * and the integration with the authentication system.
 * 
 * @author Pierre-Yves Cloux
 */
public interface IUserSessionManagerPlugin {
    public static final String NAME = "userSessionManagerPlugin";

    /**
     * Get the unique user Id from the current session.
     * 
     * @param ctx
     * @return a String or null if no session can be found
     */
    public String getUserSessionId(Http.Context ctx);

    /**
     * Set the name of the attribute to be used to retrieve the uid of the user
     * in the underlying authentication system
     * 
     * @param profileAttributeName
     *            name of a user profile attribute
     */
    public void setUserProfileAttributeName(String profileAttributeName);
}
