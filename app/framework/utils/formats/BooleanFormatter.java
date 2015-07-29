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

import framework.commons.IFrameworkConstants;
import framework.utils.IColumnFormatter;

/**
 * A formatter which render a boolean as a String
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <T>
 */
public class BooleanFormatter<T> implements IColumnFormatter<T> {
    public BooleanFormatter() {
    }

    @Override
    public String apply(T value, Object cellValue) {
        if (cellValue instanceof Boolean) {
            return views.html.framework_views.parts.formats.display_boolean.render((Boolean) cellValue).body();
        }
        return IFrameworkConstants.DEFAULT_VALUE_EMPTY_DATA;
    }
}
