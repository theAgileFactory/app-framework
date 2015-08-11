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

import javax.inject.Inject;
import javax.inject.Singleton;

import models.framework_models.account.Credential;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import framework.services.database.IDatabaseDependencyService;

/**
 * An authentication account reader plugin based on the {@link Credential}
 * object.<br/>
 * This one is to be used to the light PPM only version of BizDock
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class LightAuthenticationAccountReaderPlugin implements IAuthenticationAccountReaderPlugin {
    private static Logger.ALogger log = Logger.of(LightAuthenticationAccountReaderPlugin.class);

    /**
     * Create a new LightAuthenticationAccountReaderPlugin
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param databaseDependencyService
     */
    @Inject
    public LightAuthenticationAccountReaderPlugin(ApplicationLifecycle lifecycle, Configuration configuration,
            IDatabaseDependencyService databaseDependencyService) {
        log.info("SERVICE>>> LightAuthenticationAccountReaderPlugin starting...");
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> LightAuthenticationAccountReaderPlugin stopping...");
            log.info("SERVICE>>> LightAuthenticationAccountReaderPlugin stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> LightAuthenticationAccountReaderPlugin started");
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
