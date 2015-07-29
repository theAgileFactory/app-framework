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

import framework.utils.FileAttachmentHelper.FileType;

/**
 * 
 * @author Johann Kohler
 * 
 */
public class FileField {

    private String value;

    private String fileName;

    private String fileType;

    /**
     * Default constructor.
     */
    public FileField() {
        this.setValue("");
        this.setFileName("");
        this.setFileType(FileType.UPLOAD);
    }

    /**
     * Get the file type.
     */
    public FileType getFileType() {
        return FileType.valueOf(fileType);
    }

    /**
     * Set the file type.
     * 
     * @param fileType
     *            the file type
     */
    public void setFileType(FileType fileType) {
        this.fileType = fileType.name();
    }

    /**
     * Get the value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the value.
     * 
     * @param value
     *            the value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Get the file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Set the file name.
     * 
     * @param fileName
     *            the file name
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

}
