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
package framework.utils.formats;

import framework.utils.IColumnFormatter;

/**
 * Render an Object using a String format.<br/>
 * It is possible to provide a hook which will be used to "convert" dynamically
 * the value before it is used for by the String formatter.<br/>
 * This is especially useful for generating URLs from a controller routes
 * (revert routing).
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <T>
 */
public class StringFormatFormatter<T> implements IColumnFormatter<T> {
    private String stringFormat;
    private Hook<T> hook;

    public StringFormatFormatter(String stringFormat) {
        super();
        this.stringFormat = stringFormat;
    }

    public StringFormatFormatter(String stringFormat, Hook<T> hook) {
        this(stringFormat);
        this.hook = hook;
    }

    @Override
    public String apply(T value, Object cellValue) {
        if (getHook() == null) {
            return views.html.framework_views.parts.formats.display_with_format.render(cellValue, getStringFormat()).body();
        } else {
            String stringValue = getHook().convert(value);
            return views.html.framework_views.parts.formats.display_with_format.render(stringValue, getStringFormat()).body();
        }
    }

    private String getStringFormat() {
        return stringFormat;
    }

    /**
     * A class which could be provided to convert dynamically the String into
     * another String.
     * 
     * @author Pierre-Yves Cloux
     * 
     * @param <T>
     */
    public interface Hook<T> {
        public String convert(T value);
    }

    private Hook<T> getHook() {
        return hook;
    }
}
