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

import javax.inject.Inject;
import javax.inject.Singleton;

import models.framework_models.account.Credential;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import framework.services.database.IDatabaseDependencyService;

/**
 * An implementation of the {@link IAuthenticationAccountWriterPlugin} based on
 * the {@link Credential} object for the standalone version of BizDock.
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class LightAuthenticationAccountWriterPlugin implements IAuthenticationAccountWriterPlugin {
    private static Logger.ALogger log = Logger.of(LightAuthenticationAccountWriterPlugin.class);

    /**
     * Create a new LightAuthenticationAccountWriterPlugin
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param databaseDependencyService
     */
    @Inject
    public LightAuthenticationAccountWriterPlugin(ApplicationLifecycle lifecycle, Configuration configuration,
            IDatabaseDependencyService databaseDependencyService) {
        log.info("SERVICE>>> LightAuthenticationAccountWriterPlugin starting...");
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> LightAuthenticationAccountWriterPlugin stopping...");
            log.info("SERVICE>>> LightAuthenticationAccountWriterPlugin stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> LightAuthenticationAccountWriterPlugin started");
    }

    @Override
    public void createUserProfile(String uid, String firstName, String lastName, String mail, String password) throws AccountManagementException {
        if (Credential.isCredentialUidAlreadyExist(uid) || Credential.isCredentialMailAlreadyExist(mail)) {
            throw new AccountManagementException(String.format("The uid %s already exists, please use another one", uid));
        }
        Credential credential = new Credential();
        credential.uid = uid;
        credential.firstName = firstName;
        credential.lastName = lastName;
        credential.fullName = firstName + " " + lastName;
        credential.mail = mail;
        credential.storePassword(password);
        credential.isActive = true;
        credential.save();
    }

    @Override
    public void updateUserProfile(String uid, String firstName, String lastName, String mail) throws AccountManagementException {
        Credential credential = Credential.getCredentialFromUid(uid);
        if (credential == null) {
            throw new AccountManagementException(String.format("The uid %s does not exists, cannot update", uid));
        }
        if (firstName != null && lastName != null) {
            credential.firstName = firstName;
            credential.lastName = lastName;
            credential.fullName = firstName + " " + lastName;
        }
        if (mail != null) {
            credential.mail = mail;
        }
        credential.save();
    }

    @Override
    public void changeActivationStatus(String uid, boolean isActive) throws AccountManagementException {
        Credential credential = Credential.getCredentialFromUid(uid);
        if (credential == null) {
            throw new AccountManagementException(String.format("The uid %s does not exists, cannot update", uid));
        }
        credential.isActive = isActive;
        credential.failedLogin = 0;
        credential.save();
    }

    @Override
    public void deleteUserProfile(String uid) throws AccountManagementException {
        Credential credential = Credential.getCredentialFromUid(uid);
        if (credential == null) {
            throw new AccountManagementException(String.format("The uid %s does not exists, cannot update", uid));
        }
        credential.delete();
    }

    @Override
    public void changePassword(String uid, String password) throws AccountManagementException {
        Credential credential = Credential.getCredentialFromUid(uid);
        if (credential == null) {
            throw new AccountManagementException(String.format("The uid %s does not exists, cannot update", uid));
        }
        credential.storePassword(password);
        credential.save();
    }

    @Override
    public void addUserToGroup(String uid, String groupName) throws AccountManagementException {
        // No group for this type of storage
    }

    @Override
    public void removeUserFromGroup(String uid, String groupName) throws AccountManagementException {
        // No group for this type of storage
    }
}
