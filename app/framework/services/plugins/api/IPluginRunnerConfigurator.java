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
 * (compared to {@link IStaticPluginRunnerDescriptor} which are supposed to be
 * fully stateless).
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
     * The OUT interface configuration
     */
    public EventInterfaceConfiguration getOutInterfaceConfiguration();

    /**
     * The IN interface configuration
     */
    public EventInterfaceConfiguration getInInterfaceConfiguration();

    /**
     * Returns a map of registration option for a named DataType.
     */
    public Map<DataType, AbstractRegistrationConfiguratorController> getDataTypesWithRegistration();
}
