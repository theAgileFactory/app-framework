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

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import models.framework_models.parent.IModelConstants;
import play.Logger;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.Model;
import com.avaje.ebean.SqlUpdate;

import framework.commons.DataType;
import framework.commons.message.EventMessage;

/**
 * A pluginConfiguration log is a record of an association between an object in
 * a target system and a {@link DataObject}.<br/>
 * In principle is a pluginConfiguration log is in error this means that
 * something wrong happens and a subsequent action is supposed to update this
 * log positively. The type of event for a pluginConfiguration log can be any of
 * the type of {@link EventMessage} or START (error when the pluginConfiguration
 * was started)
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class PluginLog extends Model {
    private static Logger.ALogger log = Logger.of(PluginLog.class);
    private static final long serialVersionUID = 6250383399339895400L;
    private static final String EVENT_TYPE_START = "START";
    private static final String EVENT_TYPE_STOP = "STOP";

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, PluginLog> find = new Finder<Long, PluginLog>(PluginLog.class);

    @Id
    public Long id;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.MEDIUM_STRING, nullable = false)
    public String transactionId;

    @Column(length = IModelConstants.SMALL_STRING, nullable = false)
    public String event;

    @Column(length = IModelConstants.LARGE_STRING)
    public String dataType;

    public Long internalId;

    @Column(length = IModelConstants.LARGE_STRING)
    public String externalId;

    public boolean isError;

    @Column(length = IModelConstants.VLARGE_STRING)
    public String logMessage;

    @ManyToOne(optional = false)
    public PluginConfiguration pluginConfiguration;

    /**
     * Creates a plugin log for the start of the plugin
     * 
     * @param pluginConfigurationId
     *            the unique identifier for the plugin configuration
     * @param logMessage
     *            the error message
     * @param isError
     *            true if the message is an error message
     */
    public static void saveStartPluginLog(Long pluginConfigurationId, String logMessage, boolean isError) {
        savePluginLog(null, pluginConfigurationId, isError, EVENT_TYPE_START, logMessage, null, null, null);
    }

    /**
     * Creates a plugin log for the stop of the plugin
     * 
     * @param pluginConfigurationId
     *            the unique identifier for the plugin configuration
     * @param logMessage
     *            the error message
     * @param isError
     *            true if the message is an error message
     */
    public static void saveStopPluginLog(Long pluginConfigurationId, String logMessage, boolean isError) {
        savePluginLog(null, pluginConfigurationId, isError, EVENT_TYPE_STOP, logMessage, null, null, null);
    }

    /**
     * Creates a plugin log for a defined type of message event
     * 
     * @param transactionId
     *            the unique id of the transaction associated with the event
     *            handling
     * @param pluginConfigurationId
     *            the unique identifier for the plugin configuration
     * @param isError
     *            true if the log is an error
     * @param messageType
     *            the type of the event message handled by the
     *            pluginConfiguration when an error occured
     * @param logMessage
     *            the error message
     * @param dataType
     *            an internal BizDock data type
     * @param internalId
     *            the Id of internal BizDock object to which the transaction is
     *            associated
     * @param externalId
     *            the Id of the external (third party system) plugin to which
     *            the transaction is associated
     */
    public static void saveOnEventHandlingPluginLog(String transactionId, Long pluginConfigurationId, boolean isError, EventMessage.MessageType messageType,
            String logMessage, DataType dataType, Long internalId, String externalId) {
        String messageTypeName = null;
        if (messageType != null) {
            messageTypeName = messageType.name();
        } else {
            messageTypeName = "UNKNOWN";
        }
        savePluginLog(transactionId, pluginConfigurationId, isError, messageTypeName, logMessage, dataType, internalId, externalId);
    }

    /**
     * Creates a plugin log on a specific event associated with the plugin
     * 
     * @param transactionId
     *            the unique Id for the transaction
     * @param pluginConfigurationId
     *            the unique identifier for the plugin configuration
     * @param isError
     *            true if the log is an error
     * @param event
     *            the type of event
     * @param logMessage
     *            the error message
     * @param dataType
     *            an internal BizDock data type
     * @param internalId
     *            the Id of internal BizDock object to which the transaction is
     *            associated
     * @param externalId
     *            the Id of the external (third party system) plugin to which
     *            the transaction is associated
     */
    private static void savePluginLog(String transactionId, Long pluginConfigurationId, boolean isError, String event, String logMessage, DataType dataType,
            Long internalId, String externalId) {
        try {
            String sql = "insert into plugin_log (transaction_id, plugin_configuration_id, "
                    + "event, is_error, log_message, last_update, data_type, internal_id, external_id)"
                    + " values(:transaction_id,:pluginConfigurationId,:event,:is_error,"
                    + ":log_message, :last_update, :data_type, :internal_id, :external_id)";
            SqlUpdate update = Ebean.createSqlUpdate(sql);
            update.setParameter("transaction_id", transactionId);
            update.setParameter("pluginConfigurationId", pluginConfigurationId);
            update.setParameter("event", event);
            update.setParameter("is_error", isError);
            update.setParameter("log_message", logMessage != null ? logMessage.replace("\n", "<br/>") : null);
            update.setParameter("last_update", new Date());
            update.setParameter("data_type", dataType != null ? dataType.getDataTypeClassName() : null);
            update.setParameter("internal_id", internalId);
            update.setParameter("external_id", externalId);
            update.execute();
        } catch (Exception e) {
            log.warn("Unexpected exception", e);
        }
    }

    /**
     * Flush all log messages for the specified pluginConfiguration
     * 
     * @return the number of logs deleted
     */
    public static int flushPluginLog(Long pluginConfigurationId) {
        String sql = "delete from plugin_log where plugin_configuration_id=:plugin_configuration_id";
        SqlUpdate update = Ebean.createSqlUpdate(sql);
        update.setParameter("plugin_configuration_id", pluginConfigurationId);
        return Ebean.execute(update);
    }

    /**
     * Return all the plugin logs ordered by time (most recent first)
     * 
     * @param pluginConfigurationId
     * @return
     */
    public static ExpressionList<PluginLog> getAllPluginLogsForPluginConfigurationId(Long pluginConfigurationId) {
        return PluginLog.find.where().eq("pluginConfiguration.id", pluginConfigurationId);
    }

    /**
     * Return the plugin log associated with the specified id
     */
    public static PluginLog getPluginLogFromId(Long id) {
        return PluginLog.find.byId(id);
    }
}
