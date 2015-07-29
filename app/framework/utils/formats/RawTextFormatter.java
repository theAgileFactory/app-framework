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
 * Render a cellValue which is assumed to be a String "text" value as HTML. This
 * formatter converts line feeds and Carriage returns in &lt;br/&gt;
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <T>
 */
public class RawTextFormatter<T> implements IColumnFormatter<T> {

    public RawTextFormatter() {
    }

    @Override
    public String apply(T value, Object cellValue) {
        if (cellValue == null) {
            return "";
        }
        String cellValueAsString = cellValue.toString();
        cellValueAsString = cellValueAsString.replace("\n", "<br/>");
        return cellValueAsString;
    }

}
