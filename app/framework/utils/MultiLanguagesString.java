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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import framework.services.ServiceManager;
import framework.services.configuration.II18nMessagesPlugin;

/**
 * A String in all BizDock languages.
 * 
 * @author Johann Kohler
 * 
 */
public class MultiLanguagesString {

    public static String keyPrefix = "multi_languages_string.";

    private String key;

    /**
     * List of value translations: it is either an empty list or a list with one
     * value for each available language (the values are ordered according to
     * the languages order). Each value represents the translation (it could be
     * null).
     */
    private List<String> values;

    /**
     * Construct a MultiLanguagesString with a random key and an empty values
     * map.
     */
    public MultiLanguagesString() {
        this.key = generateKey();
        this.values = new ArrayList<String>();
    }

    /**
     * Get the key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the key. Return an empty key if there is no value.
     */
    public String getKeyIfValue() {
        boolean hasValue = false;
        for (String value : values) {
            if (value != null && !value.equals("")) {
                hasValue = true;
            }
        }
        if (hasValue) {
            return key;
        } else {
            return "";
        }
    }

    /**
     * Set the key.
     * 
     * @param key
     *            the key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Get the values.
     */
    public List<String> getValues() {
        return this.values;
    }

    /**
     * Add a value.
     * 
     * @param value
     *            the translation value
     */
    public void addValue(String value) {
        this.values.add(value);
    }

    /**
     * Persist the values in the DB (this method is called after a save/update
     * of an entry => one call for each field with MultiLanguagesString type).
     */
    public void persist() {

        II18nMessagesPlugin messagesPlugin = ServiceManager.getService(II18nMessagesPlugin.NAME, II18nMessagesPlugin.class);

        for (int i = 0; i < LanguageUtil.VALID_LANGUAGES_LIST.size(); i++) {
            Language language = LanguageUtil.VALID_LANGUAGES_LIST.get(i);
            String value = this.values.get(i);
            if (value != null && !value.equals("")) {
                messagesPlugin.add(this.key, value, language.getCode());
            } else {
                messagesPlugin.delete(this.key, language.getCode());
            }
        }
    }

    /**
     * Get a MultiLanguagesString by key (this method is called for the "edit"
     * case of an entry, when loading the data).
     * 
     * @param key
     *            the key
     */
    public static MultiLanguagesString getByKey(String key) {

        /**
         * if the key is not null (and not empty) but not defined => we consider
         * that the key is in fact a value => we generate a new key and set for
         * it the value for the default language
         */
        if (key != null && !key.equals("") && !Msg.isDefinedForAtLeastOneLanguage(key)) {

            String value = key;
            key = generateKey();

            MultiLanguagesString s = new MultiLanguagesString();
            s.setKey(key);

            for (Language language : LanguageUtil.VALID_LANGUAGES_LIST) {
                if (language.getCode().equals(LanguageUtil.VALID_LANGUAGES_LIST.get(0).getCode())) {
                    s.addValue(value);
                } else {
                    s.addValue(null);
                }
            }

            return s;

        } else {

            if (key == null || key.equals("")) {
                key = generateKey();
            }

            MultiLanguagesString s = new MultiLanguagesString();
            s.setKey(key);

            for (Language language : LanguageUtil.VALID_LANGUAGES_LIST) {
                String value = Msg.get(language.getLang(), key);
                if (!key.equals(value)) {
                    s.addValue(value);
                } else {
                    s.addValue(null);
                }
            }

            return s;
        }

    }

    /**
     * Generate a random and unique key.
     */
    public static String generateKey() {
        return keyPrefix + UUID.randomUUID();
    }

}
