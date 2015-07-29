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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * The series of a chart.
 * 
 * @author Johann Kohler
 * 
 * @param <T>
 *            the type of the item
 */
public class SeriesContainer<T extends SerieItem> implements IDataProvider {

    private List<Serie<T>> series;

    /**
     * Default constructor.
     */
    public SeriesContainer() {
        series = new ArrayList<Serie<T>>();
    }

    /**
     * Get the series.
     */
    public List<Serie<T>> getSeries() {
        return series;
    }

    /**
     * Set the series.
     * 
     * @param series
     *            the series to add
     */
    public void setSeries(List<Serie<T>> series) {
        this.series = series;
    }

    /**
     * Add a serie.
     * 
     * @param serie
     *            the serie to add
     */
    public void addSerie(Serie<T> serie) {
        this.series.add(serie);
    }

    @Override
    public String toJsonArray() {
        JsonFactory jsonfactory = new JsonFactory();
        StringWriter sw = new StringWriter();
        JsonGenerator generator;
        try {
            generator = jsonfactory.createGenerator(sw);
            generator.writeStartArray();
            for (Serie<T> serie : this.series) {
                generator.writeNumber(serie.toJsonArray());
            }
            generator.writeEndArray();
            generator.close();

        } catch (JsonGenerationException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return sw.toString();
    }

    @Override
    public String toString() {
        return "SeriesContainer [series=" + series + "]";
    }

}
