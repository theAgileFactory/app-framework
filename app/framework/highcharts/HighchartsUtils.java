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
package framework.highcharts;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import play.Logger;

/**
 * The highcharts utils.
 * 
 * @author Johann Kohler
 * 
 */
public class HighchartsUtils {

    /**
     * The standard colors.
     */
    public static final String[] COLORS = { "#777777", "#428BCA", "#5CB85C", "#F0AD4E", "#D9534F", "#5BC0DE" };

    /**
     * The standard lighted colors.
     */
    public static final String[] LIGHT_COLORS = { "#A0A0A0", "#7BAEDA", "#7DC67D", "#F3BD71", "#E48784", "#8CD3E8" };

    /**
     * Get the colors as a json array.
     */
    public static String getColorsAsJson() {
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            return ow.writeValueAsString(COLORS);
        } catch (JsonProcessingException e) {
            Logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * Convert a date to the UTC format and clean it (remove hour).
     * 
     * @param date
     *            the source date
     */
    public static Date convertToUTCAndClean(Date date) {
        Calendar sourceDate = Calendar.getInstance();
        sourceDate.setTime(date);
        int daylightOffset = sourceDate.getTimeZone().inDaylightTime(date) ? sourceDate.getTimeZone().getDSTSavings() : 0;
        long timestamp = date.getTime() + sourceDate.getTimeZone().getRawOffset() + daylightOffset;
        Calendar utcDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcDate.setTimeInMillis(timestamp);
        utcDate.set(Calendar.HOUR_OF_DAY, 0);
        utcDate.clear(Calendar.MINUTE);
        utcDate.clear(Calendar.SECOND);
        utcDate.clear(Calendar.MILLISECOND);
        return utcDate.getTime();
    }

}
