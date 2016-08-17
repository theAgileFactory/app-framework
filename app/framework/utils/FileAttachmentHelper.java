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
package framework.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;

import framework.commons.IFrameworkConstants;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import models.framework_models.common.Attachment;
import play.Logger;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

/**
 * An utility class which manages the extraction of uploaded (one or more) files
 * from a form and the database update (with an {@link Attachment} object.<br/>
 * WARNING: please see the documentation of each method. Some requires an Ebean
 * transaction to be active in order to rollback the operation in case of
 * exception.
 * 
 * @author Pierre-Yves Cloux
 * @author Johann Kohler
 */
@Singleton
public class FileAttachmentHelper {
    private static final int AUTHZ_FILE_DURATION = 1800;

    public static final String FILE_ATTRIBUTE_PREFIX = "_file_field_";

    private static Logger.ALogger log = Logger.of(FileAttachmentHelper.class);

    public static String getFileTypeInputName(String fieldName) {
        return FILE_ATTRIBUTE_PREFIX + fieldName + "_type";
    }

    public static String getFileNameInputName(String fieldName) {
        return FILE_ATTRIBUTE_PREFIX + fieldName + "_name";
    }

    public static String getFileInputName(String fieldName, FileType fileType) {
        return FILE_ATTRIBUTE_PREFIX + fieldName + "_" + fileType.name();
    }

    /**
     * Get the file type.
     * 
     * @param fieldName
     *            the field name
     */
    public static FileType getFileType(String fieldName) {
        MultipartFormData body = Controller.request().body().asMultipartFormData();
        String fileType = body.asFormUrlEncoded().get(getFileTypeInputName(fieldName))[0];
        return FileType.valueOf(fileType);
    }

    /**
     * Get the file name.
     * 
     * @param fieldName
     *            the field name
     */
    public static String getFileName(String fieldName) {

        MultipartFormData body = Controller.request().body().asMultipartFormData();
        String fileName = body.asFormUrlEncoded().get(getFileNameInputName(fieldName))[0];

        FileType fileType = getFileType(fieldName);

        if (fileName != null && !fileName.equals("")) {

            // for UPLOAD type we add the extension
            if (fileType.equals(FileType.UPLOAD)) {
                String extension = "";
                String[] values = getFilePart(fieldName).getFilename().split("\\.");
                if (values.length > 1) {
                    extension = "." + values[values.length - 1];
                }
                fileName += extension;
            }

            return fileName;
        } else {

            switch (fileType) {
            case UPLOAD:
                return getFilePart(fieldName).getFilename();
            case URL:
                String label = getUrl(fieldName);
                String[] values = label.split("/");
                if (values.length > 1) {
                    label = values[values.length - 1];
                }
                return label;
            default:
                return null;

            }

        }

    }

    /**
     * Return true if the specified form contains a valid file field.
     * 
     * @param fieldName
     *            the field name
     * 
     * @return a boolean
     */
    public static boolean hasFileField(String fieldName) {

        boolean r = false;

        MultipartFormData body = Controller.request().body().asMultipartFormData();
        if (body != null) {

            FileType fileType = getFileType(fieldName);
            String fileFieldName = getFileInputName(fieldName, fileType);

            switch (fileType) {
            case UPLOAD:
                if (body.getFile(fileFieldName) != null) {
                    r = true;
                }
                break;
            case URL:
                if (body.asFormUrlEncoded().get(fileFieldName)[0] != null && !body.asFormUrlEncoded().get(fileFieldName)[0].equals("")) {
                    r = true;
                }
                break;
            }
        }

        return r;
    }

    /**
     * Get the file part of the attachment for UPLOAD type.
     * 
     * @param fieldName
     *            the field name
     */
    public static FilePart getFilePart(String fieldName) {

        FileType fileType = getFileType(fieldName);

        if (fileType.equals(FileType.UPLOAD)) {
            MultipartFormData body = Controller.request().body().asMultipartFormData();
            FilePart filePart = body.getFile(getFileInputName(fieldName, fileType));
            return filePart;
        }

        return null;

    }

    /**
     * Get the URL of the attachment for URL type.
     * 
     * @param fieldName
     *            the field name
     */
    public static String getUrl(String fieldName) {

        FileType fileType = getFileType(fieldName);

        if (fileType.equals(FileType.URL)) {
            MultipartFormData body = Controller.request().body().asMultipartFormData();
            return body.asFormUrlEncoded().get(getFileInputName(fieldName, fileType))[0];
        }

        return null;

    }

