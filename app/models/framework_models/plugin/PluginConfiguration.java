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
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import models.framework_models.parent.IModelConstants;

import com.avaje.ebean.Model;

import framework.utils.Utilities;

/**
 * This represents a pluginConfiguration which deals with the integration of a
 * third party system.<br/>
 * A plugin configuration is an instance of a {@link PluginDefinition}.<br/>
 * Indeed we may imagine multiple instances of the same plugin running at the
 * same time. A pluginConfiguration has the following attributes:
 * <ul>
 * <li>isAutostart : if true the plugin is autostarted at system startup</li>
 * <li>stateStorage : a blob which can contains some persistent data for a
 * plugin</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class PluginConfiguration extends Model {
    private static final long serialVersionUID = 1766883631285142680L;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, PluginConfiguration> find = new Finder<Long, PluginConfiguration>(PluginConfiguration.class);

    @Id
    public Long id;

    public boolean isAutostart;

    @Lob
    public byte[] stateStorage;

    @Column(length = IModelConstants.LARGE_STRING, nullable = false)
    public String name;

    @OneToMany(mappedBy = "pluginConfiguration", cascade = CascadeType.ALL)
    public List<PluginConfigurationBlock> pluginConfigurationBlocks;

    @OneToMany(mappedBy = "pluginConfiguration", cascade = CascadeType.ALL)
    public List<PluginLog> pluginLogs;

    @OneToMany(mappedBy = "pluginConfiguration", cascade = CascadeType.ALL)
    public List<PluginIdentificationLink> pluginIdentificationLinks;

    @OneToMany(mappedBy = "pluginConfiguration", cascade = CascadeType.ALL)
    public List<PluginRegistration> pluginRegistrations;

    @ManyToOne(optional = false)
    public PluginDefinition pluginDefinition;

    public PluginConfiguration() {
    }

    /**
     * Return the state of the configuration plugin.<br/>
     * The stored object must be {@link Serializable}
     * 
     * @param stateObject
     *            an object which is to be stored as XML in the database.
     */
    public void setState(Object stateObject) {
        this.stateStorage = Utilities.marshallObject(stateObject);
    }

    /**
     * Get the state of the configuration plugin.
     * 
     * @return an object
     */
    public Object getState() {
        return Utilities.unmarshallObject(this.stateStorage);
    }

    /**
     * Return the pluginConfiguration associated with the specified identifier.<br/>
     * The pluginConfiguration must be available.
     * 
     * @param pluginConfigurationId
     *            the unique id of the pluginConfiguration
     * @return a PluginConfiguration instance
     */
    public static PluginConfiguration getAvailablePluginById(Long pluginConfigurationId) {
        return find.where().eq("pluginDefinition.isAvailable", true).eq("id", pluginConfigurationId).findUnique();
    }

    /**
     * Return the pluginConfiguration associated with the specified identifier.<br/>
     * The pluginConfiguration may not available.
     * 
     * @param pluginConfigurationId
     *            the unique id of the pluginConfiguration
     * @return a PluginConfiguration instance
     */
    public static PluginConfiguration getPluginById(Long pluginConfigurationId) {
        return find.where().eq("id", pluginConfigurationId).findUnique();
    }

    /**
     * Return all the plugins which are "available"
     * 
     * @return a list of PluginConfiguration instance
     */
    public static List<PluginConfiguration> getAllAvailablePlugins() {
        return find.where().eq("pluginDefinition.isAvailable", true).findList();
    }
}
