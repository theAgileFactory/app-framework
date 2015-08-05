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
package models.framework_models.account;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.EnumMapping;

/**
 * An object which represents category for a notification.
 * 
 * @author Johann Kohler
 */
@Entity
public class NotificationCategory extends Model implements IModel {

    private static final long serialVersionUID = 5346731082197835073L;

    public static Finder<Long, NotificationCategory> find = new Finder<Long, NotificationCategory>(NotificationCategory.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.MEDIUM_STRING)
    public String name;

    @Column(length = IModelConstants.MEDIUM_STRING)
    public Code code;

    @Column(length = IModelConstants.MEDIUM_STRING)
    public String bootstrapGlyphicon;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "notificationCategory")
    public List<Notification> notifications;

    @Override
    public String audit() {
        return "NotificationCategory [id=" + id + ", name=" + name + ", boostrapGlyphicon=" + bootstrapGlyphicon + "]";
    }

    @Override
    public void defaults() {
    }

    @Override
    public void doDelete() {
        deleted = true;
        save();
    }

    public static NotificationCategory getByCode(Code code) {
        return find.where().eq("deleted", false).eq("code", code).findUnique();
    }

    @EnumMapping(nameValuePairs = "USER_MANAGEMENT=USER_MANAGEMENT, APPROVAL=APPROVAL, REQUEST_REVIEW=REQUEST_REVIEW, DOCUMENT=DOCUMENT, ISSUE=ISSUE,"
            + " AUDIT=AUDIT, PORTFOLIO_ENTRY=PORTFOLIO_ENTRY, TIMESHEET=TIMESHEET", integerType = false)
    public static enum Code {
        USER_MANAGEMENT, APPROVAL, REQUEST_REVIEW, DOCUMENT, ISSUE, AUDIT, PORTFOLIO_ENTRY, TIMESHEET;
    }

}
