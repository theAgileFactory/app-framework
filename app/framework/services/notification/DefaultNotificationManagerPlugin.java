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

import static akka.actor.SupervisorStrategy.resume;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.avaje.ebean.ExpressionList;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.UntypedActor;
import akka.japi.Function;
import akka.routing.RoundRobinPool;
import framework.commons.IFrameworkConstants;
import framework.services.account.IAccountManagerPlugin;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.account.IUserAccount;
import framework.utils.EmailUtils;
import framework.utils.Utilities;
import models.framework_models.account.Notification;
import models.framework_models.account.NotificationCategory;
import models.framework_models.account.Principal;
import play.Configuration;
import play.Logger;
import play.Play;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import scala.concurrent.duration.Duration;

/**
 * Implementation of the {@link INotificationManagerPlugin} interface.<br/>
 * This one is implemented as an actor which means that the whole notification
 * processing is asynchronous.
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class DefaultNotificationManagerPlugin implements INotificationManagerPlugin {

    private static final String SUPERVISOR_ACTOR_NAME = "notification-router";
    private static Logger.ALogger log = Logger.of(DefaultNotificationManagerPlugin.class);

    /**
     * The supervisor strategy.
     * 
     * @param notificationRetryNumber
     *            Number of retry when a delivery failed.
     * @param notificationRetryDuration
     *            How long to wait before attempting to distribute the message
     *            again.
     */
    private static SupervisorStrategy getSupervisorStrategy(int notificationRetryNumber, String notificationRetryDuration) {
        return new OneForOneStrategy(notificationRetryNumber, Duration.create(notificationRetryDuration), new Function<Throwable, Directive>() {
            @Override
            public Directive apply(Throwable t) {
                log.error("An notification processor reported an exception, retry", t);
                return resume();
            }
        });
    }

    /**
     * The size of the actor pool which is managing the notifications.
     */
    private int poolSize;

    private String notificationRetryDuration;

    private int notificationRetryNumber;

    private IPreferenceManagerPlugin preferenceManagerPlugin;

    private IAccountManagerPlugin accountManagerPlugin;

    private ActorRef supervisorActor;

    /**
     * The service configurations.
     * 
     * @author Pierre-Yves Cloux
     */
    public enum Config {
        NOTIFICATION_POOL_SIZE("maf.actor.notification.pool"), RETRY_DURATION("maf.actor.notification.retry.duration"), RETRY_NUMBER(
                "maf.actor.notification.retry.number");

        private String configurationKey;

        /**
         * Construct with the configuration key.
         * 
         * @param configurationKey
         *            the configuration key
         */
        private Config(String configurationKey) {
            this.configurationKey = configurationKey;
        }

        /**
         * Get the configuration key.
         */
        public String getConfigurationKey() {
            return configurationKey;
        }
    }

    /**
     * Create a new DefaultNotificationManagerPlugin.
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param preferenceManagerPlugin
     *            the preference manager service
     * @param accountManagerPlugin
     *            the account manager service
     * 
     */
    @Inject
    public DefaultNotificationManagerPlugin(ApplicationLifecycle lifecycle, Configuration configuration, IPreferenceManagerPlugin preferenceManagerPlugin,
            IAccountManagerPlugin accountManagerPlugin) {
        log.info("SERVICE>>> DefaultNotificationManagerPlugin starting...");
        this.poolSize = configuration.getInt(Config.NOTIFICATION_POOL_SIZE.getConfigurationKey());
        this.notificationRetryDuration = configuration.getString(Config.RETRY_DURATION.getConfigurationKey());
        this.notificationRetryNumber = configuration.getInt(Config.RETRY_NUMBER.getConfigurationKey());
        this.preferenceManagerPlugin = preferenceManagerPlugin;
        this.accountManagerPlugin = accountManagerPlugin;
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> DefaultNotificationManagerPlugin stopping...");
            log.info("SERVICE>>> DefaultNotificationManagerPlugin stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> DefaultNotificationManagerPlugin started");
    }

    @Override
    public boolean isInternalSendingSystem() {
        return this.getSendingSystem().equals(SendingSystem.INTERNAL);
    }

    @Override
    public SendingSystem getSendingSystem() {
        return SendingSystem.getByKey(getPreferenceManagerPlugin().getPreferenceValueAsString(IFrameworkConstants.NOTIFICATION_SENDING_SYSTEM_PREFERENCE));
    }

    @Override
    public void createActors(ActorSystem actorSystem) {
        SupervisorStrategy strategy = getSupervisorStrategy(getNotificationRetryNumber(), getNotificationRetryDuration());
        this.supervisorActor = actorSystem.actorOf(
                (new RoundRobinPool(getPoolSize())).withSupervisorStrategy(strategy).props(Props.create(NotificationMessageProcessingActor.class)),
                SUPERVISOR_ACTOR_NAME);
        log.info("Actor based notification system is started");
    }

    @Override
    public boolean hasNotifications(String uid) {
        Principal principal = Principal.getPrincipalFromUid(uid);
        if (principal != null) {
            return principal.hasNotifications();
        } else {
            log.error(String.format("Check notifications for user %s failed because this user does not exists", uid));
        }
        return false;
    }

    @Override
    public int nbNotReadNotifications(String uid) {
        Principal principal = Principal.getPrincipalFromUid(uid);
        if (principal != null) {
            return principal.nbNotReadNotifications();
        } else {
            log.error(String.format("get the number of not read notifications for user %s failed because this user does not exists", uid));
        }
        return 0;
    }

    @Override
    public boolean hasMessages(String uid) {
        Principal principal = Principal.getPrincipalFromUid(uid);
        if (principal != null) {
            return principal.hasMessages();
        } else {
            log.error(String.format("Check messages for user %s failed because this user does not exists", uid));
        }
        return false;
    }

    @Override
    public int nbNotReadMessages(String uid) {
        Principal principal = Principal.getPrincipalFromUid(uid);
        if (principal != null) {
            return principal.nbNotReadMessages();
        } else {
            log.error(String.format("get the number of not read messages for user %s failed because this user does not exists", uid));
        }
        return 0;
    }

    @Override
    public List<Notification> getNotificationsForUid(String uid) {
        Principal principal = Principal.getPrincipalFromUid(uid);
        if (principal != null) {
            if (principal.hasNotifications()) {
                return principal.getNotifications();
            }
        } else {
            log.error(String.format("Get notifications for user %s failed because this user does not exists", uid));
        }
        return new ArrayList<Notification>();
    }

    @Override
    public ExpressionList<Notification> getNotificationsForUidAsExpr(String uid) {
        Principal principal = Principal.getPrincipalFromUid(uid);
        if (principal != null) {
            return principal.getNotificationsAsExpr();
        }
        return null;
    }

    @Override
    public List<Notification> getNotReadNotificationsForUid(String uid) {
        Principal principal = Principal.getPrincipalFromUid(uid);
        if (principal != null) {
            if (principal.hasNotifications()) {
                return principal.getNotReadNotifications();
            }
        } else {
            log.error(String.format("Get not read notifications for user %s failed because this user does not exists", uid));
        }
        return new ArrayList<Notification>();
    }

    @Override
    public List<Notification> getMessagesForUid(String uid) {
        Principal principal = Principal.getPrincipalFromUid(uid);
        if (principal != null) {
            if (principal.hasMessages()) {
                return principal.getMessages();
            }
        } else {
            log.error(String.format("Get messages for user %s failed because this user does not exists", uid));
        }
        return new ArrayList<Notification>();
    }

    @Override
    public List<Notification> getNotReadMessagesForUid(String uid) {
        Principal principal = Principal.getPrincipalFromUid(uid);
        if (principal != null) {
            if (principal.hasMessages()) {
                return principal.getNotReadMessages();
            }
        } else {
            log.error(String.format("Get not read messages for user %s failed because this user does not exists", uid));
        }
        return new ArrayList<Notification>();
    }

    @Override
    public boolean deleteNotificationsForUid(String uid, Long notificationId) {
        Principal principal = Principal.getPrincipalFromUid(uid);
        if (principal != null) {
            return principal.deleteNotification(notificationId);
        } else {
            log.error(String.format("Attempt to delete notification for user %s failed because this user does not exists", uid));
        }
        return false;
    }

    @Override
    public void sendNotification(String uid, NotificationCategory category, String title, String message, String actionLink) {
        NotificationToSend notificationToSend = new NotificationToSend(this.getAccountManagerPlugin(), this.getSendingSystem(), uid, category, title, message,
                actionLink);
        getSupervisorActor().tell(notificationToSend, ActorRef.noSender());
    }

    @Override
    public void sendNotification(List<String> uids, NotificationCategory category, String title, String message, String actionLink) {
        for (String uid : uids) {
            sendNotification(uid, category, title, message, actionLink);
        }
    }

    @Override
    public void sendNotificationWithPermission(String permissionName, NotificationCategory category, String title, String message, String actionLink) {
        ArrayList<String> uids = new ArrayList<String>();
        List<Principal> principals = Principal.getPrincipalsWithPermission(permissionName);
        if (principals != null) {
            for (Principal principal : principals) {
                uids.add(principal.uid);
            }
            sendNotification(uids, category, title, message, actionLink);
        }
    }

    @Override
    public void sendMessage(String senderUid, String uid, String title, String message) {
        MessageToSend messageToSend = new MessageToSend(senderUid, uid, title, message);
        getSupervisorActor().tell(messageToSend, ActorRef.noSender());
    }

    @Override
    public void sendMessage(String senderUid, List<String> uids, String title, String message) {
        for (String uid : uids) {
            sendMessage(senderUid, uid, title, message);
        }
    }

    /**
     * Get the pool size.
     * 
     */
    private int getPoolSize() {
        return poolSize;
    }

    /**
     * Get the supervisor actor.
     */
    private ActorRef getSupervisorActor() {
        return supervisorActor;
    }

    /**
     * Get the notification retry duration.
     */
    private String getNotificationRetryDuration() {
        return notificationRetryDuration;
    }

    /**
     * Get the notification retry number.
     */
    private int getNotificationRetryNumber() {
        return notificationRetryNumber;
    }

    /**
     * Get the preference manager service.
     */
    private IPreferenceManagerPlugin getPreferenceManagerPlugin() {
        return preferenceManagerPlugin;
    }

    /**
     * Get the account manager service.
     */
    private IAccountManagerPlugin getAccountManagerPlugin() {
        return accountManagerPlugin;
    }

    /**
     * A class which write a notification for a certain uid.<br/>
     * This class is an actor which is called by the
     * {@link DefaultNotificationManagerPlugin} which is its "parent".
     * 
     * @author Pierre-Yves Cloux
     */
    public static class NotificationMessageProcessingActor extends UntypedActor {

        @Override
        public void postStop() throws Exception {
            super.postStop();
            log.info(String.format("Stopping notification processor actor [%s]", getSelf().path().toString()));
        }

        @Override
        public void preStart() throws Exception {
            super.preStart();
            log.info(String.format("Starting notification processor actor [%s]", getSelf().path().toString()));
        }

        @Override
        public void onReceive(Object message) throws Exception {
            if (message instanceof NotificationToSend) {
                NotificationToSend notificationToSend = (NotificationToSend) message;

                Principal principal = Principal.getPrincipalFromUid(notificationToSend.getUid());
                if (principal != null) {
                    switch (notificationToSend.getSendingSystem()) {

                    case EMAIL:

                        IUserAccount userAccount = notificationToSend.getAccountManagerPlugin().getUserAccountFromUid(notificationToSend.getUid());

                        // construct the full link if it exists
                        String link = null;
                        if (notificationToSend.getLink() != null && !notificationToSend.getLink().equals("")) {
                            try {
                                URL url = new URL(notificationToSend.getLink());
                                link = url.toString();
                            } catch (Exception e) {
                                link = Utilities.getPreferenceElseConfigurationValue(Play.application().configuration(),
                                        IFrameworkConstants.PUBLIC_URL_PREFERENCE, "maf.public.url") + notificationToSend.getLink();
                            }
                        }

                        EmailUtils.sendEmail("BizDock - " + notificationToSend.getTitle(), play.Configuration.root().getString("maf.email.from"),
                                Utilities.renderFullViewI18n("views.html.framework_views.parts.mail.notification_html", userAccount.getFirstName(),
                                        notificationToSend.getMessage(), link).body(),
                                userAccount.getMail());

                        break;

                    case INTERNAL:

                        principal.sendNotification(notificationToSend.getCategory(), notificationToSend.getTitle(), notificationToSend.getMessage(),
                                notificationToSend.getLink());

                        break;

                    default:
                        break;
                    }

                } else {
                    log.error(String.format("Notification with title %s for user %s failed because this user does not exists", notificationToSend.getTitle(),
                            notificationToSend.getUid()));
                }

            } else if (message instanceof MessageToSend) {
                MessageToSend messageToSend = (MessageToSend) message;
                Principal principal = Principal.getPrincipalFromUid(messageToSend.getUid());
                if (principal != null) {
                    principal.sendMessage(messageToSend.getSenderUid(), messageToSend.getTitle(), messageToSend.getMessage());
                } else {
                    log.error(String.format("Message with title %s for user %s failed because this user does not exists", messageToSend.getTitle(),
                            messageToSend.getUid()));
                }
            } else {
                unhandled(message);
            }
        }
    }

    /**
     * A notification to send.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class NotificationToSend implements Serializable {

        private static final long serialVersionUID = 3050491086312130763L;

        private String uid;
        private NotificationCategory category;
        private String title;
        private String message;
        private String link;
        private SendingSystem sendingSystem;
        private IAccountManagerPlugin accountManagerPlugin;

        /**
         * Default constructor.
         */
        public NotificationToSend() {
        }

        /**
         * Construct with values.
         * 
         * @param accountManagerPlugin
         *            the account manager service
         * @param sendingSystem
         *            the sending system
         * @param uid
         *            the uid of the recipient
         * @param category
         *            the notification category
         * @param title
         *            the notification title
         * @param message
         *            the notification message
         * @param link
         *            the notification link
         */
        public NotificationToSend(IAccountManagerPlugin accountManagerPlugin, SendingSystem sendingSystem, String uid, NotificationCategory category,
                String title, String message, String link) {
            this.accountManagerPlugin = accountManagerPlugin;
            this.sendingSystem = sendingSystem;
            this.uid = uid;
            this.category = category;
            this.title = title;
            this.message = message;
            this.link = link;
        }

        /**
         * Get the account manager service.
         */
        public IAccountManagerPlugin getAccountManagerPlugin() {
            return accountManagerPlugin;
        }

        /**
         * Get the sending system.
         */
        public SendingSystem getSendingSystem() {
            return this.sendingSystem;
        }

        /**
         * Get the message.
         */
        public String getMessage() {
            return message;
        }

        /**
         * Get the link.
         */
        public String getLink() {
            return link;
        }

        /**
         * Get the uid.
         */
        public String getUid() {
            return uid;
        }

        /**
         * Get the category.
         */
        public NotificationCategory getCategory() {
            return category;
        }

        /**
         * Get the title.
         */
        public String getTitle() {
            return title;
        }

    }

    /**
     * A message to send.
     * 
     * @author Johann Kohler
     */
    public static class MessageToSend implements Serializable {

        private static final long serialVersionUID = 3041129108444130763L;

        private String senderUid;
        private String uid;
        private String title;
        private String message;

        /**
         * Default constructor.
         */
        public MessageToSend() {
        }

        /**
         * Construct with values.
         * 
         * @param senderUid
         *            the uid of the send
         * @param uid
         *            the uid of the recipient
         * @param title
         *            the message title
         * @param message
         *            the message content
         */
        public MessageToSend(String senderUid, String uid, String title, String message) {
            this.senderUid = senderUid;
            this.uid = uid;
            this.title = title;
            this.message = message;
        }

        /**
         * Get the message content.
         */
        public String getMessage() {
            return message;
        }

        /**
         * Get the uid of the recipient.
         */
        public String getUid() {
            return uid;
        }

        /**
         * Get the the message title.
         */
        public String getTitle() {
            return title;
        }

        /**
         * Get the uid of the sender.
         */
        public String getSenderUid() {
            return senderUid;
        }

    }
}
