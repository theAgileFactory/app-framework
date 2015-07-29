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
import play.db.ebean.Model;
import framework.commons.DataType;

/**
 * It is possible while not mandatory to register some objects with a
 * pluginConfiguration.<br/>
 * The purpose is to limit the handling of the events by a named
 * {@link PluginFlowConfiguration} to the registered objects.
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class PluginRegistration extends Model {
    private static final long serialVersionUID = 6159060840809455544L;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, PluginRegistration> find = new Finder<Long, PluginRegistration>(Long.class, PluginRegistration.class);

    @Id
    public Long id;

    @Column(length = IModelConstants.LARGE_STRING)
    public String dataType;

    @Column(nullable = false)
    public Long internalId;

    @Lob
    public byte[] configurationProperties;

    @ManyToOne(optional = false)
    public PluginConfiguration pluginConfiguration;

    public PluginRegistration() {
    }

    /**
     * Return true if a registration is found
     * 
     * @param pluginConfigurationId
     *            the Id of the plugin configuration
     * @param dataType
     *            the type of the BizDock object
     * @param internalId
     *            the Id of the object
     * @return a boolean
     */
    public static boolean isRegistered(Long pluginConfigurationId, DataType dataType, Long internalId) {
        return find.where().eq("pluginConfiguration.id", pluginConfigurationId).eq("dataType", dataType.getDataTypeClassName()).eq("internalId", internalId)
                .findRowCount() == 1;
    }

    /**
     * Return a plugin registration or null if not found
     * 
     * @param pluginConfigurationId
     *            the Id of the plugin configuration
     * @param dataType
     *            the type of the BizDock object
     * @param internalId
     *            the Id of the object
     * @return a boolean
     */
    public static PluginRegistration getPluginRegistration(Long pluginConfigurationId, DataType dataType, Long internalId) {
        return find.where().eq("pluginConfiguration.id", pluginConfigurationId).eq("dataType", dataType.getDataTypeClassName()).eq("internalId", internalId)
                .findUnique();
    }
}
