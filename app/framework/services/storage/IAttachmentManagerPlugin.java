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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import models.framework_models.common.Attachment;

/**
 * A plugin which manages the storage of an attachment.<br/>
 * An attachment is either a file (stored on the server file system) or a
 * structured document (XML document stored in the database) which is associated
 * with an object. WARNING: please see the documentation of each method. Some
 * requires an Ebean transaction to be active in order to rollback the operation
 * in case of exception
 * 
 * @author Pierre-Yves Cloux
 */
public interface IAttachmentManagerPlugin {

    /**
     * Add an attachment which is an URL and return the created attachment id.
     * 
     * @param url
     *            the URL
     * @param name
     *            the name of the attachment (human readable)
     * @param objectType
     *            the type of object (java class name)
     * @param objectId
     *            the Long id of the object (a Model object usually)
     * @return the id of the attachment which has been recorded into the
     *         database
     */
    public Long addUrlAttachment(String url, String name, Class<?> objectType, Long objectId) throws IOException;

    /**
     * Add an attachment which is a file (stored on the file system with a
     * path).
     * <p>
     * <b>Please wrap this method within a database transaction</b>
     * </p>
     * 
     * @param inputStream
     *            a stream of bytes (usually an XML structure)
     * @param mimeType
     *            the MIME type of the binary stream
     * @param name
     *            the name of the attachment (human readable)
     * @param objectType
     *            the type of object (java class name)
     * @param objectId
     *            the Long id of the object (a Model object usually)
     * @return the id of the attachment which has been recorded into the
     *         database
     */
    public Long addFileAttachment(InputStream inputStream, String mimeType, String name, Class<?> objectType, Long objectId) throws IOException;

    /**
     * Add a structured item attachment.
     * <p>
     * <b>Please wrap this method within a database transaction</b>
     * </p>
     * 
     * @param inputStream
     *            a stream of bytes (usually an XML structure)
     * @param mimeType
     *            the MIME type of the binary stream
     * @param name
     *            the name of the attachment (human readable)
     * @param objectType
     *            the type of object (java class name)
     * @param objectId
     *            the Long id of the object (a Model object usually)
     * @return the id of the attachment which has been recorded into the
     *         database
     */
    public Long addStructuredDocumentAttachment(InputStream inputStream, String mimeType, String name, Class<?> objectType, Long objectId) throws IOException;

    /**
     * Add a structured item attachment.
     * <p>
     * <b>Please wrap this method within a database transaction</b>
     * </p>
     * 
     * @param objectToStore
     *            an object to be stored as a serialized XML stream
     * @param mimeType
     *            the MIME type of the binary stream
     * @param name
     *            the name of the attachment (human readable)
     * @param objectType
     *            the type of object (java class name)
     * @param objectId
     *            the Long id of the object (a Model object usually)
     * @return the id of the attachment which has been recorded into the
     *         database
     */
    public Long addStructuredDocumentAttachment(Object objectToStore, String mimeType, String name, Class<?> objectType, Long objectId) throws IOException;

    /**
     * Attach an existing attachment to an object.
     * <p>
     * <b>Please wrap this method within a database transaction</b>
     * </p>
     * 
     * @param existingAttachmentId
     *            ID of an existing attachment to be re-linked
     * @param objectType
     *            the type of object (java class name)
     * @param objectId
     *            the Long id of the object (a Model object usually)
     * @return the id of the attachment which has been recorded into the
     *         database
     */
    public Long createLink(Long existingAttachmentId, Class<?> objectType, Long objectId) throws IOException;

    /**
     * Move an attachment from an object to another one.<br/>
     * The existing attachment is deleted and the data are transferred to a new
     * one.
     * <p>
     * <b>Please wrap this method within a database transaction</b>
     * </p>
     * 
     * @param existingAttachmentId
     *            ID of an existing attachment to be moved
     * @param objectType
     *            the type of object (java class name)
     * @param objectId
     *            the Long id of the object (a Model object usually)
     * @return the id of the attachment which has been recorded into the
     *         database
     */
    public Long moveAttachment(Long existingAttachmentId, Class<?> objectType, Long objectId) throws IOException;

    /**
     * Return the data associated with the specified attachment
     * 
     * @param attachmentId
     *            ID of an existing attachment to be moved
     */
    public InputStream getAttachmentContent(Long attachmentId) throws IOException;

    /**
     * Return the object associated with the specified attachment.<br/>
     * <b>WARNING:</b> an exception is throws if the attachment is not
     * associated to a structured document
     * 
     * @param attachmentId
     *            ID of an existing attachment to be moved
     */
    public Object getStructuredDocumentAttachmentContent(Long attachmentId) throws IOException;

    /**
     * Delete the data associated with the attachment (if it is not referenced
     * by any other attachment).<br/>
     * <b>WARNING</b>: this operation cannot be reverted, once a file is deleted
     * it does not exists anymore.
     * <p>
     * <b>Please wrap this method within a database transaction</b>
     * </p>
     * 
     * @param attachmentId
     *            an attachment id
     * @throws IOException
     */
    public void deleteAttachment(Long attachmentId) throws IOException;

    /**
     * Update an attachment with the data provided.<br/>
     * <b>WARNING</b>: all the attachments referencing the same content are
     * updated at the same time.<br/>
     * If this is not the wished behavior, please create a new attachment.
     * <p>
     * <b>Please wrap this method within a database transaction</b>
     * </p>
     * 
     * @param attachmentId
     *            an attachment id
     * @throws IOException
     */
    public void updateAttachment(Long attachmentId, InputStream inputStream) throws IOException;

    /**
     * Return the attachment associated with the specified unique id
     * 
     * @param id
     *            an Id
     * @return an attachment or null if not found
     */
    public Attachment getAttachmentFromId(Long id);

    /**
     * Return a list of attachments associated with the specified objectType and
     * objectId
     * 
     * @param objectType
     *            an objectType
     * @param objectId
     *            an objectId
     * @return a list of attachments
     */
    public List<Attachment> getAttachmentsFromObjectTypeAndObjectId(Class<?> objectType, Long objectId);

    /**
     * Return a list of attachments associated with the specified objectType and
     * objectId.<br/>
     * Filter either file files or the structured documents.
     * 
     * @param objectType
     *            an objectType
     * @param objectId
     *            an objectId
     * @param structuredDocument
     *            if true returns only attachments with structured documents
     *            (and ignore the files)
     * @return a list of attachments
     */
    public List<Attachment> getAttachmentsFromObjectTypeAndObjectId(Class<?> objectType, Long objectId, boolean structuredDocument);

    /**
     * Get the size (bytes) of the attachments folder.
     */
    public Long getSize();

    /**
     * Get the URL to an ajax wait animated gif.
     */
    String getAjaxWaitImageUrl();

    /**
     * Get the URL to download an attachment.
     * 
     * @param attachmentId
     *            the attachment id
     */
    String getAttachmentDownloadUrl(Long attachmentId);

    /**
     * Get the URL to delete an attachment.
     * 
     * @param attachmentId
     *            the attachment id
     */
    String getAttachmentDeleteUrl(Long attachmentId);
}
