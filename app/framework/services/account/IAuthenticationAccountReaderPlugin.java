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

import java.util.List;

/**
 * The interface to be used to read the user account information from the
 * <b>authentication</b> back-end.<br/>
 * The only user account information retreived is :
 * <ul>
 * <li>First name</li>
 * <li>Last name</li>
 * <li>Mail</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 * 
 */
public interface IAuthenticationAccountReaderPlugin {
    public static final String NAME = "authenticationAccountReaderPlugin";

    /**
     * Return the {@link IUserAuthenticationAccount} object associated with the
     * specified search key.<br/>
     * The provided user account do not contains any group.
     * 
     * @param uid
     *            the unique identifier for the user provided by the
     *            authentication system
     * @return a IUserAuthenticationAccount object or null if the user is not
     *         found
     * @throws AccountManagementException
     */
    public IUserAuthenticationAccount getAccountFromUid(String uid) throws AccountManagementException;

    /**
     * Return the {@link IUserAuthenticationAccount} object associated with the
     * specified search key.<br/>
     * The provided user account do not contains any group.
     * 
     * @param mail
     *            an e-mail address to be used to find a user
     * @return a IUserAuthenticationAccount object or null if the user is not
     *         found
     * @throws AccountManagementException
     */
    public IUserAuthenticationAccount getAccountFromEmail(String mail) throws AccountManagementException;

    /**
     * Return the {@link IUserAuthenticationAccount} objects associated with the
     * specified search key.<br/>
     * The provided user account do not contains any group.
     * 
     * @param nameCriteria
     *            the name of a user (may contain jokers "*")
     * @return a list of IUserAuthenticationAccount object (empty if no account
     *         are found)
     * @throws AccountManagementException
     */
    public List<IUserAuthenticationAccount> getAccountsFromName(String nameCriteria) throws AccountManagementException;

    /**
     * Check if the email already exist in the authentication system
     * 
     * @param email
     * @return true is the email exist
     * @throws AccountManagementException
     */
    public boolean isMailAlreadyExist(String mail) throws AccountManagementException;

    /**
     * Check if the specified uid already exist in the authentication system
     * 
     * @param uid
     * @return true is the uid exist
     * @throws AccountManagementException
     */
    public boolean isUidAlreadyExist(String uid) throws AccountManagementException;

    /**
     * Perform an authentication using the specified password
     * 
     * @param uid
     * @param password
     *            the user password
     * @throws AccountManagementException
     */
    public boolean checkPassword(String uid, String password) throws AccountManagementException;
}
