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

import models.framework_models.account.Credential;

/**
 * A {@link IUserAccount} implementation which is based on the
 * {@link Credential} object and is to be used for the light standalone BizDock
 * implementation.
 * 
 * @author Pierre-Yves Cloux
 */
public class LightUserAccount extends AbstractDefaultCommonUserAccount implements ICommonUserAccount {

    public LightUserAccount() {
    }
}
