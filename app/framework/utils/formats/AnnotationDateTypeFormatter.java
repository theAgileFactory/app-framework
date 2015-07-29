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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import play.data.format.Formatters.AnnotationFormatter;
import framework.commons.IFrameworkConstants;
import framework.utils.Utilities;

/**
 * A standard play formatter to be used with the {@link Date} attributes.<br/>
 * This one rely on the same
 * 
 * @author Pierre-Yves Cloux
 */
public class AnnotationDateTypeFormatter extends AnnotationFormatter<DateType, Date> {

    @Override
    public Date parse(DateType annotation, String text, Locale locale) throws ParseException {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        DateFormat dateFormat = Utilities.getDateFormat(annotation != null ? annotation.pattern() : null, locale);
        return dateFormat.parse(text);
    }

    @Override
    public String print(DateType annotation, Date value, Locale locale) {
        if (value == null) {
            return IFrameworkConstants.DEFAULT_VALUE_EMPTY_DATA;
        }
        return Utilities.getDateFormat(annotation != null ? annotation.pattern() : null, locale).format(value);
    }

}
