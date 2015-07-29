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
package framework.highcharts.data;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * A "name/value" item.
 * 
 * @author Johann Kohler
 * 
 */
public class TimeValueItem extends SerieItem {

    private Date date;
    private double value;

    /**
     * Default constructor.
     * 
     * @param date
     *            the item date
     * @param value
     *            the item value
     */
    public TimeValueItem(Date date, double value) {
        super();
        this.date = date;
        this.value = value;
    }

    /**
     * Get the date.
     */
    public Date getDate() {
        return date;
    }

    /**
     * Set the date.
     * 
     * @param date
     *            the item date
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * Get the value.
     */
    public double getValue() {
        return value;
    }

    /**
     * Set the value.
     * 
     * @param value
     *            the item value
     */
    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public void writeJsonElement(JsonGenerator generator) throws JsonGenerationException, IOException {
        generator.writeStartArray();
        generator.writeNumber(date.getTime());
        generator.writeNumber(value);
        generator.writeEndArray();
    }

    @Override
    public String toString() {
        return "TimeValueItem [date=" + date + ", value=" + value + "]";
    }

}
