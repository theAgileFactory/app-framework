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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import models.framework_models.parent.IModelConstants;

import com.avaje.ebean.Model;

/**
 * This represents a configuration element of a plugin.<br/>
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class PluginConfigurationBlock extends Model {
    private static final long serialVersionUID = -7922152669516687690L;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, PluginConfigurationBlock> find = new Finder<Long, PluginConfigurationBlock>(PluginConfigurationBlock.class);

    @Id
    public Long id;

    public Integer version;

    @Column(length = IModelConstants.SMALL_STRING, nullable = false)
    public String identifier;

    @Column(length = IModelConstants.SMALL_STRING, nullable = false)
    public String configurationType;

    @Lob
    public byte[] configuration;

    @ManyToOne(optional = false)
    public PluginConfiguration pluginConfiguration;

    public PluginConfigurationBlock() {
    }

    /**
     * Return the plugin configuration block associated with the specified
     * identifier
     * 
     * @param pluginConfigurationId
     *            the id of the {@link PluginConfiguration}
     * @param identifier
     *            the unique identifier of the configuration
     */
    public static PluginConfigurationBlock getPluginConfigurationBlockFromIdentifier(Long pluginConfigurationId, String identifier) {
        return find.where().eq("pluginConfiguration.id", pluginConfigurationId).eq("identifier", identifier).findUnique();
    }
}
