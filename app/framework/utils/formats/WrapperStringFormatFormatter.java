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
 * A {@link IColumnFormatter} which is using a String format to format the
 * result of another formatter (the subFormatter).
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <T>
 */
public class WrapperStringFormatFormatter<T> implements IColumnFormatter<T> {
    private IColumnFormatter<T> subColumnFormatter;
    private String stringFormat;

    public WrapperStringFormatFormatter(IColumnFormatter<T> subColumnFormatter, String stringFormat) {
        this.stringFormat = stringFormat;
        this.subColumnFormatter = subColumnFormatter;
    }

    @Override
    public String apply(T object, Object value) {
        String subValue = getSubColumnFormatter().apply(object, value);
        return views.html.framework_views.parts.formats.display_with_format.render(subValue, getStringFormat()).body();
    }

    private String getStringFormat() {
        return stringFormat;
    }

    private IColumnFormatter<T> getSubColumnFormatter() {
        return subColumnFormatter;
    }
}
