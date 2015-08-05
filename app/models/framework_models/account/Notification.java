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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;

import com.avaje.ebean.Model;

import framework.utils.formats.DateType;

/**
 * An object which represents a notification targeting a {@link Principal}. A
 * notification is a message possibly associated with a link that can be clicked
 * by the user.
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class Notification extends Model implements IModel {
    private static final long serialVersionUID = 5333733082197835073L;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, Notification> find = new Finder<Long, Notification>(Notification.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @DateType
    public Date creationDate = new Date();

    public boolean isMessage;

    @ManyToOne(optional = true)
    public NotificationCategory notificationCategory;

    @Column(length = IModelConstants.MEDIUM_STRING)
    public String title;

    @Column(length = IModelConstants.LARGE_STRING, nullable = false)
    public String message;

    @Column(length = IModelConstants.LARGE_STRING)
    public String actionLink;

    @ManyToOne(optional = false)
    public Principal principal;

    @ManyToOne(optional = true)
    public Principal senderPrincipal;

    public boolean isRead;

    public Notification() {
    }

    @Override
    public String audit() {
        return "Notification [id=" + id + ", title=" + title + ", message=" + message + ", actionLink=" + actionLink + ", principal=" + principal + "]";
    }

    @Override
    public void defaults() {

    }

    @Override
    public void doDelete() {
        deleted = true;
        save();
    }

    @Override
    public String toString() {
        return "to: " + (principal != null ? principal.uid : "unknown") + " message: " + message;
    }

    public String getTitle() {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        return "";
    }
}
