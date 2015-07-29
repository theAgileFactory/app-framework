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
import framework.commons.message.EventMessage;
import framework.utils.DynamicFormDescriptor;

/**
 * The meta-descriptor of an action which the plugin is able to handle through
 * its out interface.<br/>
 * An action is a CUSTOM {@link EventMessage} sent to the OUT interface
 * automatically which triggered a specific behavior from the plugin.<br/>
 * This could be an admin action (reload of a configuration, reset of a counter,
 * etc.) or a data specific action (an action related to the a specific data
 * object).
 * 
 * @author Pierre-Yves Cloux
 */
public interface IPluginActionDescriptor {
    /**
     * Unique identifier of the action
     */
    public String getIdentifier();

    /**
     * The i18n key for the action name (to be displayed in a button)
     * 
     * @return a String
     */
    public String getLabel();

    /**
     * Return true if the action is an admin action.<br/>
     * If true, the "getDataType" method must return null.
     */
    public boolean isAdmin();

    /**
     * The data type to which this action can be sent
     * 
     * @return a data type
     */
    public DataType getDataType();

    /**
     * Return a form structure to be used to collect some parameters for the
     * payload generation
     * 
     * @return a form structure
     */
    public DynamicFormDescriptor getFormDescriptor();

    /**
     * Payload to be sent in the {@link EventMessage} if this action is
     * triggered
     * 
     * @param uniqueId
     *            if the action is not admin, the Id of the object to customize
     *            the payload
     * @return an object
     */
    public Object getPayLoad(Long uniqueId);

    /**
     * Payload to be sent in the {@link EventMessage} if this action is
     * triggered
     * 
     * @param uniqueId
     *            if the action is not admin, the Id of the object to customize
     *            the payload
     * @param parameters
     *            some parameters for the payload generation
     * @return an object
     */
    public Object getPayLoad(Long uniqueId, Map<String, Object> parameters);
}
