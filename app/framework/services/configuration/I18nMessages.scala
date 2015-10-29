package framework.services.configuration;

import scala.collection.JavaConverters._
import play.api.i18n._
import javax.inject.{ Inject, Singleton }
import play.Logger

trait I18nMessages {

    /**
     * Get all the authorized i18n keys that could be edited
     */
    def getAuthorizedKeys: java.util.List[String]

}

@Singleton
class DefaultI18nMessages @Inject() (messagesApi: MessagesApi) extends I18nMessages {

    val authorizedStartPatterns = "idzone.messages." :: "idzone.notifications." :: "topmenubar." :: "kpi." :: "report." :: "object." :: "index." :: "messaging." :: "notifications." :: "my." :: "core." :: "admin." :: Nil

    val getAuthorizedKeys = {
        var list:List[String] = Nil
        val (language, values) = messagesApi.messages.head
    
        values.foreach { case(key, value) => 
            if(authorizedStartPatterns.exists(key.startsWith)) {
                list = list ::: List(key)
            }
        }

        list.sorted.asJava
    }
}