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
package framework.highcharts.pattern;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import play.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import framework.highcharts.data.NameValueItem;
import framework.highcharts.data.Serie;
import framework.highcharts.data.SeriesContainer;

/**
 * A range line pattern is a line chart with for the xAxis a set of ranges (n)
 * and for the yAxis a set of values (n+1) for each line.
 * 
 * A range is not temporal, it simply represents a cycle with a start and a end.
 * 
 * -The first y value is the value at start of the first range<br/>
 * -The second y value is the value at the end of the first range and at the
 * start of the second<br/>
 * -and so on<br/>
 * -The last y value is the value at the end of the last range
 * 
 * @author Johann Kohler
 * 
 */
public class RangeLine {

    /**
     * List of ranges.
     * 
     * It represents the labels of the xAxis.
     */
    private List<String> ranges = new ArrayList<String>();

    /**
     * List of value names.
     * 
     * Each line has exactly (#ranges + 1) points, so there are exactly the same
     * number of value names.
     */
    private List<String> valueNames = new ArrayList<String>();

    /**
     * List of elems.
     * 
     * An elem is a line, it contains<br/>
     * -A name<br/>
     * -A list of values (#ranges + 1)
     */
    private List<Elem> elems = new ArrayList<Elem>();

    /**
     * Add a range.
     * 
     * @param range
     *            the range to add
     */
    public void addRange(String range) {
        this.ranges.add(range);
    }

    /**
     * Add a value name.
     * 
     * @param name
     *            the value name to add
     */
    public void addValueName(String name) {
        this.valueNames.add(name);
    }

    /**
     * Add an elem.
     * 
     * @param elem
     *            the elem to add
     */
    public void addElem(Elem elem) {
        this.elems.add(elem);
    }

    /**
     * Get the options of the xAxis.
     */
    public String getXAxisOptionsAsJson() {

        JsonFactory jsonfactory = new JsonFactory();
        StringWriter sw = new StringWriter();
        JsonGenerator generator;
        try {
            generator = jsonfactory.createGenerator(sw);

            generator.writeStartObject();

            generator.writeFieldName("categories");
            generator.writeStartArray();
            for (String range : this.ranges) {
                generator.writeString(range);
            }
            generator.writeString("");
            generator.writeEndArray();

            generator.writeBooleanField("showLastLabel", false);

            generator.writeEndObject();

            generator.close();

            return sw.toString();

        } catch (JsonGenerationException e) {
            Logger.error("impossible to get the xAxis options", e);
            return null;
        } catch (IOException e) {
            Logger.error("impossible to get the xAxis options", e);
            return null;
        }

    }

    /**
     * Get the point placement for the series options.
     */
    public String getPointPlacement() {
        return "-0.5";
    }

    /**
     * Get the series.
     */
    public SeriesContainer<NameValueItem> getSeriesContainer() {

        SeriesContainer<NameValueItem> data = new SeriesContainer<NameValueItem>();

        for (Elem elem : this.elems) {
            Serie<NameValueItem> serie = new Serie<NameValueItem>(elem.getName());
            int i = 0;
            for (Double value : elem.getValues()) {
                serie.add(new NameValueItem(this.valueNames.get(i), value, null));
                i++;
            }
            data.addSerie(serie);
        }

        return data;
    }

    /**
     * An elem is a full-definition of a line.
     * 
     * @author Johann Kohler
     * 
     */
    public static class Elem {

        private String name;
        private List<Double> values;

        /**
         * Default constructor.
         * 
         * @param name
         *            the bar name
         */
        public Elem(String name) {
            this.name = name;
            this.values = new ArrayList<Double>();
        }

        /**
         * Get the name.
         */
        public String getName() {
            return this.name;
        }

        /**
         * Add a bar value.
         * 
         * @param value
         *            the bar value
         */
        public void addValue(Double value) {
            this.values.add(value);
        }

        /**
         * Get the bar values.
         */
        public List<Double> getValues() {
            return this.values;
        }

    }

}
