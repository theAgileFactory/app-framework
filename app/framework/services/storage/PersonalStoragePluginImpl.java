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
package framework.services.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;

import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import scala.concurrent.duration.Duration;
import akka.actor.Cancellable;
import framework.services.account.AccountManagementException;
import framework.services.account.IAccountManagerPlugin;
import framework.services.database.IDatabaseDependencyService;
import framework.services.system.ISysAdminUtils;
import framework.utils.Utilities;

/**
 * The default implementation for the personal storage plugin.<br/>
 * This one makes use of {@link IAccountManagerPlugin} in order to check the uid
 * provided as parameters.
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class PersonalStoragePluginImpl implements IPersonalStoragePlugin {
    private static Logger.ALogger log = Logger.of(PersonalStoragePluginImpl.class);
    private IAccountManagerPlugin accountManagerPlugin;
    private Cancellable scheduler;
    private File personalStorageRootFolder;
    private int personalStorageCleanupFrequency;

    public enum Config {
        PLAY_PERSONAL_STORAGE_ROOT_CONFIG_PARAMETER("maf.personal.space.root"), PLAY_PERSONAL_STORAGE_CLEANUP_FREQUENCY_PARAMETER(
                "maf.personal.space.cleanup.frequency");

        private String configurationKey;

        private Config(String configurationKey) {
            this.configurationKey = configurationKey;
        }

        public String getConfigurationKey() {
            return configurationKey;
        }
    }

    /**
     * Create a new PersonalStoragePluginImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param accountManagerPlugin
     *            the account manager plugin service
     * @param sysAdminUtils
     * @param databaseDependencyService
     */
    @Inject
    public PersonalStoragePluginImpl(ApplicationLifecycle lifecycle, Configuration configuration, IAccountManagerPlugin accountManagerPlugin,
            ISysAdminUtils sysAdminUtils, IDatabaseDependencyService databaseDependencyService) {
        log.info("SERVICE>>> PersonalStoragePluginImpl starting...");
        this.personalStorageRootFolder = new File(configuration.getString(Config.PLAY_PERSONAL_STORAGE_ROOT_CONFIG_PARAMETER.getConfigurationKey()));
        this.personalStorageCleanupFrequency = configuration.getInt(Config.PLAY_PERSONAL_STORAGE_CLEANUP_FREQUENCY_PARAMETER.getConfigurationKey());
        this.accountManagerPlugin = accountManagerPlugin;
        this.scheduler = sysAdminUtils.scheduleRecurring(true, "PersonalStorage", Duration.create(0, TimeUnit.MILLISECONDS),
                Duration.create(getPersonalStorageCleanupFrequency(), TimeUnit.HOURS), new Runnable() {
                    @Override
                    public void run() {
                        String uuid = UUID.randomUUID().toString();
                        log.info(String.format("Cleanup of the personal storage started with %s", uuid));
                        cleanup();
                        log.info(String.format("Cleanup of the personal storage completed with %s", uuid));
                    }
                });
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> PersonalStoragePluginImpl stopping...");
            destroy();
            log.info("SERVICE>>> PersonalStoragePluginImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> PersonalStoragePluginImpl started");
    }

    /**
     * Method to be registered at bean context to destroy the bean when the
     * context is closed
     */
    private void destroy() {
        if (getScheduler() != null) {
            getScheduler().cancel();
        }
    }

    /**
     * Analyze recursively the content of the folder and remove the "old" files.
     */
    private void cleanup() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR, -getPersonalStorageCleanupFrequency());
        recursiveCleanup(getPersonalStorageRootFolder(), calendar.getTimeInMillis());
    }

    /**
     * Delete all the files which are older than the specified time
     * 
     * @param directory
     *            a folder
     * @param maxLastModified
     *            a time in millisecond
     */
    private synchronized void recursiveCleanup(File directory, long maxLastModified) {
        for (File file : directory.listFiles()) {
            if (file.exists() && file.isDirectory()) {
                recursiveCleanup(file, maxLastModified);
            } else {
                if (file.exists() && file.lastModified() < maxLastModified) {
                    if (!file.delete()) {
                        log.error(String.format("Cleanup of the personal storage: unable to delete the file %s", file.getAbsolutePath()));
                    } else {
                        log.info(String.format("Cleanup of the personal storage: file %s deleted", file.getAbsolutePath()));
                    }
                }
            }
        }
    }

    @Override
    public File[] getContentView(String uid) throws IOException {
        File personalFolder = new File(getPersonalStorageRootFolder(), getPersonalStorageFolderFromUid(uid));
        createPersonalFolderIfNotExists(uid, personalFolder);
        return personalFolder.listFiles();
    }

    @Override
    public InputStream readFile(String uid, String name) throws IOException {
        File personalFolder = new File(getPersonalStorageRootFolder(), getPersonalStorageFolderFromUid(uid));
        createPersonalFolderIfNotExists(uid, personalFolder);
        File fileToRead = new File(personalFolder, name);
        if (!fileToRead.exists()) {
            return null;
        }
        return new FileInputStream(fileToRead);
    }

    @Override
    public OutputStream createNewFile(String uid, String name) throws IOException {
        File personalFolder = new File(getPersonalStorageRootFolder(), getPersonalStorageFolderFromUid(uid));
        createPersonalFolderIfNotExists(uid, personalFolder);
        File newFileToCreate = new File(personalFolder, name);
        if (newFileToCreate.exists()) {
            if (!newFileToCreate.canWrite()) {
                String errorMessage = String.format(
                        "Unable to write a file (named %s) to the personal storage for %s (file already exists and cannot be overwritten)", name, uid);
                log.error(errorMessage);
                throw new IOException(errorMessage);
            }
        }
        return new FileOutputStream(newFileToCreate);
    }

    @Override
    public void deleteFile(String uid, String name) throws IOException {
        File personalFolder = new File(getPersonalStorageRootFolder(), getPersonalStorageFolderFromUid(uid));
        createPersonalFolderIfNotExists(uid, personalFolder);
        File fileToDelete = new File(personalFolder, name);
        if (!fileToDelete.exists()) {
            throw new IOException("Unknow file " + name + " in personal space of " + uid);
        }
        boolean isDeleted = fileToDelete.delete();
        if (!isDeleted) {
            throw new IOException("Unable to delete the file " + name + " in personal space of " + uid);
        }
    }

    @Override
    public void moveFile(String uid, File absoluteSourceFilePath, String name) throws IOException {
        // Check if the file to move exists
        if (!absoluteSourceFilePath.isFile() || !absoluteSourceFilePath.exists()) {
            throw new IOException(String.format("The file %s does not exists", absoluteSourceFilePath.getAbsolutePath()));
        }
        File personalFolder = new File(getPersonalStorageRootFolder(), getPersonalStorageFolderFromUid(uid));
        createPersonalFolderIfNotExists(uid, personalFolder);
        File targetFile = new File(personalFolder, name);
        if (targetFile.exists()) {
            if (!targetFile.canWrite()) {
                String errorMessage = String.format(
                        "Unable to move a file (named %s) to the personal storage for %s (file already exists and cannot be overwritten)", name, uid);
                log.error(errorMessage);
                throw new IOException(errorMessage);
            }
        }
        FileUtils.copyFile(absoluteSourceFilePath, targetFile);
    }

    @Override
    public Long getSize() {
        return Utilities.folderSize(this.getPersonalStorageRootFolder());
    }

    /**
     * Create the personal folder for the specified user if this one does not
     * exists
     * 
     * @param uid
     *            the user id of a named user
     * @param personalFolder
     *            a personal folder
     * @throws IOException
     */
    private void createPersonalFolderIfNotExists(String uid, File personalFolder) throws IOException {
        if (!personalFolder.exists()) {
            if (!personalFolder.mkdir()) {
                String errorMessage = String.format("Unable to create the personal storage for %s (folder creation failed)", uid);
                log.error(errorMessage);
                throw new IOException(errorMessage);
            }
        }
    }

    /**
     * Creates a folder name from the specified uid
     * 
     * @param uid
     *            a unique user id
     * @return a folder name
     * @throws IOException
     */
    private String getPersonalStorageFolderFromUid(String uid) throws IOException {
        if (uid == null) {
            throw new IOException("Invalid uid : blank or empty");
        }
        try {
            if (!getAccountManagerPlugin().isUserIdExists(uid)) {
                throw new IOException(String.format("The specified uid [%s] does not exists", uid));
            }
        } catch (AccountManagementException e) {
            String errorMessage = String.format("Unexpected error while checking the uid : %s", uid);
            log.error(errorMessage, e);
            throw new IOException(errorMessage, e);
        }
        return uid.replaceAll("/", "").replaceAll("\\.", "");
    }

    private File getPersonalStorageRootFolder() {
        return personalStorageRootFolder;
    }

    private int getPersonalStorageCleanupFrequency() {
        return personalStorageCleanupFrequency;
    }

    private IAccountManagerPlugin getAccountManagerPlugin() {
        return accountManagerPlugin;
    }

    private Cancellable getScheduler() {
        return scheduler;
    }

}
