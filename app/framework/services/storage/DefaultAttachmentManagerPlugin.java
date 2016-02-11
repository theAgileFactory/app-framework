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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.database.IDatabaseDependencyService;
import framework.utils.FileAttachmentHelper;
import framework.utils.Utilities;
import models.framework_models.common.Attachment;
import models.framework_models.common.StructuredDocument;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;

/**
 * Default implementation for the {@link IAttachmentManagerPlugin} interface.
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class DefaultAttachmentManagerPlugin implements IAttachmentManagerPlugin {
    private static Logger.ALogger log = Logger.of(DefaultAttachmentManagerPlugin.class);
    /**
     * The directory in which the attachments are stored
     */
    private String attachmentRootDirectoryPath;
    private IImplementationDefinedObjectService implementationDefinedObjectService;

    public enum Config {
        ATTACHMENT_ROOT("maf.attachments.root");

        private String configurationKey;

        private Config(String configurationKey) {
            this.configurationKey = configurationKey;
        }

        public String getConfigurationKey() {
            return configurationKey;
        }
    }

    /**
     * Creates a new attachment manager.
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param databaseDependencyService
     */
    @Inject
    public DefaultAttachmentManagerPlugin(ApplicationLifecycle lifecycle, Configuration configuration, IDatabaseDependencyService databaseDependencyService,
            IImplementationDefinedObjectService implementationDefinedObjectService) {
        log.info("SERVICE>>> DefaultAttachmentManagerPlugin starting...");
        this.implementationDefinedObjectService = implementationDefinedObjectService;
        this.attachmentRootDirectoryPath = configuration.getString(Config.ATTACHMENT_ROOT.getConfigurationKey());
        File attachmentDirectory = new File(attachmentRootDirectoryPath);
        if (!attachmentDirectory.exists() || !attachmentDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid attachments directory: " + attachmentRootDirectoryPath);
        }
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> DefaultAttachmentManagerPlugin stopping...");
            log.info("SERVICE>>> DefaultAttachmentManagerPlugin stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> DefaultAttachmentManagerPlugin started");
    }

    @Override
    public Long addUrlAttachment(String url, String name, Class<?> objectType, Long objectId) throws IOException {

        if (objectType == null || objectId == null) {
            throw new IllegalArgumentException("objectType cannot be null");
        }

        Attachment attachment = new Attachment();
        attachment.defaults();
        attachment.mimeType = FileAttachmentHelper.FileType.URL.name();
        attachment.name = name;
        attachment.objectId = objectId;
        attachment.objectType = objectType.getName();
        attachment.path = url;

        attachment.save();

        return attachment.id;
    }

    @Override
    public Long addFileAttachment(InputStream inputStream, String mimeType, String name, Class<?> objectType, Long objectId) throws IOException {
        if (objectType == null || objectId == null) {
            throw new IllegalArgumentException("objectType cannot be null");
        }
        String uuid = UUID.randomUUID().toString();
        File fileAttachment = new File(getAttachmentRootDirectoryPath(), uuid);
        if (fileAttachment.exists()) {
            throw new IOException(String.format("The file %s already exists, this is unexpected", fileAttachment.getAbsolutePath()));
        }
        FileOutputStream fOut = null;
        try {
            // Write the file on the file system
            fOut = new FileOutputStream(fileAttachment);
            IOUtils.copy(inputStream, fOut);

            // Create the Attachment record
            Attachment attachment = new Attachment();
            attachment.defaults();
            attachment.mimeType = mimeType;
            attachment.name = name;
            attachment.objectId = objectId;
            attachment.objectType = objectType.getName();
            attachment.path = uuid;

            attachment.save();

            return attachment.id;
        } catch (Exception e) {
            String errorMessage = String.format("Exception while writing the file %s [objectId=%s, objectType=%s]", fileAttachment.getAbsolutePath(),
                    String.valueOf(objectId), objectType);
            log.error(errorMessage, e);
            throw new IOException(errorMessage, e);
        } finally {
            IOUtils.closeQuietly(fOut);
        }
    }

    @Override
    public Long addStructuredDocumentAttachment(InputStream inputStream, String mimeType, String name, Class<?> objectType, Long objectId)
            throws IOException {
        return addStructuredDocumentAttachment(IOUtils.toByteArray(inputStream), mimeType, name, objectType, objectId);
    }

    @Override
    public Long addStructuredDocumentAttachment(Object objectToStore, String mimeType, String name, Class<?> objectType, Long objectId) throws IOException {
        return addStructuredDocumentAttachment(Utilities.marshallObject(objectToStore), mimeType, name, objectType, objectId);
    }

    private Long addStructuredDocumentAttachment(byte[] data, String mimeType, String name, Class<?> objectType, Long objectId) throws IOException {
        if (objectType == null || objectId == null) {
            throw new IllegalArgumentException("objectType cannot be null");
        }
        try {
            // Structured document
            StructuredDocument structuredDoc = new StructuredDocument();
            structuredDoc.defaults();
            structuredDoc.content = data;
            structuredDoc.save();

            // Create the Attachment record
            Attachment attachment = new Attachment();
            attachment.defaults();
            attachment.mimeType = mimeType;
            attachment.name = name;
            attachment.objectId = objectId;
            attachment.objectType = objectType.getName();
            attachment.structuredDocument = structuredDoc;
            attachment.save();

            return attachment.id;
        } catch (Exception e) {
            String errorMessage = String.format("Exception while storing a structured document %s [objectId=%s, objectType=%s]", name,
                    String.valueOf(objectId), objectType);
            log.error(errorMessage, e);
            throw new IOException(errorMessage, e);
        }
    }

    @Override
    public Long createLink(Long existingAttachmentId, Class<?> objectType, Long objectId) throws IOException {
        return moveOrLinkAttachment(existingAttachmentId, objectType, objectId, false);
    }

    @Override
    public Long moveAttachment(Long existingAttachmentId, Class<?> objectType, Long objectId) throws IOException {
        return moveOrLinkAttachment(existingAttachmentId, objectType, objectId, true);
    }

    @Override
    public InputStream getAttachmentContent(Long attachmentId) throws IOException {
        Attachment existingAttachment = Attachment.getAttachmentFromId(attachmentId);
        if (existingAttachment == null) {
            throw new IOException(String.format("Unknow object %s cannot create a link", String.valueOf(attachmentId)));
        }
        log.debug(String.format("Existing attachment to link %s found", String.valueOf(attachmentId)));
        if (existingAttachment.isFile()) {
            File fileAttachment = getFileFromAttachment(existingAttachment);
            return new FileInputStream(fileAttachment);
        }
        // This is a structured document
        if (existingAttachment.structuredDocument == null || existingAttachment.structuredDocument.content == null) {
            throw new IOException(String.format("Attachment object %s is not linked to a file nor a structured document", String.valueOf(attachmentId)));
        }
        return new ByteArrayInputStream(existingAttachment.structuredDocument.content);
    }

    @Override
    public Object getStructuredDocumentAttachmentContent(Long attachmentId) throws IOException {
        Attachment existingAttachment = Attachment.getAttachmentFromId(attachmentId);
        if (existingAttachment == null) {
            throw new IOException(String.format("Unknow object %s cannot create a link", String.valueOf(attachmentId)));
        }
        log.debug(String.format("Existing attachment to link %s found", String.valueOf(attachmentId)));
        if (existingAttachment.isFile()) {
            throw new IllegalArgumentException(String.format("The attachment must be a structured data attachment %s", String.valueOf(attachmentId)));
        }
        if (existingAttachment.structuredDocument == null || existingAttachment.structuredDocument.content == null) {
            throw new IOException(String.format("Attachment object %s is not linked to a file nor a structured document", String.valueOf(attachmentId)));
        }
        return Utilities.unmarshallObject(existingAttachment.structuredDocument.content);
    }

    @Override
    public Attachment getAttachmentFromId(Long id) {
        return Attachment.getAttachmentFromId(id);
    }

    @Override
    public List<Attachment> getAttachmentsFromObjectTypeAndObjectId(Class<?> objectType, Long objectId) {
        return Attachment.getAttachmentsFromObjectTypeAndObjectId(objectType, objectId);
    }

    @Override
    public List<Attachment> getAttachmentsFromObjectTypeAndObjectId(Class<?> objectType, Long objectId, boolean structuredDocument) {
        return Attachment.getAttachmentsFromObjectTypeAndObjectId(objectType, objectId, structuredDocument);
    }

    @Override
    public void deleteAttachment(Long attachmentId) throws IOException {
        Attachment existingAttachment = Attachment.getAttachmentFromId(attachmentId);
        if (existingAttachment == null) {
            throw new IOException(String.format("Unknow object %s cannot create a link", String.valueOf(attachmentId)));
        }
        log.debug(String.format("Existing attachment to link %s found", String.valueOf(attachmentId)));

        try {
            if (existingAttachment.isFile()) {
                int fileAttachmentCount = Attachment.getNumberOfAttachments(existingAttachment.path);
                if (fileAttachmentCount == 1) {
                    // Only one attachment is referencing this file, it can be
                    // deleted
                    File fileAttachment = getFileFromAttachment(existingAttachment);
                    log.info(String.format("Deleting the file %s", fileAttachment.getAbsolutePath()));
                    if (!fileAttachment.delete()) {
                        log.error(String.format("Unable to delete the file %s", fileAttachment.getAbsolutePath()));
                    }
                }
            } else {
                if (existingAttachment.structuredDocument == null || existingAttachment.structuredDocument.content == null) {
                    throw new IOException(
                            String.format("Attachment object %s is not linked to a file nor a structured document", String.valueOf(attachmentId)));
                }
                int structuredAttachmentCount = Attachment.getNumberOfAttachments(existingAttachment.structuredDocument.id);
                if (structuredAttachmentCount == 1) {
                    // Only one attachment, it can be deleted
                    existingAttachment.structuredDocument.doDelete();
                }
            }

            existingAttachment.doDelete();

        } catch (Exception e) {
            String errorMessage = String.format("Exception while deleting an existing attachment %s", String.valueOf(attachmentId));
            log.error(errorMessage, e);
            throw new IOException(errorMessage, e);
        }
    }

    @Override
    public void updateAttachment(Long attachmentId, InputStream inputStream) throws IOException {
        Attachment existingAttachment = Attachment.getAttachmentFromId(attachmentId);
        if (existingAttachment == null) {
            throw new IOException(String.format("Unknow object %s cannot create a link", String.valueOf(attachmentId)));
        }
        log.debug(String.format("Existing attachment to link %s found", String.valueOf(attachmentId)));
        if (existingAttachment.isFile()) {
            File fileAttachment = getFileFromAttachment(existingAttachment);
            FileUtils.copyInputStreamToFile(inputStream, fileAttachment);
        } else {
            if (existingAttachment.structuredDocument == null || existingAttachment.structuredDocument.content == null) {
                throw new IOException(String.format("Attachment object %s is not linked to a file nor a structured document", String.valueOf(attachmentId)));
            }
            existingAttachment.structuredDocument.content = IOUtils.toByteArray(inputStream);
        }
    }

    @Override
    public Long getSize() {
        File root = new java.io.File(this.getAttachmentRootDirectoryPath());
        return Utilities.folderSize(root);
    }

    @Override
    public String getAjaxWaitImageUrl() {
        return this.getImplementationDefinedObjectService().getRouteForAjaxWaitImage().url();
    }

    @Override
    public String getAttachmentDownloadUrl(Long attachmentId) {
        return this.getImplementationDefinedObjectService().getRouteForDownloadAttachedFile(attachmentId).url();
    }

    @Override
    public String getAttachmentDeleteUrl(Long attachmentId) {
        return this.getImplementationDefinedObjectService().getRouteForDeleteAttachedFile(attachmentId).url();
    }

    private Long moveOrLinkAttachment(Long existingAttachmentId, Class<?> objectType, Long objectId, boolean move) throws IOException {
        Attachment existingAttachment = Attachment.getAttachmentFromId(existingAttachmentId);
        if (existingAttachment == null) {
            throw new IOException(String.format("Unknow object %s cannot create a link", String.valueOf(existingAttachmentId)));
        }
        log.debug(String.format("Existing attachment to link %s found", String.valueOf(existingAttachmentId)));
        if (existingAttachment.isFile()) {
            getFileFromAttachment(existingAttachment);
        }
        try {
            Attachment linkedAttachment = new Attachment();
            linkedAttachment.defaults();
            linkedAttachment.mimeType = existingAttachment.mimeType;
            linkedAttachment.name = existingAttachment.name;
            linkedAttachment.objectId = objectId;
            linkedAttachment.objectType = objectType.getName();
            linkedAttachment.path = existingAttachment.path;
            linkedAttachment.structuredDocument = existingAttachment.structuredDocument;
            linkedAttachment.save();

            // Delete the initial attachment if this is a move
            if (move) {
                existingAttachment.doDelete();
            }

            return linkedAttachment.id;
        } catch (Exception e) {
            String errorMessage = String.format("Exception while linking or moving an existing attachment %s to a new one [objectId=%s, objectType=%s]",
                    String.valueOf(existingAttachmentId), String.valueOf(objectId), objectType);
            log.error(errorMessage, e);
            throw new IOException(errorMessage, e);
        }
    }

    /**
     * Look for the file linked to the specified attachment
     * 
     * @param attachment
     *            an {@link Attachment}
     * @return a {@link File}
     * @throws IOException
     */
    private File getFileFromAttachment(Attachment attachment) throws IOException {
        File attachmentFile = new File(getAttachmentRootDirectoryPath(), attachment.path);
        if (!attachmentFile.exists() || !attachmentFile.isFile()) {
            throw new IOException(String.format("File %s not found on the file system", attachmentFile.getAbsolutePath()));
        }
        return attachmentFile;
    }

    private String getAttachmentRootDirectoryPath() {
        return attachmentRootDirectoryPath;
    }

    /**
     * Get the implementation defined object service.
     */
    private IImplementationDefinedObjectService getImplementationDefinedObjectService() {
        return this.implementationDefinedObjectService;
    }
}
