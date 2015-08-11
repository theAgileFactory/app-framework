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

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import play.Logger;
import play.Play;
import play.i18n.Lang;
import play.i18n.Messages;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import play.twirl.api.Html;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.avaje.ebean.SqlUpdate;

import framework.services.database.IDatabaseDependencyService;
import framework.utils.Language;
import framework.utils.LanguageUtil;

/**
 * The default implementation for the {@link II18nMessagesPlugin} interface.<br/>
 * This implementation is based on a in memory {@link Hashtable} which:
 * <ul>
 * <li>key = a language code (lower case)</li>
 * <li>hashtable = a dictionary which key is a i18n key and value is the content
 * </li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class I18nMessagesPluginImpl implements II18nMessagesPlugin {
    private static Logger.ALogger log = Logger.of(I18nMessagesPluginImpl.class);
    private Hashtable<String, Hashtable<Object, Object>> i18nMessagesStore;

    /**
     * Creates a new I18nMessagesPluginImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param databaseDependencyService
     *            the service which secure the availability of the database
     */
    @Inject
    public I18nMessagesPluginImpl(ApplicationLifecycle lifecycle, IDatabaseDependencyService databaseDependencyService) {
        log.info("SERVICE>>> I18nMessagesPluginImpl starting...");
        reload(true);
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> I18nMessagesPluginImpl stopping...");
            log.info("SERVICE>>> I18nMessagesPluginImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> I18nMessagesPluginImpl started");
    }

    @Override
    public void reload(boolean fullReload) {
        if (fullReload) {
            i18nMessagesStore = new Hashtable<String, Hashtable<Object, Object>>();
        }
        // Load from the table
        loadFromDb();
    }

    /**
     * Load the keys stored into the database
     */
    private void loadFromDb() {
        try {
            SqlQuery query = Ebean.createSqlQuery("select * from i18n_messages");
            List<SqlRow> rows = query.findList();
            if (rows != null) {
                for (SqlRow row : rows) {
                    String language = row.getString("language");
                    Hashtable<Object, Object> messages = getI18nMessagesStore().get(language);
                    if (messages == null) {
                        messages = new Hashtable<Object, Object>();
                        getI18nMessagesStore().put(language, messages);
                    }
                    messages.put(row.getString("key"), row.getString("value"));
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while loading the resources form the database", e);
        }
    }

    @Override
    public String get(Lang lang, String key, Object... args) {

        if (StringUtils.isBlank(key)) {
            return "";
        }

        if (key.endsWith("_content")) {
            return getI18nContent(key, lang.code());
        }
        if (Messages.isDefined(lang, key)) {
            return Messages.get(lang, key, args);
        }
        Hashtable<Object, Object> messages = getI18nMessagesStore().get(lang.code());
        if (messages == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Unknown language requested [%s] for key [%s]", lang.code(), key));
            }
            return key;
        }

        String value = null;
        if (args.length != 0 && messages.containsKey(key)) {
            value = MessageFormat.format((String) messages.get(key), args);
        } else {
            value = messages.containsKey(key) ? (String) messages.get(key) : key;
        }

        return value;

    }

    @Override
    public String get(String key, Object... args) {
        Language language = LanguageUtil.getCurrent();
        return get(language.getLang(), key, args);
    }

    @Override
    public void add(String key, String value, String languageCode) {
        if (!LanguageUtil.isValid(languageCode)) {
            log.error("Invalid language code " + languageCode + " the key [" + key + "] will not be added");
            return;
        }
        try {
            // Check if the key for this language already exists in the database
            SqlQuery findKeyQuery = Ebean.createSqlQuery("select * from i18n_messages where `key`=:key and language=:language");
            findKeyQuery.setParameter("key", key);
            findKeyQuery.setParameter("language", languageCode);
            SqlRow row = findKeyQuery.findUnique();
            if (row != null && !row.isEmpty()) {
                // This is an update
                SqlUpdate updateKeyQuery = Ebean.createSqlUpdate("update i18n_messages set value=:value where `key`=:key and language=:language");
                updateKeyQuery.setParameter("value", value);
                updateKeyQuery.setParameter("key", key);
                updateKeyQuery.setParameter("language", languageCode);
                int rowModified = updateKeyQuery.execute();
                if (rowModified == 0) {
                    throw new IllegalArgumentException("Unable to update the key (no row modified) : " + key);
                }
            } else {
                // This is an insert
                SqlUpdate insertKeyQuery = Ebean.createSqlUpdate("insert into i18n_messages (`key`, language, value) values (:key, :language, :value)");
                insertKeyQuery.setParameter("value", value);
                insertKeyQuery.setParameter("key", key);
                insertKeyQuery.setParameter("language", languageCode);
                int rowModified = insertKeyQuery.execute();
                if (rowModified == 0) {
                    throw new IllegalArgumentException("Unable to insert the key (no row modified) : " + key);
                }
            }
            synchronized (i18nMessagesStore) {
                Hashtable<Object, Object> i18Messages = getI18nMessagesStore().get(languageCode);
                if (i18Messages == null) {
                    i18Messages = new Hashtable<Object, Object>();
                }
                i18Messages.put(key, value);
                getI18nMessagesStore().put(languageCode, i18Messages);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while adding the i18n key : " + key, e);
        }
    }

    @Override
    public void delete(String key, String languageCode) {
        if (!LanguageUtil.isValid(languageCode)) {
            throw new IllegalArgumentException("Invalid language code " + languageCode);
        }
        try {

            SqlUpdate updateKeyQuery = Ebean.createSqlUpdate("DELETE FROM `i18n_messages` WHERE `key`=:key and`language`=:language");
            updateKeyQuery.setParameter("key", key);
            updateKeyQuery.setParameter("language", languageCode);
            updateKeyQuery.execute();

            synchronized (i18nMessagesStore) {
                Hashtable<Object, Object> i18Messages = getI18nMessagesStore().get(languageCode);
                if (i18Messages == null) {
                    i18Messages = new Hashtable<Object, Object>();
                }
                i18Messages.remove(key);
                getI18nMessagesStore().put(languageCode, i18Messages);
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Error while deleting the i18n key : " + key, e);
        }

    }

    @Override
    public boolean isDefinedForAtLeastOneLanguage(String key) {
        boolean result = false;
        for (Language language : LanguageUtil.VALID_LANGUAGES_LIST) {
            result = result || isDefined(key, language);
        }
        return result;
    }

    @Override
    public boolean isDefined(String key) {
        return isDefined(key, LanguageUtil.getCurrent());
    }

    private boolean isDefined(String key, Language language) {
        if (Messages.isDefined(language.getLang(), key)) {
            return true;
        }
        Hashtable<Object, Object> messages = getI18nMessagesStore().get(language.getCode());
        if (messages == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Unknown language requested [%s] for key [%s]", language.getCode(), key));
            }
            return false;
        }
        return messages.containsKey(key);
    }

    /**
     * Return the I18n content block matching the specified key.<br/>
     * The content can be:
     * <ul>
     * <li>a i18n message</li>
     * <li>a scala template named according to the following pattern:
     * views.content.[current language].[key]</li>
     * </ul>
     * 
     * @param key
     *            the i18n key for the content block
     * @param language
     *            the language to be used for this content
     * @return a String or the i18n key itself
     */
    private static String getI18nContent(String key, String language) {
        try {
            Class<?> viewClass = Play.application().classloader().loadClass(String.format("views.html.content.%s.%s", language, key));
            return ((Html) viewClass.getMethod("render").invoke(viewClass, new Object[] {})).body();
        } catch (Exception e) {
            log.error("Error while calling the i18n content template " + key, e);
        }
        return key;
    }

    private Hashtable<String, Hashtable<Object, Object>> getI18nMessagesStore() {
        return i18nMessagesStore;
    }
}
