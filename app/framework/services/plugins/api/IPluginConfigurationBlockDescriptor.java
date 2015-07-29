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
package framework.services.plugins.api;

/**
 * A meta-description of a configuration block for a plugin
 * 
 * @author Pierre-Yves Cloux
 */
public interface IPluginConfigurationBlockDescriptor {
    /**
     * The allowed types of configuration blocks
     * 
     * @author Pierre-Yves Cloux
     */
    public enum ConfigurationBlockEditionType {
        XML, PROPERTIES, JAVASCRIPT, VELOCITY, FILE
    }

    /**
     * The version of this configuration descriptor.<br/>
     * This one will be stored in the database.<br/>
     * Each time you modify the configuration descriptor with a risk of
     * incompatibility, you must increment this version number.<br/>
     * This version number should be checked when reading the configuration.
     */
    public int getVersion();

    /**
     * The unique identifier of the configuration block.<br/>
     * You can use it to extract the configuration block from the database
     * 
     * @return a String
     */
    public String getIdentifier();

    /**
     * The i18n key for name of the configuration block
     */
    public String getName();

    /**
     * The i18n key for the description of the configuration block
     */
    public String getDescription();

    /**
     * Return the type of the configuration block.<br/>
     * This defines the way the block will be edited through the configuration
     * interface
     */
    public ConfigurationBlockEditionType getEditionType();

    /**
     * Return the default value (when the plugin is initialized for the first
     * time or manually reseted)
     */
    public byte[] getDefaultValue();
}
