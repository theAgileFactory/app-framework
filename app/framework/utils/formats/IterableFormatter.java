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

import java.util.Iterator;

import org.apache.commons.collections.IteratorUtils;

import framework.utils.IColumnFormatter;

/**
 * A column formatter which uses another {@link IColumnFormatter} (the
 * subFormatter) to format each elements of an {@link Iterable} value.<br/>
 * The values resulting from the execution of the subFormatters are appended.
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <T>
 */
public class IterableFormatter<T> implements IColumnFormatter<T> {
    private IColumnFormatter<T> subColumnFormatter;

    public IterableFormatter(IColumnFormatter<T> subColumnFormatter) {
        this.subColumnFormatter = subColumnFormatter;
    }

    @Override
    public String apply(T object, Object value) {
        StringBuffer sb = new StringBuffer();
        if (value != null && value instanceof Iterable) {
            Iterator<?> iterator = IteratorUtils.getIterator(value);
            while (iterator.hasNext()) {
                sb.append(getSubColumnFormatter().apply(object, iterator.next()));
            }
        }
        return sb.toString();
    }

    private IColumnFormatter<T> getSubColumnFormatter() {
        return subColumnFormatter;
    }
}
