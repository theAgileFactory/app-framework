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
package models.framework_models.common;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.avaje.ebean.Model;

import framework.services.configuration.II18nMessagesPlugin;
import framework.services.custom_attribute.ICustomAttributeManagerService;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import framework.utils.FileAttachmentHelper;
import framework.utils.Msg;
import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;
import play.Logger;
import play.api.data.Field;
import play.mvc.Http.MultipartFormData.FilePart;
import play.twirl.api.Html;

/**
 * The image custom attribute value. An image is represented by a String in the
 * DB.
 * 
 * @author Johann Kohler
 */
@Entity
@Table(name = "string_custom_attribute_value")
public class ImageCustomAttributeValue extends Model implements IModel, ICustomAttributeValue {

    public static Finder<Long, ImageCustomAttributeValue> find = new Finder<Long, ImageCustomAttributeValue>(ImageCustomAttributeValue.class);

    public static ArrayList<String> authorizedContentType = new ArrayList<String>() {
        private static final long serialVersionUID = 1L;

        {
            add("image/gif");
            add("image/jpeg");
            add("image/png");
        }
    };

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String objectType;

    public Long objectId;

    @Column(length = IModelConstants.LARGE_STRING)
    public String value;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public CustomAttributeDefinition customAttributeDefinition;

    @Transient
    private boolean hasError = false;

    @Transient
    private String errorMessage;

    @Transient
    private boolean isNotReadFromDb = false;

    /**
     * Default constructor.
     */
    public ImageCustomAttributeValue() {
    }

    @Override
    public String audit() {
        return "ImageCustomAttributeValue [id=" + id + ", deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", objectType=" + objectType + ", objectId="
                + objectId + ", value=" + value + "]";
    }

    @Override
    public void defaults() {
        if (customAttributeDefinition != null && customAttributeDefinition.getDefaultValueAsString() != null) {
            this.value = customAttributeDefinition.getDefaultValueAsString();
        }
    }

    @Override
    public void doDelete() {
        deleted = true;
        save();
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    @Override
    public CustomAttributeDefinition getDefinition() {
        return this.customAttributeDefinition;
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.IMAGE;
    }

    public static ImageCustomAttributeValue getOrCreateCustomAttributeValueFromObjectReference(Class<?> objectType, String filter, Long objectId,
            CustomAttributeDefinition customAttributeDefinition) {

        String key = objectType.getName();
        if (filter != null) {
            key += ":" + filter;
        }

        ImageCustomAttributeValue customAttributeValue = find.where().eq("deleted", false).eq("objectType", key).eq("objectId", objectId)
                .eq("customAttributeDefinition.id", customAttributeDefinition.id).findUnique();
        if (customAttributeValue == null) {
            customAttributeValue = new ImageCustomAttributeValue();
            customAttributeValue.objectType = key;
            customAttributeValue.objectId = objectId;
            customAttributeValue.customAttributeDefinition = customAttributeDefinition;
            customAttributeValue.isNotReadFromDb = true;
        }
        return customAttributeValue;
    }

    @Override
    public Object getValueAsObject() {
        return this.value;
    }

    @Override
    public String print() {
        if (this.value == null) {
            return "";
        }
        return this.value;
    }

    @Override
    public boolean parse(II18nMessagesPlugin i18nMessagesPlugin, String text) {
        return false;
    }

    @Override
    public boolean parseFile(ICustomAttributeManagerService customAttributeManagerService) {

        String fieldName = customAttributeManagerService.getFieldNameFromDefinitionUuid(getDefinition().uuid);
        if (FileAttachmentHelper.hasFileField(fieldName)) {

            FilePart filePart = FileAttachmentHelper.getFilePart(fieldName);

            if (authorizedContentType.contains(filePart.getContentType())) {

                if (customAttributeDefinition.maxWidth() != null || customAttributeDefinition.maxHeight() != null) {

                    int imageWidth = 0;
                    int imageHeight = 0;

                    BufferedImage bimg;
                    try {
                        bimg = ImageIO.read(filePart.getFile());
                        imageWidth = bimg.getWidth();
                        imageHeight = bimg.getHeight();
                    } catch (IOException e) {
                    }

                    Logger.debug("imageWidth: " + imageWidth);
                    Logger.debug("imageHeight: " + imageHeight);

                    if (customAttributeDefinition.maxWidth() != null && customAttributeDefinition.maxWidth().intValue() < imageWidth) {
                        this.hasError = true;
                        this.errorMessage = Msg.get(customAttributeDefinition.getMaxWidthMessage(), customAttributeDefinition.maxWidth());
                        return false;
                    }

                    if (customAttributeDefinition.maxHeight() != null && customAttributeDefinition.maxHeight().intValue() < imageHeight) {
                        this.hasError = true;
                        this.errorMessage = Msg.get(customAttributeDefinition.getMaxHeightMessage(), customAttributeDefinition.maxHeight());
                        return false;
                    }

                }

                this.value = "#attachment";

            } else {
                this.hasError = true;
                this.errorMessage = Msg.get(Msg.get("form.input.image.required"));
                return false;
            }
        }

        return true;
    }

    @Override
    public void setValueAsObject(Object newValue) {
        // Nothing done here
    }

    @Override
    public boolean hasError() {
        return this.hasError;
    }

    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Override
    public void resetError() {
        this.hasError = false;
        this.errorMessage = null;
    }

    @Override
    public Html renderFormField(II18nMessagesPlugin i18nMessagesPlugin, Field field, boolean displayDescription) {
        String description = "";
        if (displayDescription) {
            description = customAttributeDefinition.description;
        }
        return views.html.framework_views.parts.fileupload_field.render(field, customAttributeDefinition.name, description, null, null);
    }

    @Override
    public Html renderDisplay(II18nMessagesPlugin i18nMessagesPlugin) {
        if (value.equals("#attachment")) {
            return views.html.framework_views.parts.formats.display_image_from_file_attachment.render(ImageCustomAttributeValue.class, this.id);
        } else {
            return views.html.framework_views.parts.formats.display_image.render(value);
        }
    }

    @Override
    public Html renderDisplayNoDescription(II18nMessagesPlugin i18nMessagesPlugin) {
        return renderDisplay(i18nMessagesPlugin);
    }

    @Override
    public void performSave(IUserSessionManagerPlugin userSessionManagerPlugin, IAttachmentManagerPlugin attachmentManagerPlugin, String fieldName) {

        if (FileAttachmentHelper.hasFileField(fieldName)) {

            save();
            this.isNotReadFromDb = false;

            try {
                // if exists, remove the current image
                List<Attachment> attachments = FileAttachmentHelper.getFileAttachmentsForDisplay(ImageCustomAttributeValue.class, this.id,
                        attachmentManagerPlugin, userSessionManagerPlugin);
                if (attachments != null && attachments.size() > 0) {
                    FileAttachmentHelper.deleteFileAttachment(attachments.get(0).id, attachmentManagerPlugin, userSessionManagerPlugin);
                    attachments.get(0).doDelete();
                }

                // add the new image
                FileAttachmentHelper.saveAsAttachement(fieldName, ImageCustomAttributeValue.class, this.id, attachmentManagerPlugin);

            } catch (Exception e) {
            }
        }

    }

    @Override
    public boolean isNotReadFromDb() {
        return isNotReadFromDb;
    }

    @Override
    public Object getAsSerializableValue() {
        return getValueAsObject();
    }

    @Override
    public String getLinkedObjectClassName() {
        return objectType;
    }

    @Override
    public Long getLinkedObjectId() {
        return objectId;
    }
}
