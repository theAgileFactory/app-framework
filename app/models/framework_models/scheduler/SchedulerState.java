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

/**
 * An object which stores the status of execution of recurring jobs.
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
     * Return the SchedulerState associated with the specified transactionId.
     * <br/>
     */
    public static SchedulerState getRunningSchedulerStateFromTransactionId(String transactionId) {
        return SchedulerState.find.where().eq("transactionId", transactionId).eq("isRunning", true).findUnique();
    }

    /**
     * Flush the scheduler states which are older than 24 hours.<br/>
     * If the process is running it is removed from the database anyway.
     * 
     * @param hours
     *            number of hours
     */
    public static int flushOldStates(int hours) {
        String sql = "delete from scheduler_state where last_update < DATE_SUB(NOW(), INTERVAL " + hours + " HOUR)";
        SqlUpdate update = Ebean.createSqlUpdate(sql);
        return Ebean.execute(update);
    }
}