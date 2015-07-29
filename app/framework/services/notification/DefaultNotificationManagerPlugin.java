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
import java.util.ArrayList;
import java.util.List;

import models.framework_models.account.Notification;
import models.framework_models.account.NotificationCategory;
import models.framework_models.account.Principal;
import play.Logger;
import play.Play;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.UntypedActor;
import akka.japi.Function;
import akka.routing.RoundRobinPool;

import com.avaje.ebean.ExpressionList;

import framework.services.actor.IActorServiceLifecycleHook;

/**
 * Implementation of the {@link INotificationManagerPlugin} interface.<br/>
 * This one is implemented as an actor which means that the whole notification
 * processing is asynchronous.
 * 
 * @author Pierre-Yves Cloux
 */
public class DefaultNotificationManagerPlugin implements INotificationManagerPlugin, IActorServiceLifecycleHook {

    private static final String SUPERVISOR_ACTOR_NAME = "notification-router";
    public static final String DURATION = Play.application().configuration().getString("maf.actor.notification.retry.duration");
    public static final int MAX_RETRY_BEFORE_FAILURE = Play.application().configuration().getInt("maf.actor.notification.retry.number");
    private static SupervisorStrategy strategy = new OneForOneStrategy(MAX_RETRY_BEFORE_FAILURE, Duration.create(DURATION),
            new Function<Throwable, Directive>() {
                @Override
                public Directive apply(Throwable t) {
                    log.error("An notification processor reported an exception, retry", t);
                    return resume();
                }
            });
    private static Logger.ALogger log = Logger.of(DefaultNotificationManagerPlugin.class);
    private int poolSize;
    private ActorRef supervisorActor;

    public DefaultNotificationManagerPlugin(int poolSize) {
        this.poolSize = poolSize;
    }

    @Override
    public void createActors(ActorSystem actorSystem) {
        /*
         * this.supervisorActor = actorSystem.actorOf(
         * Props.create(NotificationMessageProcessingActor.class).withRouter(
         * new
         * SmallestMailboxRouter(getPoolSize()).withSupervisorStrategy(strategy
         * )), SUPERVISOR_ACTOR_NAME);
         */
        this.supervisorActor =
                actorSystem.actorOf(
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
        NotificationToSend notificationToSend = new NotificationToSend(uid, category, title, message, actionLink);
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

    private int getPoolSize() {
        return poolSize;
    }

    private ActorRef getSupervisorActor() {
        return supervisorActor;
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
                    principal.sendNotification(notificationToSend.getCategory(), notificationToSend.getTitle(), notificationToSend.getMessage(),
                            notificationToSend.getLink());
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
            }

            else {
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

        public NotificationToSend() {
        }

        public NotificationToSend(String uid, NotificationCategory category, String title, String message, String link) {
            this.uid = uid;
            this.category = category;
            this.title = title;
            this.message = message;
            this.link = link;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public NotificationCategory getCategory() {
            return category;
        }

        public void setCategory(NotificationCategory category) {
            this.category = category;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
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

        public MessageToSend() {
        }

        public MessageToSend(String senderUid, String uid, String title, String message) {
            this.senderUid = senderUid;
            this.uid = uid;
            this.title = title;
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSenderUid() {
            return senderUid;
        }

        public void setSenderUid(String senderUid) {
            this.senderUid = senderUid;
        }
    }
}
