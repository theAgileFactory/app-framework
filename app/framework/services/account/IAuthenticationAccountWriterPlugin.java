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

/**
 * The interface to be implemented by the classes which can update the
 * authentication back-end.
 * 
 * @author Pierre-Yves Cloux
 * 
 */
public interface IAuthenticationAccountWriterPlugin {

    /**
     * Create a blank user profile
     * 
     * @param uid
     *            a unique Id
     * @param firstName
     *            the First name of the user
     * @param lastName
     *            the Last name of the user
     * @param mail
     *            a valid e-mail address
     * @param password
     *            the user password
     */
    public void createUserProfile(String uid, String firstName, String lastName, String mail, String password) throws AccountManagementException;

    /**
     * Updates the user profile with the provided attributes Important: only the
     * attributes not null are updated
     * 
     * @param uid
     *            a unique Id
     * @param firstName
     * @param lastName
     * @param mail
     */
    public void updateUserProfile(String uid, String firstName, String lastName, String mail) throws AccountManagementException;

    /**
     * Change the activation status
     * 
     * @param uid
     *            the user account
     * @param isActive
     *            the activation status for the user
     */
    public void changeActivationStatus(String uid, boolean isActive) throws AccountManagementException;

    /**
     * Delete the LDAP entry associated with the specified uid
     * 
     * @param uid
     *            a unique Id
     */
    public void deleteUserProfile(String uid) throws AccountManagementException;

    /**
     * Change the password of the specified user profile
     * 
     * @param userProfile
     *            a user profile
     * @param password
     *            the new password
     */
    public void changePassword(String uid, String password) throws AccountManagementException;

    /**
     * Add the specified user to the specified LDAP group
     * 
     * @param uid
     *            a unique Id
     * @param groupName
     *            the name of group to be modified
     * @throws NamingException
     * @throws AccountManagementException
     */
    public void addUserToGroup(String uid, String groupName) throws AccountManagementException;

    /**
     * Remove the specified user to the specified LDAP group
     * 
     * @param uid
     *            a unique Id
     * @param groupName
     *            the name of group to be modified
     * @throws NamingException
     * @throws AccountManagementException
     */
    public void removeUserFromGroup(String uid, String groupName) throws AccountManagementException;
}
