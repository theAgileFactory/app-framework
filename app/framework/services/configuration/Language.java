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

import java.util.HashMap;
import java.util.Map;

import play.i18n.Lang;
import framework.utils.ISelectableValueHolder;

/**
 * A system language
 * 
 * if a system language is enabled (application.langs in the file
 * application.conf) then the use can switch the app language to it
 * 
 * @author Johann Kohler
 * 
 */
public class Language implements ISelectableValueHolder<String> {
    private static Map<String, String> labels = new HashMap<String, String>() {
        private static final long serialVersionUID = 4564564564L;
        {
            put("en", "English");
            put("fr", "Français");
            put("de", "Deutsch");
            put("it", "Italiano");
            put("es", "Español");
        }
    };

    private String code;
    private Integer order;

    public Language(String code) {
        if (code != null) {
            this.setCode(code.trim().toLowerCase());
        }
    }

    public Language(String code, Integer order) {
        if (code != null && order != null) {
            this.setCode(code.trim().toLowerCase());
            this.setOrder(order);
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    /**
     * get the play Lang object for the language
     */
    public Lang getLang() {
        return new Lang(Lang.forCode(code));
    }

    @Override
    public int compareTo(Object o) {
        Language l = (Language) o;
        return this.getOrder().intValue() > l.getOrder().intValue() ? +1 : this.getOrder().intValue() < l.getOrder().intValue() ? -1 : 0;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getName() {
        if (labels.containsKey(getCode())) {
            return labels.get(getCode());
        }
        return getCode().toUpperCase();
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public String getValue() {
        return getCode();
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public void setUrl(String url) {
    }

    @Override
    public boolean equals(Object object) {
        Language language = (Language) object;
        if (this.getCode() != null && language.getCode() != null) {
            return this.getCode().equals(language.getCode());
        }
        return false;
    }

    @Override
    public String toString() {
        return "Language [code=" + code + ", order=" + order + "]";
    }
}