    /**
     * Save the specified file as an attachment.
     * 
     * @param fieldName
     *            the field name
     * @param objectClass
     *            the object class
     * @param objectId
     *            the object id
     * @param attachmentPlugin
     *            the service which is managing attachments
     */
    public static Long saveAsAttachement(String fieldName, Class<?> objectClass, Long objectId, IAttachmentManagerPlugin attachmentPlugin) throws IOException {
        FileInputStream fIn = null;
        try {

            FileType fileType = getFileType(fieldName);

            switch (fileType) {
            case UPLOAD:
                FilePart filePart = getFilePart(fieldName);
                fIn = new FileInputStream(filePart.getFile());
                return attachmentPlugin.addFileAttachment(fIn, filePart.getContentType(), getFileName(fieldName), objectClass, objectId);
            case URL:
                return attachmentPlugin.addUrlAttachment(getUrl(fieldName), getFileName(fieldName), objectClass, objectId);
            default:
                return null;

            }

        } catch (Exception e) {
            String message = String.format("Failure while creating the attachments for : %s [class %s]", objectId, objectClass);
            throw new IOException(message, e);
        } finally {
            IOUtils.closeQuietly(fIn);
        }
    }
    
    /**
     * Ensure that the specified attachment can be displayed by the end user.<br/>
     * WARNING: this process is overwriting any previously set authorizations.
     * @param attachementId an attachment id
     * @param sessionManagerPlugin
     *            the service which is managing user sessions
     */
    public static void authorizeFileAttachementForDisplay(Long attachementId, IUserSessionManagerPlugin sessionManagerPlugin){
    	Set<Long> allowedIds=new HashSet<Long>();
    	allowedIds.add(attachementId);
    	String uid = sessionManagerPlugin.getUserSessionId(Controller.ctx());
        allocateReadAuthorization(allowedIds, uid);
    }
    
    /**
     * Ensure that the specified attachment can be displayed by the end user.<br/>
     * WARNING: this process is overwriting any previously set authorizations.
     * @param attachementId an attachment id
     * @param sessionManagerPlugin
     *            the service which is managing user sessions
     */
    public static void authorizeFileAttachementForUpdate(Long attachementId, IUserSessionManagerPlugin sessionManagerPlugin){
    	Set<Long> allowedIds=new HashSet<Long>();
    	allowedIds.add(attachementId);
    	String uid = sessionManagerPlugin.getUserSessionId(Controller.ctx());
        allocateUpdateAuthorization(allowedIds, uid);
    }

    /**
     * Get attachments for display.<br/>
     * Return a list of attachments which is to be displayed to the end user.
     * <br/>
     * If this method is called this means that the user is "allowed" to
     * download the file.<br/>
     * This methods thus generates an authorization token which contains the id
     * of the attachment. This "authorization token" is set in the session of
     * the user. The attachment download function will check this token before
     * allowing the download. This is to prevent any unauthorized download.
     * 
     * @param objectClass
     *            the object class
     * @param objectId
     *            the object id
     * @param attachmentManagerPlugin
     *            the service which is managing attachments
     * @param sessionManagerPlugin
     *            the service which is managing user sessions
     */
    public static List<Attachment> getFileAttachmentsForDisplay(Class<?> objectClass, Long objectId, IAttachmentManagerPlugin attachmentManagerPlugin,
            IUserSessionManagerPlugin sessionManagerPlugin) {
        return getFileAttachmentsForUpdateOrDisplay(objectClass, objectId, false, attachmentManagerPlugin, sessionManagerPlugin);
    }

    /**
     * Get attachments for update.<br/>
     * Return a list of attachments which is to be displayed and possibly
     * updated by the end user.<br/>
     * If this method is called this means that the user is "allowed" to
     * download the file.<br/>
     * This methods thus generates an authorization token which contains the id
     * of the attachment. This "authorization token" is set in the session of
     * the user. The attachment download function will check this token before
     * allowing the update. This is to prevent any unauthorized update.
     * 
     * @param objectClass
     *            the object class
     * @param objectId
     *            the object id
     * @param attachmentManagerPlugin
     *            the service which is managing attachments
     * @param sessionManagerPlugin
     *            the service which is managing user sessions
     */
    public static List<Attachment> getFileAttachmentsForUpdate(Class<?> objectClass, Long objectId, IAttachmentManagerPlugin attachmentManagerPlugin,
            IUserSessionManagerPlugin sessionManagerPlugin) {
        return getFileAttachmentsForUpdateOrDisplay(objectClass, objectId, true, attachmentManagerPlugin, sessionManagerPlugin);
    }

    /**
     * Return a list of attachments for display or update.
     * 
     * @param objectClass
     *            the class of the object to which the attachment belong
     * @param objectId
     *            the id of the object to which the attachment belong
     * @param canUpdate
     *            true if the update authorization should be allocated
     * @param attachmentManagerPlugin
     *            the service which is managing attachments
     * @param sessionManagerPlugin
     *            the service which is managing user sessions
     */
    private static List<Attachment> getFileAttachmentsForUpdateOrDisplay(Class<?> objectClass, Long objectId, boolean canUpdate,
            IAttachmentManagerPlugin attachmentManagerPlugin, IUserSessionManagerPlugin sessionManagerPlugin) {
        List<Attachment> attachments = attachmentManagerPlugin.getAttachmentsFromObjectTypeAndObjectId(objectClass, objectId, false);
        Set<Long> allowedIds = new HashSet<Long>();
        for (Attachment attachment : attachments) {
            allowedIds.add(attachment.id);
        }
        String uid = sessionManagerPlugin.getUserSessionId(Controller.ctx());
        allocateReadAuthorization(allowedIds, uid);
        if (canUpdate) {
            allocateUpdateAuthorization(allowedIds, uid);
        }
        return attachments;
    }

