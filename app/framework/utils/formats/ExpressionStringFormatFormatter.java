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

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;

import framework.commons.IFrameworkConstants;
import framework.utils.IColumnFormatter;

/**
 * A formatter which uses:
 * <ul>
 * <li>a EL expression to extract a value from either:
 * <ul>
 * <li>the object : which is named "object" in the expression context</li>
 * <li>the value : which is named "value" in the expression context</li>
 * </ul>
 * </li>
 * <li>a string format : to format the value extracted by the expression</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <T>
 */
public class ExpressionStringFormatFormatter<T> implements IColumnFormatter<T> {
    private static JexlEngine jexlEngine = new JexlEngine();
    private String stringFormat;
    private Expression expression;

    public ExpressionStringFormatFormatter(String stringFormat, String expression) {
        this.stringFormat = stringFormat;
        this.expression = jexlEngine.createExpression(expression);
    }

    @Override
    public String apply(T object, Object value) {
        if (object != null) {
            JexlContext context = new MapContext();
            context.set("object", object);
            if (value != null) {
                context.set("value", value);
            }
            return views.html.framework_views.parts.formats.display_with_format.render(getExpression().evaluate(context), getStringFormat()).body();
        }
        return IFrameworkConstants.DEFAULT_VALUE_EMPTY_DATA;
    }

    private String getStringFormat() {
        return stringFormat;
    }

    private Expression getExpression() {
        return expression;
    }
}
