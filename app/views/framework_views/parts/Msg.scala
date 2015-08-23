package views.html.framework_views.parts

import play.twirl.api._;
import commons._;

/**
 * An alternative class to Messages
 */
object Msg {
	/**
	 * Default implementation is automatically "escaped"
	 */
	def apply(key: String, values: Object*): Html = {
		Html(_messagesPluginService.get(key, values: _*));
	}
	/**
	 * Using this method, the returned value is a String
	 */
	def asString(key: String, values: Object*): String = {
		_messagesPluginService.get(key, values: _*);
	}
}