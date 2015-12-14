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
package models.framework_models.scheduler;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.SqlUpdate;

import models.framework_models.parent.IModelConstants;
import play.Configuration;

/**
 * An object which stores the status of an exclusive scheduled action. Exclusive
 * means that, at a defined time, only one instance of the action can run for a
 * MAF instance.
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class SchedulerState extends Model {
    private static final long serialVersionUID = 535370303541384294L;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, SchedulerState> find = new Finder<Long, SchedulerState>(SchedulerState.class);

    @Id
    public Long id;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING, nullable = false)
    public String actionUuid;

    @Column(length = IModelConstants.MEDIUM_STRING, nullable = false)
    public String transactionId;

    public boolean isRunning;

    /**
     * Return the SchedulerState associated with the specified actionUuid.<br/>
     * In principle, only one actionUuid can run at a time.
     */
    public static SchedulerState getRunningSchedulerStateFromActionUuid(String actionUuid) {
        return SchedulerState.find.where().eq("actionUuid", actionUuid).eq("isRunning", true).findUnique();
    }

    /**
     * Flush the scheduler states which are older than 24 hours.<br/>
     * If the process is running it is removed from the database anyway.
     * 
     * @param configuration
     *            the application configuration
     */
    public static int flushOldStates(Configuration configuration) {
        int hours = configuration.getInt("maf.flush.scheduler.states.interval");
        String sql = "delete from scheduler_state where last_update < DATE_SUB(NOW(), INTERVAL " + hours + " HOUR)";
        SqlUpdate update = Ebean.createSqlUpdate(sql);
        return Ebean.execute(update);
    }

    /**
     * Flush all the scheduler states.<br/>
     * WARNING: use with care.
     */
    public static int flushAllStates() {
        String sql = "delete from scheduler_state";
        SqlUpdate update = Ebean.createSqlUpdate(sql);
        return Ebean.execute(update);
    }
}