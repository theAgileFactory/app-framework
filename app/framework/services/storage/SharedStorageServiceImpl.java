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
import java.util.Arrays;
import java.util.Comparator;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import framework.services.database.IDatabaseDependencyService;
import framework.utils.Utilities;

/**
 * The default implementation of the shared storage system.
 * 
 * @author Pierre-Yves Cloux
 * 
 */
@Singleton
public class SharedStorageServiceImpl implements ISharedStorageService {
    private static Logger.ALogger log = Logger.of(SharedStorageServiceImpl.class);
    private String sharedStorageRootPath;

    public enum Config {
        SFTP_STORE_ROOT("maf.sftp.store.root");

        private String configurationKey;

        private Config(String configurationKey) {
            this.configurationKey = configurationKey;
        }

        public String getConfigurationKey() {
            return configurationKey;
        }
    }

    /**
     * Creates a new SharedStorageServiceImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param databaseDependencyService
     */
    @Inject
    public SharedStorageServiceImpl(ApplicationLifecycle lifecycle, Configuration configuration, IDatabaseDependencyService databaseDependencyService) {
        log.info("SERVICE>>> SharedStorageServiceImpl starting...");
        this.sharedStorageRootPath = configuration.getString(Config.SFTP_STORE_ROOT.getConfigurationKey());
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> SharedStorageServiceImpl stopping...");
            log.info("SERVICE>>> SharedStorageServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> SharedStorageServiceImpl started");
    }

    @Override
    public InputStream getFileAsStream(String filePath) throws IOException {
        return new FileInputStream(getFile(filePath));
    }

    @Override
    public File getFile(String filePath) throws IOException {
        File fileToRead = getFileFromRelativePath(filePath);
        if (!fileToRead.exists()) {
            throw new IOException(String.format("The specified file %s does not exists or in not a file", filePath));
        }
        return fileToRead;
    }

    @Override
    public OutputStream writeFile(String filePath, boolean overwrite) throws IOException {
        File fileToWrite = getFileFromRelativePath(filePath);
        if (fileToWrite.exists() && !overwrite) {
            throw new IOException(String.format("The specified file %s already exists", filePath));
        }
        return new FileOutputStream(fileToWrite);
    }

    @Override
    public void createNewFolder(String directoryPath) throws IOException {
        File fileToWrite = getFileFromRelativePath(directoryPath);
        if (fileToWrite.exists()) {
            throw new IOException(String.format("The specified directory %s already exists", directoryPath));
        }
        boolean isCreated = fileToWrite.mkdir();
        if (!isCreated) {
            throw new IOException(String.format("Unable to create the specified folder", directoryPath));
        }
    }

    @Override
    public void deleteFile(String filePath) throws IOException {
        File fileToDelete = getFileFromRelativePath(filePath);
        if (!fileToDelete.exists() || !fileToDelete.isFile()) {
            throw new IOException(String.format("The specified file %s does not exists or in not a file", filePath));
        }
        if (!fileToDelete.delete()) {
            throw new IOException(String.format("Unable to delete the specified file %s", filePath));
        }
    }

    @Override
    public void deleteFolder(String directoryPath) throws IOException {
        File folderToDelete = getFileFromRelativePath(directoryPath);
        if (!folderToDelete.exists() || !folderToDelete.isDirectory()) {
            throw new IOException(String.format("The specified directory %s does not exists or in not a directory", directoryPath));
        }
        if (!folderToDelete.delete()) {
            throw new IOException(String.format("Unable to delete the specified directory %s", directoryPath));
        }
    }

    @Override
    public String[] getFileList(String directoryPath) throws IOException {
        File directoryToList = getFileFromRelativePath(directoryPath);
        if (!directoryToList.exists() || !directoryToList.isDirectory()) {
            throw new IOException(String.format("The specified directory %s does not exists or in not a directory", directoryPath));
        }
        File[] files = directoryToList.listFiles();
        if (files == null || files.length == 0) {
            return new String[] {};
        }

        // Sort files by last update date then by name
        Utilities.sortFiles(files);

        String[] filesAsString = new String[files.length];
        int count = 0;
        for (File file : files) {
            filesAsString[count++] = (new File(directoryPath, file.getName())).getPath();
        }
        return filesAsString;
    }

    @Override
    public void rename(String sourceFilePath, String targetFilePath) throws IOException {
        File sourceFile = getFileFromRelativePath(sourceFilePath);
        File targetFile = getFileFromRelativePath(targetFilePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            throw new IOException(String.format("The specified source file %s does not exists or in not a file", sourceFilePath));
        }
        if (!targetFile.exists() || !targetFile.isFile()) {
            throw new IOException(String.format("The specified target file %s does not exists or in not a file", targetFilePath));
        }
        if (!sourceFile.getParentFile().equals(targetFile.getParentFile())) {
            throw new IOException(String.format("Cannot rename %s into %s, the two files are not in the same folder", sourceFilePath, targetFilePath));
        }
        if (!sourceFile.renameTo(targetFile)) {
            throw new IOException(String.format("Cannot rename %s into %s, one of the file may be locked", sourceFilePath, targetFilePath));
        }
    }

    @Override
    public void move(String sourceFilePath, String targetFolderPath) throws IOException {
        File sourceFile = getFileFromRelativePath(sourceFilePath);
        File targetFolder = getFileFromRelativePath(targetFolderPath);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            throw new IOException(String.format("The specified source file %s does not exists or in not a file", sourceFilePath));
        }
        if (!targetFolder.exists() || targetFolder.isFile()) {
            throw new IOException(String.format("The specified target folder %s does not exists or in not a folder", targetFolderPath));
        }
        if (!sourceFile.renameTo(new File(targetFolder, sourceFile.getName()))) {
            throw new IOException(String.format("Cannot move %s into %s, the file may be locked", sourceFilePath, targetFolderPath));
        }
    }

    @Override
    public void copy(String sourceFilePath, String targetFilePath) throws IOException {
        File sourceFile = getFileFromRelativePath(sourceFilePath);
        File targetFile = getFileFromRelativePath(targetFilePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            throw new IOException(String.format("The specified source file %s does not exists or in not a file", sourceFilePath));
        }
        if (targetFile.exists()) {
            throw new IOException(String.format("The specified target folder %s already exists or in not a folder", targetFilePath));
        }
        FileUtils.copyFile(sourceFile, targetFile);
    }

    @Override
    public Long getSize() {
        File root = new java.io.File(this.getSharedStorageRootPath());
        return Utilities.folderSize(root);
    }

    /**
     * Return a file relative to the shared storage root path.<br/>
     * 
     * @param fileRelativePath
     *            a relative path
     * @return a file
     * @throws IOException
     */
    private File getFileFromRelativePath(String fileRelativePath) throws IOException {
        if (fileRelativePath == null || StringUtils.isBlank(fileRelativePath)) {
            throw new IOException("Invalid file path : null or blank");
        }
        if (fileRelativePath.contains("..")) {
            throw new IOException(String.format("Invalid file path %s", fileRelativePath));
        }
        File file = new File(getSharedStorageRootPath(), fileRelativePath);
        return file;
    }

    private String getSharedStorageRootPath() {
        return sharedStorageRootPath;
    }

}
