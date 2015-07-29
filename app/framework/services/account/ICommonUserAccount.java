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

import models.framework_models.account.SystemLevelRoleType;
import models.framework_models.account.SystemPermission;
import be.objectify.deadbolt.core.models.Role;

/**
 * The common interface for the "service level" user account management.<br/>
 * This interface is exposing some methods which are not accessible from the
 * {@link IUserAccount} interface
 * 
 * @author Pierre-Yves Cloux
 */
public interface ICommonUserAccount extends IUserAccount {
    /**
     * Fill the current object with the data extracted from the specified
     * IUserAuthenticationAccount
     * 
     * @param userAuthenticationAccount
     *            the user authentication information
     * @param mafUid
     *            unique Id of the account in Maf
     * @param accountType
     *            the user account type
     * @param preferredLanguage
     *            the user preferred language
     * @param markedForDeletion
     *            true if the user is marked for deletion
     * @param isDisplayed
     *            true if the user should be displayed
     */
    public void fill(IUserAuthenticationAccount userAuthenticationAccount, Long mafUid, AccountType accountType, String preferredLanguage,
            boolean markedForDeletion, Boolean isDisplayed);

    /**
     * Set the activation status of the account
     */
    public void setActive(boolean isActive);

    /**
     * Add a role (actually the name of a {@link SystemPermission})
     */
    public void addGroup(Role role);

    /**
     * Add a selectable role (actually the name of a {@link SystemPermission})
     */
    public void addSelectableGroup(Role role);

    /**
     * Add a system level role (actually the name of {@link SystemLevelRoleType}
     */
    public void addSystemLevelRoleType(String systemLevelRoleTypeName);
}
