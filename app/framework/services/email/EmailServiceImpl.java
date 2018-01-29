package framework.services.email;

import framework.commons.IFrameworkConstants;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.database.IDatabaseDependencyService;
import framework.services.system.ISysAdminUtils;
import org.apache.commons.lang3.ArrayUtils;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Singleton
public class EmailServiceImpl implements IEmailService {
    private static Logger.ALogger log = Logger.of(EmailServiceImpl.class);
    private Configuration configuration;
    private IPreferenceManagerPlugin preferenceManagerPlugin;
    private ISysAdminUtils sysAdminUtils;
    private boolean simulateEmailSending;

    /**
     * Create a new EmailServiceImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param databaseDependencyService
     *            the service which secure the availability of the database
     */
    @Inject
    public EmailServiceImpl(ApplicationLifecycle lifecycle, Configuration configuration, IDatabaseDependencyService databaseDependencyService,
            IPreferenceManagerPlugin preferenceManagerPlugin, ISysAdminUtils sysAdminUtils) {
        log.info("SERVICE>>> EmailServiceImpl starting...");
        this.configuration = configuration;
        this.preferenceManagerPlugin = preferenceManagerPlugin;
        this.sysAdminUtils = sysAdminUtils;
        this.simulateEmailSending = getConfiguration().getBoolean("maf.email.simulation");
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> SysAdminUtilsImpl stopping...");
            log.info("SERVICE>>> SysAdminUtilsImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> EmailServiceImpl started...");
    }

    @Override
    public void sendEmail(final String subject, final String from, final String body, final String... to) {
        this.sendEmail(subject, from, body, to, null);
    }

    @Override
    public void sendEmail(final String subject, final String from, final String body, final String[] to, final String[] cc) {
        getSysAdminUtils().scheduleOnce(false, "SEND_MAIL", Duration.create(0, TimeUnit.MILLISECONDS), () -> sendEmailSynchronous(subject, from, body, to, cc));
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
    private void sendEmailSynchronous(String subject, String from, String body, String[] to, String[] cc) {

        // Check if static recipient is configured for testing purpose
        String configTo = getConfiguration().getString("maf.email.to");
        if (configTo != null) {
            to = new String[]{configTo};
        }

        if (!simulateEmailSending) {
            try {
                // Send a real e-mail
                Properties props = new Properties();
                props.put("mail.smtp.host", getPreferenceManagerPlugin()
                        .getPreferenceElseConfigurationValue(framework.commons.IFrameworkConstants.SMTP_HOST_PREFERENCE, "smtp.host"));
                props.put("mail.smtp.port", getPreferenceManagerPlugin()
                        .getPreferenceElseConfigurationValueAsInteger(framework.commons.IFrameworkConstants.SMTP_PORT_PREFERENCE, "smtp.port"));
                props.put("mail.smtp.starttls.enable", getPreferenceManagerPlugin()
                        .getPreferenceElseConfigurationValueAsBoolean(framework.commons.IFrameworkConstants.SMTP_TLS_PREFERENCE, "smtp.tls"));
                props.put("mail.smtp.auth", "true");
                Boolean sslPreference = getPreferenceManagerPlugin().getPreferenceElseConfigurationValueAsBoolean(IFrameworkConstants.SMTP_SSL_PREFERENCE,
                        "play.mailer.ssl");
                if (sslPreference != null && sslPreference) {
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    props.put("mail.smtp.socketFactory.fallback", "false");
                }
                Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                getPreferenceManagerPlugin().getPreferenceElseConfigurationValue(framework.commons.IFrameworkConstants.SMTP_USER_PREFERENCE,
                                        "smtp.user"),
                                getPreferenceManagerPlugin().getPreferenceElseConfigurationValue(
                                        framework.commons.IFrameworkConstants.SMTP_PASSWORD_PREFERENCE, "smtp.password"));
                    }
                });
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                for (String recipient : to) {
                    message.addRecipient(Message.RecipientType.TO, InternetAddress.parse(recipient)[0]);
                }
                if (cc != null) {
                    for (String recipient : cc) {
                        message.addRecipient(Message.RecipientType.CC, InternetAddress.parse(recipient)[0]);
                    }
                }
                message.setSubject(subject);
                message.setContent(body, "text/html; charset=utf-8");
                Transport.send(message);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Email sent to %s with body %s", (to.length != 0) ? to[0] : "null", body));
                }
            } catch (Exception e) {
                log.error("Unable to send an e-mail to " + ArrayUtils.toString(to), e);
            }
        } else {
            // Simulate sending an e-mail by dumping the mail content to the
            // console
            StringBuilder sb = new StringBuilder();
            sb.append("Subject: ").append(subject).append('\n');
            sb.append("From: ").append(from).append('\n');
            for (String toUnit : to) {
                sb.append("To: ").append(toUnit).append('\n');
            }
            for (String ccUnit : cc) {
                sb.append("Cc: ").append(ccUnit).append('\n');
            }
            sb.append("Body: ").append(body).append('\n');
            log.info(sb.toString());
        }
    }

    private Configuration getConfiguration() {
        return this.configuration;
    }

    private IPreferenceManagerPlugin getPreferenceManagerPlugin() {
        return this.preferenceManagerPlugin;
    }

    private ISysAdminUtils getSysAdminUtils() {
        return this.sysAdminUtils;
    }

}
