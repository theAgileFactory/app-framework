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

import java.io.Serializable;
import java.util.UUID;

import framework.commons.DataType;
import framework.services.account.IUserAccount;

/**
 * A provisioning message is a message to be used by the actors linked with the
 * provisioning module connectors.<br/>
 * Here are the attributes of a message:
 * <ul>
 * <li>transactionId : a unique transaction id (it is generated when the message
 * is created)</li>
 * <li>externalId : the unique id of the "external" object which the message
 * received from the IN interface is altering.</li>
 * The nature of this unique id vary depending on the interface through which is
 * sent the message.
 * <li>internalId : the unique id of the "internal" object which the message
 * received from the OUT interface is altering.</li>
 * <li>dataType : the data type which is associated with the id</i>
 * <li>messageType : the type of message (see enumeration description)</li>
 * <li>pluginConfigurationId : required for CUSTOM messages, this is the ID of
 * the plugin to be specifically notified</li>
 * <li>payload : a {@link Serializable} object instance to qualify the event
 * (not mandatory)</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
public class EventMessage implements Serializable {
    public static final String USERACCOUNT_OBJECT_TYPE = IUserAccount.class.getName();

    private static final long serialVersionUID = -6233807889244812016L;
    private String transactionId;
    private Long internalId;
    private String externalId;
    private DataType dataType;
    private MessageType messageType;
    private Long pluginConfigurationId;
    private Object payload;

    /**
     * The possible types of provisioning messages:
     * <ul>
     * <li>OBJECT_CREATED : the specified object has just been created</li>
     * <li>OBJECT_DELETED : the specified object has been deleted</li>
     * <li>OBJECT_UPDATED : the specified object has been updated</li>
     * <li>OBJECT_STATUS_CHANGED : the specified has its status changed
     * (example: locked/unlocked for an account)</li>
     * <li>RESYNC : attempt to recover a provisioning issue by resync</li>
     * <li>CUSTOM : a custom message (in such case the "payload" attribute is
     * not null and should specify some details about the event)</li>
     * </ul>
     * 
     * @author Pierre-Yves Cloux
     */
    public static enum MessageType {
        OBJECT_CREATED, OBJECT_DELETED, OBJECT_UPDATED, OBJECT_STATUS_CHANGED, RESYNC, CUSTOM
    }

    public EventMessage() {
        this.transactionId = UUID.randomUUID().toString();
        this.messageType = MessageType.CUSTOM;
    }

    public EventMessage(String externalId, DataType dataType, MessageType messageType) {
        this();
        this.externalId = externalId;
        this.dataType = dataType;
        this.messageType = messageType;
    }

    public EventMessage(String externalId, DataType dataType, MessageType messageType, Object payload) {
        this();
        this.externalId = externalId;
        this.dataType = dataType;
        this.messageType = messageType;
        this.payload = payload;
    }

    public EventMessage(String externalId, DataType dataType, MessageType messageType, Long pluginConfigurationId) {
        this(externalId, dataType, messageType);
        this.pluginConfigurationId = pluginConfigurationId;
    }

    public EventMessage(String externalId, DataType dataType, MessageType messageType, Long pluginConfigurationId, Object payload) {
        this(externalId, dataType, messageType, pluginConfigurationId);
        this.payload = payload;
    }

    public EventMessage(Long internalId, DataType dataType, MessageType messageType) {
        this();
        this.internalId = internalId;
        this.dataType = dataType;
        this.messageType = messageType;
    }

    public EventMessage(Long internalId, DataType dataType, MessageType messageType, Long pluginConfigurationId) {
        this(internalId, dataType, messageType);
        this.pluginConfigurationId = pluginConfigurationId;
    }

    public EventMessage(Long internalId, DataType dataType, MessageType messageType, Long pluginConfigurationId, Object payload) {
        this(internalId, dataType, messageType, pluginConfigurationId);
        this.payload = payload;
    }

    /**
     * Return true if the message is consistent, meaning that if the message is
     * not CUSTOM:
     * <ul>
     * <li>has a non null externalId</li>
     * <li>has a non null dataType</li>
     * <li>has a valid message type</li>
     * </ul>
     * 
     * @return a boolean
     */
    public boolean isConsistent() {
        if (!getMessageType().equals(MessageType.CUSTOM)) {
            return (this.externalId != null || this.internalId != null) && this.dataType != null && this.messageType != null;
        }
        return true;
    }

    /**
     * Return a resync provisioning message which is a clone from the current
     * message (except for the UUID and the {@link MessageType})
     * 
     * @return a new {@link EventMessage}
     */
    public EventMessage getResyncProvisioningMessage() {
        EventMessage resyncProvisioningMessage = null;
        if (getExternalId() == null) {
            resyncProvisioningMessage = new EventMessage(getInternalId(), getDataType(), MessageType.RESYNC);
        } else {
            resyncProvisioningMessage = new EventMessage(getExternalId(), getDataType(), MessageType.RESYNC);
        }
        return resyncProvisioningMessage;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String uniqueId) {
        this.externalId = uniqueId;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Long getPluginConfigurationId() {
        return pluginConfigurationId;
    }

    public void setPluginConfigurationId(Long pluginId) {
        this.pluginConfigurationId = pluginId;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Long getInternalId() {
        return internalId;
    }

    public void setInternalId(Long internalId) {
        this.internalId = internalId;
    }

    @Override
    public String toString() {
        return "EventMessage [transactionId=" + transactionId + ", internalId=" + internalId + ", externalId=" + externalId + ", dataType=" + dataType
                + ", messageType=" + messageType + ", pluginConfigurationId=" + pluginConfigurationId + ", payload=" + payload + "]";
    }
}
