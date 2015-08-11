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

import javax.inject.Inject;
import javax.inject.Singleton;

import play.i18n.Lang;
import play.i18n.Messages;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.Language;

/**
 * The class to be used in replacement of the play {@link Messages} class
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class Msg {
    @Inject
    private static II18nMessagesPlugin messagesPlugin;

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
    public static String get(String key, Object... args) {

        if (key == null) {
            return null;
        }

        String value = getMessagesPlugin().get(key, args);

        if (!key.equals(value)) {
            return value;
        } else {
            for (Language language : getMessagesPlugin().getValidLanguageMap().values()) {
                value = getMessagesPlugin().get(language.getLang(), key, args);
                if (!key.equals(value)) {
                    return getMessagesPlugin().getCurrentLanguage().getCode().toUpperCase() + " - " + value;
                }
            }
            return key;
        }

    }

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
    public static String get(Lang lang, String key, Object... args) {
        return getMessagesPlugin().get(lang, key, args);
    }

    /**
     * Return true if the specified key is translated in the current language.
     * 
     * @param key
     *            the i18n key
     * @return true if the message exists
     */
    public static boolean isDefined(String key) {
        return getMessagesPlugin().isDefined(key);
    }

    /**
     * Return true if the specified key is translated for at least one language.
     * 
     * @param key
     *            the i18n key
     */
    public static boolean isDefinedForAtLeastOneLanguage(String key) {
        return getMessagesPlugin().isDefinedForAtLeastOneLanguage(key);
    }

    private static II18nMessagesPlugin getMessagesPlugin() {
        return messagesPlugin;
    }
}
