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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * A serie item. Each item type should extends this class.
 * 
 * @author Johann Kohler
 */
public abstract class SerieItem implements IDataProvider {

    /**
     * Convert a java item to a json object.
     * 
     * Each item type should implements the writeJsonElement method.
     * 
     * @param generator
     *            the json generator
     */
    public abstract void writeJsonElement(JsonGenerator generator) throws JsonGenerationException, IOException;

    @Override
    public String toJsonArray() {

        JsonFactory jsonfactory = new JsonFactory();
        StringWriter sw = new StringWriter();
        JsonGenerator generator;
        try {
            generator = jsonfactory.createGenerator(sw);
            writeJsonElement(generator);
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
}
