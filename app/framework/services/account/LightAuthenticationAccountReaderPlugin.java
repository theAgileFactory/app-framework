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

import java.util.ArrayList;
import java.util.List;

import models.framework_models.account.Credential;

/**
 * An authentication account reader plugin based on the {@link Credential}
 * object.<br/>
 * This one is to be used to the light PPM only version of BizDock
 * 
 * @author Pierre-Yves Cloux
 */
public class LightAuthenticationAccountReaderPlugin implements IAuthenticationAccountReaderPlugin {

    public LightAuthenticationAccountReaderPlugin() {
    }

    @Override
    public IUserAuthenticationAccount getAccountFromUid(String uid) throws AccountManagementException {
        Credential credential = Credential.getCredentialFromUid(uid);
        if (credential == null)
            return null;
        LightUserAccount lightUserAccount = new LightUserAccount();
        lightUserAccount.fill(credential);
        return lightUserAccount;
    }

    @Override
    public IUserAuthenticationAccount getAccountFromEmail(String mail) throws AccountManagementException {
        Credential credential = Credential.getCredentialFromEmail(mail);
        if (credential == null)
            return null;
        LightUserAccount lightUserAccount = new LightUserAccount();
        lightUserAccount.fill(credential);
        return lightUserAccount;
    }

    @Override
    public List<IUserAuthenticationAccount> getAccountsFromName(String nameCriteria) throws AccountManagementException {
        List<Credential> credentials = Credential.getCredentialsFromName(nameCriteria);
        List<IUserAuthenticationAccount> accounts = new ArrayList<IUserAuthenticationAccount>();
        if (credentials != null) {
            for (Credential credential : credentials) {
                LightUserAccount lightUserAccount = new LightUserAccount();
                lightUserAccount.fill(credential);
                accounts.add(lightUserAccount);
            }
        }
        return accounts;
    }

    @Override
    public boolean isMailAlreadyExist(String mail) throws AccountManagementException {
        return Credential.isCredentialMailAlreadyExist(mail);
    }

    @Override
    public boolean isUidAlreadyExist(String uid) throws AccountManagementException {
        return Credential.isCredentialUidAlreadyExist(uid);
    }

    @Override
    public boolean checkPassword(String uid, String password) throws AccountManagementException {
        return Credential.checkCredentialPassword(uid, password);
    }

}
