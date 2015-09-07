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

import java.util.Map;

import framework.commons.DataType;

/**
 * The interface which provides the "dynamic meta-data" for a plugin.<br/>
 * These meta-data are "dynamic" because they may access some external resources
 * 
 * @author Pierre-Yves Cloux
 */
public interface IPluginRunnerConfigurator {
    /**
     * A custom configuration interface for advanced configurations options.
     * 
     * @return
     */
    public AbstractCustomConfiguratorController getCustomConfigurator();

    /**
     * Returns a map of registration option for a named DataType.
     */
    public Map<DataType, AbstractRegistrationConfiguratorController> getDataTypesWithRegistration();

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
}
