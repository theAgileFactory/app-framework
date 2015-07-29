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

import java.text.NumberFormat;

import framework.commons.IFrameworkConstants;
import framework.utils.IColumnFormatter;

/**
 * Render a number as a String using the specified pattern of a default one
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <T>
 */
public class NumberFormatter<T> implements IColumnFormatter<T> {
    private String pattern;

    public NumberFormatter() {
    }

    /**
     * Creates a new NumberFormatter with the specified pattern
     * 
     * @param pattern
     *            a {@link NumberFormat} pattern
     */
    public NumberFormatter(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String apply(T value, Object cellValue) {
        if (cellValue instanceof Number) {
            return views.html.framework_views.parts.formats.display_number.render((Number) cellValue, getPattern(), false).body();
        }
        return IFrameworkConstants.DEFAULT_VALUE_EMPTY_DATA;
    }

    private String getPattern() {
        return pattern;
    }

}
