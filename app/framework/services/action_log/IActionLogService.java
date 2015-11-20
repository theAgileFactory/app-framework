package framework.services.action_log;

import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;

import framework.commons.DataType;
import models.framework_models.common.ActionLog;

/**
 * The action log service interface.
 * 
 * @author Johann Kohler
 *
 */
public interface IActionLogService {

    /**
     * Create a new log for an object and an action.
     * 
     * @param dataType
     *            the data type
     * @param objectId
     *            the object id
     * @param action
     *            the action specific to the data type
     * @param parameters
     *            the parameters specific to the action
     */
    void log(DataType dataType, Long objectId, String action, PropertiesConfiguration parameters);

    /**
     * Get the list of logs for an object and an action.
     * 
     * @param dataType
     *            the data type
     * @param objectId
     *            the object id
     * @param action
     *            the action
     */
    List<ActionLog> getLogs(DataType dataType, Long objectId, String action);

    /**
     * Get the list of logs that occurred after a given date for an object and
     * an action.
     * 
     * @param dataType
     *            the data type
     * @param objectId
     *            the object id
     * @param action
     *            the action
     * @param fromDate
     *            the from date
     */
    List<ActionLog> getLogs(DataType dataType, Long objectId, String action, Date fromDate);

}
