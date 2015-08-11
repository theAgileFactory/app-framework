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
package framework.utils;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.ArrayUtils;

import play.Logger;
import play.Play;
import scala.concurrent.duration.Duration;
import framework.services.ServiceStaticAccessor;

/**
 * This class encapsulate the e-mail notification features. If the flag
 * "maf.email.simulation" is set to TRUE in the configuration, no e-mail is
 * effectively sent. A log is generated to the console with the content of the
 * mail.
 * 
 * @author Pierre-Yves Cloux
 */
public class EmailUtils {
    private static final boolean simulateEmailSending = Play.application().configuration().getBoolean("maf.email.simulation");
    private static Logger.ALogger log = Logger.of(EmailUtils.class);

    /**
     * Send an e-mail
     * 
     * @param subject
     *            the subject of the mail
     * @param from
     *            the sender of the mail
     * @param body
     *            the body of the message
     * @param to
     *            a table of recipients for this email
     */
    public static void sendEmail(final String subject, final String from, final String body, final String... to) {
        ServiceStaticAccessor.getSysAdminUtils().scheduleOnce(false, "SEND_MAIL", Duration.create(0, TimeUnit.MILLISECONDS), new Runnable() {
            @Override
            public void run() {
                sendEmailSynchronous(subject, from, body, to);
            }
        });
    }

    /**
     * Send an e-mail
     * 
     * @param subject
     *            the subject of the mail
     * @param from
     *            the sender of the mail
     * @param body
     *            the body of the message
     * @param to
     *            a table of recipients for this email
     */
    private static void sendEmailSynchronous(String subject, String from, String body, String... to) {
        if (!simulateEmailSending) {
            try {
                // Send a real e-mail
                Properties props = new Properties();
                props.put("mail.smtp.host", framework.utils.Utilities.getPreferenceElseConfigurationValue(Play.application().configuration(),
                        framework.commons.IFrameworkConstants.SMTP_HOST_PREFERENCE, "smtp.host"));
                props.put("mail.smtp.port", framework.utils.Utilities.getPreferenceElseConfigurationValueAsInteger(Play.application().configuration(),
                        framework.commons.IFrameworkConstants.SMTP_PORT_PREFERENCE, "smtp.port"));
                props.put("mail.smtp.starttls.enable", framework.utils.Utilities.getPreferenceElseConfigurationValueAsBoolean(Play.application()
                        .configuration(), framework.commons.IFrameworkConstants.SMTP_TLS_PREFERENCE, "smtp.tls"));
                props.put("mail.smtp.auth", "true");
                if (framework.utils.Utilities.getPreferenceElseConfigurationValueAsBoolean(Play.application().configuration(),
                        framework.commons.IFrameworkConstants.SMTP_SSL_PREFERENCE, "play.mailer.ssl")) {
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    props.put("mail.smtp.socketFactory.fallback", "false");
                }
                Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(framework.utils.Utilities.getPreferenceElseConfigurationValue(Play.application().configuration(),
                                framework.commons.IFrameworkConstants.SMTP_USER_PREFERENCE, "smtp.user"), framework.utils.Utilities
                                .getPreferenceElseConfigurationValue(Play.application().configuration(),
                                        framework.commons.IFrameworkConstants.SMTP_PASSWORD_PREFERENCE, "smtp.password"));
                    }
                });
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                for (String recipient : to) {
                    message.addRecipient(Message.RecipientType.TO, InternetAddress.parse(recipient)[0]);
                }
                message.setSubject(subject);
                message.setContent(body, "text/html");
                Transport.send(message);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Email sent to %s with body %s", (to != null && to.length != 00) ? to[0] : "null", body));
                }
            } catch (Exception e) {
                log.error("Unable to send an e-mail to " + ArrayUtils.toString(to), e);
            }
        } else {
            // Simulate sending an e-mail by dumping the mail content to the
            // console
            StringBuffer sb = new StringBuffer();
            sb.append("Subject: ").append(subject).append('\n');
            sb.append("From: ").append(from).append('\n');
            for (String toUnit : to) {
                sb.append("To: ").append(toUnit).append('\n');
            }
            sb.append("Body: ").append(body).append('\n');
            log.info(sb.toString());
        }
    }
}
