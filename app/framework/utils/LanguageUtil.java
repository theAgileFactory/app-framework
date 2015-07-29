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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.Play;
import play.mvc.Http;

/**
 * provides all helpers useful for the language management
 * 
 * @author Johann Kohler
 * 
 */
public class LanguageUtil {

    private static Logger.ALogger log = Logger.of(LanguageUtil.class);

    public static Map<String, Language> VALID_LANGUAGES_MAP = Collections.unmodifiableMap(Collections.synchronizedMap(new LinkedHashMap<String, Language>() {
        private static final long serialVersionUID = 1L;
        {
            String languagesString = Play.application().configuration().getString("application.langs");
            Integer c = 1;
            for (String l : languagesString.trim().split(",")) {
                String code = l.trim().toLowerCase();
                this.put(code, new Language(code, c));
                c++;
            }
        }
    }));

    public static List<Language> VALID_LANGUAGES_LIST = Collections.unmodifiableList(Collections.synchronizedList(new ArrayList<Language>(VALID_LANGUAGES_MAP
            .values())));

    /**
     * get the list of valid languages as value holder collection
     */
    public static DefaultSelectableValueHolderCollection<String> getValidLanguagesAsValueHolderCollection() {
        return new DefaultSelectableValueHolderCollection<String>(VALID_LANGUAGES_MAP.values());
    }

    /**
     * Get the current language according to the context.<br/>
     * If the context is not available (example: no context, server side or
     * asynchronous process then the default language is used).
     */
    public static Language getCurrent() {
        try {
            return VALID_LANGUAGES_MAP.get(Http.Context.current().lang().code());
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[I18n] Attempt to get the language out of the scope of a session, default language is used instead", e);
            }
            return VALID_LANGUAGES_LIST.get(0);
        }
    }

    /**
     * get a language by code.
     * 
     * @param code
     *            the language code
     */
    public static Language getLanguageByCode(String code) {
        return new Language(code);
    }

    /**
     * get a language by code.
     * 
     * @param code
     *            the language code
     */
    public static boolean isValid(String code) {
        return VALID_LANGUAGES_MAP.containsKey(code);
    }
}
