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
package framework.services.notification;

import java.util.List;

import com.avaje.ebean.ExpressionList;

import models.framework_models.account.Notification;
import models.framework_models.account.NotificationCategory;
import models.framework_models.account.Principal;

/**
 * The interface to be implemented by the service which handles the notification
 * (see {@link Notification}).<br/>
 * This service offer two notification methods :
 * <ul>
 * <li>Simple : which notify one unique {@link Principal} and is acting
 * synchronously</li>
 * <li>Bulk : which notify several {@link Principal} and is acting
 * "asynchronously"</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
public interface INotificationManagerPlugin {

    /**
     * Return true if the sending system is INTERNAL.
     */
    public boolean isInternalSendingSystem();

    /**
     * Get the sending system.
     */
    public SendingSystem getSendingSystem();

    /**
     * Return true if the specified Principal (identified by its uid) has some
     * notifications.
     * 
     * @param uid
     *            a unique user login
     * 
     * @return a boolean
     */
    public boolean hasNotifications(String uid);

    /**
     * Return the number of not read notifications for the specified Principal.
     * 
     * @param uid
     *            a unique user login
     */
    public int nbNotReadNotifications(String uid);

    /**
     * Return true if the specified Principal (identified by its uid) has some
     * messages (notification with is_message flag equals to true).
     * 
     * @param uid
     *            a unique user login
     * 
     * @return a boolean
     */
    public boolean hasMessages(String uid);

    /**
     * Return the number of not read messages for the specified Principal.
     * 
     * @param uid
     *            a unique user login
     */
    public int nbNotReadMessages(String uid);

    /**
     * Return the notifications for the specified user.
     * 
     * The notifications are order by: isRead, creationDate DESC.
     * 
     * @param uid
     *            a unique user login
     * 
     * @return a list of Notification objects
     */
    public List<Notification> getNotificationsForUid(String uid);

    /**
     * Return the notifications for the specified user as an expression list.
     * 
     * The notifications are not ordered.
     * 
     * @param uid
     *            a unique user login
     * 
     * @return a list of Notification objects
     */
    public ExpressionList<Notification> getNotificationsForUidAsExpr(String uid);

    /**
     * Return the last 5 not read notifications for the specified user.
     * 
     * @param uid
     *            a unique user login
     * 
     * @return a list of Notification objects
     */
    public List<Notification> getNotReadNotificationsForUid(String uid);

    /**
     * Return the messages for the specified user.
     * 
     * @param uid
     *            a unique user login
     * 
     * @return a list of Notification objects
     */
    public List<Notification> getMessagesForUid(String uid);

    /**
     * Return the last 5 not read messages for the specified user.
     * 
     * @param uid
     *            a unique user login
     * 
     * @return a list of Notification objects
     */
    public List<Notification> getNotReadMessagesForUid(String uid);

    /**
     * Delete the notification which id is the one specified for the specified
     * user.<br/>
     * If the notification does not belong to the specified user, then return
     * false.
     * 
     * @param uid
     *            a unique user login
     * @param notificationId
     *            a notification id
     * @return false if something goes wrong
     */
    public boolean deleteNotificationsForUid(String uid, Long notificationId);

    /**
     * Send a notification to a specified principal.
     * 
     * @param uid
     *            the principal uid of the recipient
     * @param category
     *            the notification category
     * @param title
     *            the notification title
     * @param message
     *            the notification content
     * @param actionLink
     *            the action link
     */
    public void sendNotification(String uid, NotificationCategory category, String title, String message, String actionLink);

    /**
     * Send a notification to many principals.
     * 
     * @param uids
     *            the list of principal uid
     * @param category
     *            the notification category
     * @param title
     *            the notification title
     * @param message
     *            the notification content
     * @param actionLink
     *            the action link
     */
    public void sendNotification(List<String> uids, NotificationCategory category, String title, String message, String actionLink);

    /**
     * Send a notification to all principal that have a specified permission.
     * 
     * @param permissionName
     *            the permission name
     * @param category
     *            the notification category
     * @param title
     *            the notification title
     * @param message
     *            the notification content
     * @param actionLink
     *            the action link
     */
    public void sendNotificationWithPermission(String permissionName, NotificationCategory category, String title, String message, String actionLink);

    /**
     * Send a message to a specified principal.
     * 
     * @param senderUid
     *            the principal uid of the sender
     * @param uid
     *            the principal uid of the recipient
     * @param title
     *            the message title
     * @param message
     *            the message content
     */
    public void sendMessage(String senderUid, String uid, String title, String message);

    /**
     * Send a message to many principals.
     * 
     * @param senderUid
     *            the principal uid of the sender
     * @param uids
     *            the list of principal uid
     * @param title
     *            the message title
     * @param message
     *            the message content
     */
    public void sendMessage(String senderUid, List<String> uids, String title, String message);

    /**
     * The possible sending system.
     * 
     * @author Johann Kohler
     */
    public enum SendingSystem {

        INTERNAL("notification.sending_system.internal"), EMAIL("notification.sending_system.email");

        private String key;

        /**
         * Construct with the i18n key.
         * 
         * @param key
         *            the i18n key
         */
        SendingSystem(String key) {
            this.key = key;
        }

        /**
         * Get a sending system by key.
         * 
         * @param key
         *            the i18n key
         */
        public static SendingSystem getByKey(String key) {
            for (SendingSystem sendingSystem : SendingSystem.values()) {
                if (sendingSystem.key.equals(key)) {
                    return sendingSystem;
                }
            }
            throw new java.lang.IllegalArgumentException("No enum constant for key " + key);
        }
    }

}
