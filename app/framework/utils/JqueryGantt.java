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
package framework.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import play.Logger;

/**
 * Provide helpers for the jQuery gantt plugin.
 * 
 * @author Johann Kohler
 * 
 */
public class JqueryGantt {

    /**
     * Clean the to date for the gantt chart due to a plugin issue.
     * 
     * refs #498<br/>
     * fix for jquery.gantt plugin: if an interval contains the daylight time
     * date (but NOT the winter time date) then the end date is wrongly
     * interpreted (shifted by one day)
     * 
     * @param from
     *            the from date
     * @param to
     *            the to date
     */
    public static Date cleanToDate(Date from, Date to) {

        if (!TimeZone.getTimeZone("Europe/London").inDaylightTime(from) && TimeZone.getTimeZone("Europe/London").inDaylightTime(to)) {
            Logger.warn("need to add one day to the end date");
            Calendar c = Calendar.getInstance();
            c.setTime(to);
            c.add(Calendar.DATE, 1);
            to = c.getTime();
        }
        return to;
    }

}
