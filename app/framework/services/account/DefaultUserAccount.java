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

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

/**
 * The implementation of the {@link IUserAccount} interface.<br/>
 * The {@link IUserAccount} holds all the authentication & authorization
 * information for a user.
 * 
 * @author Pierre-Yves Cloux
 */
public class DefaultUserAccount extends AbstractDefaultCommonUserAccount implements ICommonUserAccount {
    public DefaultUserAccount() {
        super();
    }

    /**
     * Get the user profile attributes from the LDAP {@link Attributes}
     * structure
     * 
     * @param userUniqueIdAttributeName
     *            the name of the attribute that contains the user unique id
     * @param attributes
     *            the LDAP attributes to be used to fill the user account
     * @param isActive
     *            true if the user is active
     * @throws NamingException
     */
    void fill(String userUniqueIdAttributeName, Attributes attributes, boolean isActive) throws NamingException {
        this.firstName = attributes.get("givenname") == null ? "" : (String) attributes.get("givenname").get(0);
        this.lastName = attributes.get("sn") == null ? "" : (String) attributes.get("sn").get(0);
        this.mail = attributes.get("mail") == null ? "" : (String) attributes.get("mail").get(0);
        this.uid = attributes.get(userUniqueIdAttributeName) == null ? "" : (String) attributes.get(userUniqueIdAttributeName).get(0);
        this.isActive = isActive;
    }
}