    /**
     * Add read authorization for the specified list of attachments.
     * 
     * @param allowedIds
     *            a list of ids
     * @param uid
     *            the uid of the authorized user
     */
    private static void allocateReadAuthorization(Set<Long> allowedIds, String uid) {
        if (Cache.get(IFrameworkConstants.ATTACHMENT_READ_AUTHZ_CACHE_PREFIX + uid) != null) {
            @SuppressWarnings("unchecked")
            Set<Long> otherAllowerIds = (Set<Long>) Cache.get(IFrameworkConstants.ATTACHMENT_READ_AUTHZ_CACHE_PREFIX + uid);
            allowedIds.addAll(otherAllowerIds);
        }
        Cache.set(IFrameworkConstants.ATTACHMENT_READ_AUTHZ_CACHE_PREFIX + uid, allowedIds, AUTHZ_FILE_DURATION);
    }

    /**
     * Add update authorization for the specified list of attachments.
     * 
     * @param allowedIds
     *            a list of ids
     * @param uid
     *            the uid of the authorized user
     */
    private static void allocateUpdateAuthorization(Set<Long> allowedIds, String uid) {
        if (Cache.get(IFrameworkConstants.ATTACHMENT_WRITE_AUTHZ_CACHE_PREFIX + uid) != null) {
            @SuppressWarnings("unchecked")
            Set<Long> otherAllowerIds = (Set<Long>) Cache.get(IFrameworkConstants.ATTACHMENT_WRITE_AUTHZ_CACHE_PREFIX + uid);
            allowedIds.addAll(otherAllowerIds);
        }
        Cache.set(IFrameworkConstants.ATTACHMENT_WRITE_AUTHZ_CACHE_PREFIX + uid, allowedIds, AUTHZ_FILE_DURATION);
    }

    /**
     * This method is to be integrated within a controller.<br/>
     * It looks for the specified attachment and returns it if the user is
     * allowed to access it.
     * 
     * @param attachmentId
     *            the id of an attachment
     * @param attachmentManagerPlugin
     *            the service which is managing attachments
     * @param sessionManagerPlugin
     *            the service which is managing user sessions
     * @return the attachment as a stream
     */
    public static Result downloadFileAttachment(Long attachmentId, IAttachmentManagerPlugin attachmentManagerPlugin,
            IUserSessionManagerPlugin sessionManagerPlugin) {
        @SuppressWarnings("unchecked")
        Set<Long> allowedIds = (Set<Long>) Cache
                .get(IFrameworkConstants.ATTACHMENT_READ_AUTHZ_CACHE_PREFIX + sessionManagerPlugin.getUserSessionId(Controller.ctx()));
        if (allowedIds != null && allowedIds.contains(attachmentId)) {
            try {
                Attachment attachment = attachmentManagerPlugin.getAttachmentFromId(attachmentId);

                if (attachment.mimeType.equals(FileAttachmentHelper.FileType.URL.name())) {
                    return Controller.redirect(attachment.path);
                } else {
                    Controller.response().setHeader("Content-Disposition", "attachment; filename=\"" + attachment.name + "\"");
                    return Controller.ok(attachmentManagerPlugin.getAttachmentContent(attachmentId));
                }
            } catch (IOException e) {
                log.error("Error while retreiving the attachment content for " + attachmentId);
                return Controller.badRequest();
            }
        }
        return Controller.badRequest();
    }

    /**
     * This method is to be integrated within a controller.<br/>
     * It looks for the specified attachment and delete it if the user is
     * allowed to erase it.<br/>
     * It is to be called by an AJAX GET with a single attribute : the id of the
     * attachment.
     *
     * @param attachmentId
     *            the id of an attachment
     * @param attachmentManagerPlugin
     *            the service which is managing attachments
     * @param sessionManagerPlugin
     *            the service which is managing user sessions
     * @return the result
     */
    public static Result deleteFileAttachment(Long attachmentId, IAttachmentManagerPlugin attachmentManagerPlugin,
            IUserSessionManagerPlugin sessionManagerPlugin) {
        @SuppressWarnings("unchecked")
        Set<Long> allowedIds = (Set<Long>) Cache
                .get(IFrameworkConstants.ATTACHMENT_WRITE_AUTHZ_CACHE_PREFIX + sessionManagerPlugin.getUserSessionId(Controller.ctx()));
        if (allowedIds != null && allowedIds.contains(attachmentId)) {
            try {
                attachmentManagerPlugin.deleteAttachment(attachmentId);
                return Controller.ok();
            } catch (IOException e) {
                log.error("Error while deleting the attachment content for " + attachmentId);
                return Controller.badRequest();
            }
        }
        return Controller.badRequest();
    }

    /**
     * The possible file types.
     * 
     * @author Johann Kohler
     * 
     */
    public static enum FileType {
        UPLOAD, URL;

        public String getLabel() {
            return Msg.get("form.input.file_field.type." + name() + ".label");
        }
    }
}
