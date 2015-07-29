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

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import models.framework_models.parent.IModelConstants;
import play.db.ebean.Model;
import framework.services.plugins.api.IPluginRunner;

/**
 * This represents a plugin definition which could be "instanciated" with
 * multiple {@link PluginConfiguration} Here are the attributes of a plugin
 * definition:
 * <ul>
 * <li>identifier : a unique human readable identifier for the plugin definition
 * </li>
 * <li>clazz : the Java class name implementing the {@link IPluginRunner}
 * interface</li>
 * <li>isAvailable : if false the plugin cannot be used (example: not paid)</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class PluginDefinition extends Model {
    private static final long serialVersionUID = 1766883631285142680L;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, PluginDefinition> find = new Finder<Long, PluginDefinition>(Long.class, PluginDefinition.class);

    @Id
    public Long id;

    @Column(length = IModelConstants.LARGE_STRING, nullable = false)
    public String identifier;

    @Column(length = IModelConstants.LARGE_STRING, nullable = false)
    public String clazz;

    public boolean isAvailable;

    @OneToMany(mappedBy = "pluginDefinition")
    public List<PluginConfiguration> pluginConfigurations;

    public PluginDefinition() {
    }

    /**
     * Return all the available plugin definitions
     */
    public static List<PluginDefinition> getAllPluginDefinitions() {
        return find.where().findList();
    }

    /**
     * Return all the available plugin definitions
     */
    public static List<PluginDefinition> getAllAvailablePluginDefinitions() {
        return find.where().eq("isAvailable", true).findList();
    }

    /**
     * Return the available plugin definition associated with the specified
     * identifier
     * 
     * @param pluginDefinitionIdentifier
     *            a plugin definition identifier
     */
    public static PluginDefinition getAvailablePluginDefinitionFromIdentifier(String pluginDefinitionIdentifier) {
        return find.where().eq("isAvailable", true).eq("identifier", pluginDefinitionIdentifier).findUnique();
    }

    /**
     * Return the plugin definition associated with the specified identifier
     * 
     * @param pluginDefinitionIdentifier
     *            a plugin definition identifier
     */
    public static PluginDefinition getPluginDefinitionFromIdentifier(String pluginDefinitionIdentifier) {
        return find.where().eq("identifier", pluginDefinitionIdentifier).findUnique();
    }

    /**
     * Return the available plugin definition associated with the specified Id
     * 
     * @param pluginDefinitionId
     *            a plugin definition id
     */
    public static PluginDefinition getAvailablePluginDefinitionFromId(Long pluginDefinitionId) {
        return find.where().eq("isAvailable", true).eq("id", pluginDefinitionId).findUnique();
    }
}
