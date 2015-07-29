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

import java.net.URL;
import java.util.List;
import java.util.Map;

import framework.commons.DataType;

/**
 * The interface which describes the plugin Runner (meta-information)
 * 
 * @author Pierre-Yves Cloux
 */
public interface IStaticPluginRunnerDescriptor {
    /**
     * Return the unique id of the plugin definition (common to all the plugin
     * instances)
     */
    public String getPluginDefinitionIdentifier();

    /**
     * The version of the plugin
     */
    public String getVersion();

    /**
     * The URL to the web site of the vendor of the plugin
     */
    public URL getVendorUrl();

    /**
     * True if the plugin can be multi-instanciated (multiple configurations for
     * the same plugin)
     * 
     * @return a boolean
     */
    public boolean multiInstanceAllowed();

    /**
     * The i18n key for the name of the plugin
     */
    public String getName();

    /**
     * The i18n key for the description of the plugin
     */
    public String getDescription();

    /**
     * Returns a map of configuration blocks required by this plugin indexed by
     * their unique identifier
     * 
     * @return a map of configuration blocks
     */
    public Map<String, IPluginConfigurationBlockDescriptor> getConfigurationBlockDescriptors();

    /**
     * Returns a descriptor of plugin menu entry (giving access to a plugin GUI)
     * 
     * @return a plugin menu descriptor
     */
    public IPluginMenuDescriptor getMenuDescriptor();

    /**
     * Returns a map of actions supported by this plugin indexed by their unique
     * identifier
     * 
     * @return a map of actions descriptors
     */
    public Map<String, IPluginActionDescriptor> getActionDescriptors();

    /**
     * Returns a list of supported data types.<br/>
     * Meaning that this plugin is able to deal with the listed data types
     */
    public List<DataType> getSupportedDataTypes();
}
