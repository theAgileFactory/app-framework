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
package framework.commons.message;

import java.util.ArrayList;
import java.util.List;

import models.framework_models.account.SystemLevelRoleType;
import framework.commons.DataType;

/**
 * An event message associated with changes occuring on
 * {@link SystemLevelRoleType} objects. The payload object contains the former
 * list of permissions (in order to track the removed or added permissions).
 * 
 * @author Pierre-Yves Cloux
 */
public class SystemLevelRoleTypeEventMessage extends EventMessage {
    private static final long serialVersionUID = 2577100743752554418L;

    public SystemLevelRoleTypeEventMessage() {
    }

    public SystemLevelRoleTypeEventMessage(String externalId, DataType dataType, EventMessage.MessageType messageType) {
        super(externalId, dataType, messageType);
    }

    public SystemLevelRoleTypeEventMessage(String externalId, DataType dataType, EventMessage.MessageType messageType, Object payload) {
        super(externalId, dataType, messageType, payload);
    }

    public SystemLevelRoleTypeEventMessage(String externalId, DataType dataType, EventMessage.MessageType messageType, Long pluginConfigurationId) {
        super(externalId, dataType, messageType, pluginConfigurationId);
    }

    public SystemLevelRoleTypeEventMessage(String externalId, DataType dataType, EventMessage.MessageType messageType, Long pluginConfigurationId,
            Object payload) {
        super(externalId, dataType, messageType, pluginConfigurationId, payload);
    }

    public SystemLevelRoleTypeEventMessage(Long internalId, DataType dataType, EventMessage.MessageType messageType) {
        super(internalId, dataType, messageType);
    }

    public SystemLevelRoleTypeEventMessage(Long internalId, DataType dataType, EventMessage.MessageType messageType, Long pluginConfigurationId) {
        super(internalId, dataType, messageType, pluginConfigurationId);
    }

    public SystemLevelRoleTypeEventMessage(Long internalId, DataType dataType, EventMessage.MessageType messageType, Long pluginConfigurationId, Object payload) {
        super(internalId, dataType, messageType, pluginConfigurationId, payload);
    }

    /**
     * A class which represents the payload for a role change event.<br/>
     * This is to be used by the plugin system.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class PayLoad {
        private List<String> previousPermissionNames;

        public List<String> getPreviousPermissionNames() {
            return previousPermissionNames;
        }

        public void setPreviousPermissionNames(List<String> previousPermissionNames) {
            if (this.previousPermissionNames == null) {
                this.previousPermissionNames = new ArrayList<String>();
            }
            this.previousPermissionNames = previousPermissionNames;
        }

    }
}
