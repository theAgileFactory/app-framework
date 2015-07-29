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
package framework.services.configuration;

import play.i18n.Lang;
import play.i18n.Messages;

/**
 * Interface to be implemented by the service which manages the I18Messages.<br/>
 * This plugin is to replace and extends the standard play {@link Messages}
 * class There are two sources of I18n messages:
 * <ul>
 * <li><b>The "messages.[language]" files</b> : These are the messages which are
 * "standard" not specific from a customer</li>
 * <li><b>The "content" references</b>: which are references to big content
 * blocks loaded at runtime (complete pages)<br/>
 * <b>WARNING</b> : these content keys must ends with "_content" and the other
 * keys must NEVER ends with content</li>
 * <li><b>The custom keys loaded directly into the i18n_messages table into the
 * database</b> : these are the messages specific to one customer. <b>WARNING :
 * the language code in the database must be lowercase !!!</b></li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
public interface II18nMessagesPlugin {
    public static final String NAME = "i18nPlugin";

    /**
     * Init the i18n plugin
     */
    public void init();

    /**
     * Invalidate the cache and reload the resources into memory
     * 
     * @param fullReload
     *            if true the cache is invalidated before being fully reloaded
     *            otherwise the content is simply "updated" = existing values
     *            overwritten)
     */
    public void reload(boolean fullReload);

    /**
     * Return the content associated with the specified key.<br/>
     * The system is using the language of the currently logged user.
     * 
     * @param key
     *            the i18n key
     * @param args
     *            the arguments for the message
     * @return the content
     */
    public String get(String key, Object... args);

    /**
     * Return the content associated with the specified key in the given
     * language
     * 
     * @param lang
     *            the language for the message
     * @param key
     *            the i18n key
     * @param args
     *            the arguments for the message
     * @return the content
     */
    public String get(Lang lang, String key, Object... args);

    /**
     * Return true if the specified key is translated in the current language
     * 
     * @param key
     *            the i18n key
     * @return true if the message exists
     */
    public boolean isDefined(String key);

    /**
     * Return true if the specified key is translated for at least one language
     * 
     * @param key
     *            the i18n key
     */
    public boolean isDefinedForAtLeastOneLanguage(String key);

    /**
     * Add a i18n value.<br/>
     * Please ensure that this method call is wrapped within an Ebean
     * transaction.
     * 
     * @param key
     *            the i18n key
     * @param value
     *            the value associated with this key
     * @param languageCode
     *            the language code "en"
     */
    public void add(String key, String value, String languageCode);

    /**
     * Delete a i18n value.<br/>
     * 
     * @param key
     *            the i18n key
     * @param languageCode
     *            the language code "en"
     */
    public void delete(String key, String languageCode);

}
