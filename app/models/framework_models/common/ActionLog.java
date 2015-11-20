package models.framework_models.common;

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.avaje.ebean.Model;

import models.framework_models.account.Principal;
import models.framework_models.parent.IModelConstants;
import play.Logger;

/**
 * An action log is represents a user action that has been logged. The scope of
 * the log is depending of the action itself.
 * 
 * @author Johann Kohler
 *
 */
@Entity
public class ActionLog extends Model {

    public static Finder<Long, ActionLog> find = new Finder<Long, ActionLog>(ActionLog.class);

    @Id
    public Long id;

    @Version
    public Timestamp creationDate;

    @ManyToOne(cascade = CascadeType.ALL)
    public Principal principal;

    @Column(length = IModelConstants.LARGE_STRING, nullable = false)
    public String objectType;

    @Column(nullable = false)
    public Long objectId;

    @Column(length = IModelConstants.MEDIUM_STRING, nullable = false)
    public String action;

    public String parameters;

    /**
     * Get the parameters as a properties configuration.
     */
    public PropertiesConfiguration getParametersAsProperties() {
        if (this.parameters != null) {
            PropertiesConfiguration properties = new PropertiesConfiguration();
            try {
                properties.load(new ByteArrayInputStream(this.parameters.getBytes()));
                return properties;
            } catch (Exception e) {
                Logger.error("Impossible to read the parameters for the action log " + this.id, e);
            }
        }
        return null;
    }

    /**
     * Default constructor.
     */
    public ActionLog() {
    }

    /**
     * Get the action logs of an object for an action.
     * 
     * @param objectType
     *            the object type
     * @param objectId
     *            the object id
     * @param action
     *            the action
     */
    public static List<ActionLog> getActionLogAsListByObjectAndAction(String objectType, Long objectId, String action) {
        return find.where().eq("objectType", objectType).eq("objectId", objectId).eq("action", action).findList();
    }

    /**
     * Get the action logs that occurred after a given date of an object for an
     * action.
     * 
     * @param objectType
     *            the object type
     * @param objectId
     *            the object id
     * @param action
     *            the action
     * @param fromDate
     *            the from date
     */
    public static List<ActionLog> getActionLogAsListByObjectAndAction(String objectType, Long objectId, String action, Date fromDate) {
        return find.where().eq("objectType", objectType).eq("objectId", objectId).eq("action", action).ge("timestamp", fromDate).findList();
    }

}
