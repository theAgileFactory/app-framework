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

import framework.commons.DataType;

/**
 * An event message sent for modifications regarding the User {@link DataType}.<br/>
 * For the OBJECT_DELETED event, the "uid" of the user is retained in the
 * {@link PayLoad} object.
 * 
 * @author Pierre-Yves Cloux
 */
public class UserEventMessage extends EventMessage {
    private static final long serialVersionUID = -857073200964933536L;

    public UserEventMessage() {
    }

    public UserEventMessage(String externalId, DataType dataType, EventMessage.MessageType messageType) {
        super(externalId, dataType, messageType);
    }

    public UserEventMessage(String externalId, DataType dataType, EventMessage.MessageType messageType, Object payload) {
        super(externalId, dataType, messageType, payload);
    }

    public UserEventMessage(String externalId, DataType dataType, EventMessage.MessageType messageType, Long pluginConfigurationId) {
        super(externalId, dataType, messageType, pluginConfigurationId);
    }

    public UserEventMessage(String externalId, DataType dataType, EventMessage.MessageType messageType, Long pluginConfigurationId, Object payload) {
        super(externalId, dataType, messageType, pluginConfigurationId, payload);
    }

    public UserEventMessage(Long internalId, DataType dataType, EventMessage.MessageType messageType) {
        super(internalId, dataType, messageType);
    }

    public UserEventMessage(Long internalId, DataType dataType, EventMessage.MessageType messageType, Long pluginConfigurationId) {
        super(internalId, dataType, messageType, pluginConfigurationId);
    }

    public UserEventMessage(Long internalId, DataType dataType, EventMessage.MessageType messageType, Long pluginConfigurationId, Object payload) {
        super(internalId, dataType, messageType, pluginConfigurationId, payload);
    }

    /**
     * A payload for a user change event (to be used by the plugin system).
     * 
     * @author Pierre-Yves Cloux
     */
    public static class PayLoad {
        private String deletedUid;

        public String getDeletedUid() {
            return deletedUid;
        }

        public void setDeletedUid(String deletedUid) {
            this.deletedUid = deletedUid;
        }
    }
}
