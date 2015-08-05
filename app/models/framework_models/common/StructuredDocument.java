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

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Version;

import models.framework_models.parent.IModel;

import com.avaje.ebean.Model;

/**
 * A structured document is a document stored into the MAF DB.<br/>
 * This is often an XML serialized object.<br/>
 * Such object is often associated with a "renderer" which is simply a
 * controller url to be used to "display" the document.<br/>
 * WARNING: it is assumed that this controller will deal with the authorization
 * of access to this document
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class StructuredDocument extends Model implements IModel {
    private static final long serialVersionUID = -7054709702360918152L;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, StructuredDocument> find = new Finder<Long, StructuredDocument>(StructuredDocument.class);

    public boolean deleted = false;
    @Version
    public Timestamp lastUpdate;

    @Id
    public Long id;

    @Lob
    public byte[] content;

    @OneToOne(mappedBy = "structuredDocument", optional = true)
    public Attachment attachment;

    public StructuredDocument() {
    }

    @Override
    public String audit() {
        return "StructuredDocument [id=" + id + "]";
    }

    @Override
    public void defaults() {

    }

    @Override
    public void doDelete() {
        this.deleted = true;
        save();
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
