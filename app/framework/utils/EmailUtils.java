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

import java.util.concurrent.TimeUnit;

import play.Logger;
import play.Play;
import play.api.libs.mailer.MailerAPI;
import play.libs.mailer.Email;
import scala.concurrent.duration.Duration;

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
        SysAdminUtils.scheduleOnce(false, "SEND_MAIL", Duration.create(0, TimeUnit.MILLISECONDS), new Runnable() {
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

            MailerAPI mailerApi = Play.application().plugin(framework.utils.MafMailerPlugin.class).instance();

            Email email = new Email();
            email.setSubject(subject);
            email.setFrom(from);
            for (int i = 0; i < to.length; i++) {
                email.addTo(to[i]);
            }
            email.setBodyHtml(body);

            mailerApi.send(email);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Email sent to %s with body %s", (to != null && to.length != 00) ? to[0] : "null", body));
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
