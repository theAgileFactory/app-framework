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
package models.framework_models.plugin;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Version;

import com.avaje.ebean.Model;

import framework.utils.Utilities;
import models.framework_models.parent.IModelConstants;

/**
 * A model class used to store some shared data between the plugins.
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class PluginSharedRecord extends Model {
    /**
     * Default finder for the entity class
     */
    public static Finder<Long, PluginSharedRecord> find = new Finder<Long, PluginSharedRecord>(PluginSharedRecord.class);

    @Id
    public Long id;
    
    @Column(length = IModelConstants.LARGE_STRING, nullable = false)
    public String name;

    @Column(length = IModelConstants.VLARGE_STRING, nullable=true)
    public String smallDataStorage;

    @Lob
    public byte[] bigDataStorage;
    
    @Version
    public Timestamp lastUpdate;

    public PluginSharedRecord() {
    }

    /**
     * Return the some data stored as a blob.<br/>
     * The stored object must be {@link Serializable}
     * 
     * @param stateObject
     *            an object which is to be stored as XML in the database.
     */
    public void setBigData(Object stateObject) {
        this.bigDataStorage = Utilities.marshallObject(stateObject);
    }

    /**
     * Plugin data stored as a blob.
     * 
     * @return an object
     */
    public Object getBigData() {
        return Utilities.unmarshallObject(this.bigDataStorage);
    }
    
    /**
     * Return the record associated with the specified id
     * @param sharedRecordId a record id
     */
    public static PluginSharedRecord getRecordById(Long sharedRecordId) {
        return find.where().eq("id", sharedRecordId).findUnique();
    }
    
    /**
     * Return the record associated with the specified key
     * @param sharedRecordKey a record key
     */
    public static PluginSharedRecord getRecordByName(String sharedRecordKey) {
        return find.where().eq("name", sharedRecordKey).findUnique();
    }
}
