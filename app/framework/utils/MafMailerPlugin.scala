package framework.utils

import java.io.{File, FilterOutputStream, PrintStream}
import javax.mail.internet.InternetAddress

import org.apache.commons.mail._
import play.api._
import scala.collection.JavaConverters._

import play.api.libs.mailer._


/**
 * plugin implementation
 */
class MafMailerPlugin(app: play.api.Application) extends MailerPlugin {

  private lazy val mock = app.configuration.getBoolean("smtp.mock").getOrElse(false)

  private def getMailerInstance: MailerAPI = {
    if (mock) {
      MockMailer
    } else {
      val smtpHost = Option(framework.utils.Utilities.getPreferenceElseConfigurationValue(framework.commons.IFrameworkConstants.SMTP_HOST_PREFERENCE, "smtp.host")).getOrElse(throw new RuntimeException("smtp.host needs to be set in application.conf in order to use this plugin (or set smtp.mock to true)"))
      val smtpPort = Option(framework.utils.Utilities.getPreferenceElseConfigurationValueAsInteger(framework.commons.IFrameworkConstants.SMTP_PORT_PREFERENCE, "smtp.port")).getOrElse[Integer](25)
      val smtpSsl = Option(framework.utils.Utilities.getPreferenceElseConfigurationValueAsBoolean(framework.commons.IFrameworkConstants.SMTP_SSL_PREFERENCE, "smtp.ssl")).getOrElse[java.lang.Boolean](false)
      val smtpTls = Option(framework.utils.Utilities.getPreferenceElseConfigurationValueAsBoolean(framework.commons.IFrameworkConstants.SMTP_TLS_PREFERENCE, "smtp.tls")).getOrElse[java.lang.Boolean](false)
      val smtpUser = Option(framework.utils.Utilities.getPreferenceElseConfigurationValue(framework.commons.IFrameworkConstants.SMTP_USER_PREFERENCE, "smtp.user"))
      val smtpPassword = Option(framework.utils.Utilities.getPreferenceElseConfigurationValue(framework.commons.IFrameworkConstants.SMTP_PASSWORD_PREFERENCE, "smtp.password"))
      val debugMode = app.configuration.getBoolean("smtp.debug").getOrElse(false)
      val smtpTimeout = app.configuration.getInt("smtp.timeout")
      val smtpConnectionTimeout = app.configuration.getInt("smtp.connectiontimeout")
      new CommonsMailer(smtpHost, smtpPort, smtpSsl, smtpTls, smtpUser, smtpPassword, debugMode, smtpTimeout, smtpConnectionTimeout) {
        override def send(email: MultiPartEmail): String = email.send()
        override def createMultiPartEmail(): MultiPartEmail = new MultiPartEmail()
        override def createHtmlEmail(): HtmlEmail = new HtmlEmail()
      }
    }
  }

  var mailerInstance : MailerAPI = null

  override lazy val enabled = !app.configuration.getString("apachecommonsmailerplugin").filter(_ == "disabled").isDefined

  override def onStart() {
  }
  
  def reset {
    mailerInstance = null;
  }
  
  def instance: MailerAPI = {
    if (mailerInstance == null) {
      mailerInstance = getMailerInstance
    }
    mailerInstance
  }
}
