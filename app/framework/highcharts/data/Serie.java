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
 * A serie is a set of items.
 * 
 * @author Johann Kohler
 * 
 * @param <T>
 *            the type of the item
 */
public class Serie<T extends SerieItem> implements IDataProvider {

    private String name;
    private List<T> data;
    private String stack;

    /**
     * Default constructor.
     * 
     * @param name
     *            the serie name
     */
    public Serie(String name) {
        this.name = name;
        this.data = new ArrayList<T>();
        this.stack = null;
    }

    /**
     * Get the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name.
     * 
     * @param name
     *            the serie name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the stack.
     */
    public String getStack() {
        return stack;
    }

    /**
     * Set the stack.
     * 
     * @param stack
     *            the stack
     */
    public void setStack(String stack) {
        this.stack = stack;
    }

    /**
     * Add an item.
     * 
     * @param element
     *            the item to add.
     */
    public void add(T element) {
        data.add(element);
    }

    /**
     * Get the number of items.
     */
    public int size() {
        return data.size();
    }

    @Override
    public String toJsonArray() {

        JsonFactory jsonfactory = new JsonFactory();
        StringWriter sw = new StringWriter();
        JsonGenerator generator;
        try {
            generator = jsonfactory.createGenerator(sw);

            generator.writeStartObject();

            generator.writeStringField("name", this.name);

            if (this.stack != null) {
                generator.writeStringField("stack", this.stack);
            }

            generator.writeFieldName("data");

            generator.writeStartArray();
            for (T element : this.data) {
                generator.writeNumber(element.toJsonArray());
            }
            generator.writeEndArray();

            generator.writeEndObject();

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
        return "Serie [name=" + name + ", data=" + data + ", stack=" + stack + "]";
    }
}
