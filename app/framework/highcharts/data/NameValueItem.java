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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * A "name/value" item.
 * 
 * @author Johann Kohler
 * 
 */
public class NameValueItem extends SerieItem {

    private String name;
    private Double y;
    private String color;

    /**
     * Default constructor.
     * 
     * @param name
     *            the item name
     * @param value
     *            the item value
     * @param color
     *            the item color, let to null to apply the standard color
     */
    public NameValueItem(String name, Double value, String color) {
        super();
        this.name = name;
        this.y = value;
        this.color = color;
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
     *            the item name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the value.
     */
    public Double getValue() {
        return y;
    }

    /**
     * Set the value.
     * 
     * @param value
     *            the item value
     */
    public void setValue(Double value) {
        this.y = value;
    }

    /**
     * Get the color.
     */
    public String getColor() {
        return color;
    }

    /**
     * Set the color.
     * 
     * @param color
     *            the item color
     */
    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public void writeJsonElement(JsonGenerator generator) throws JsonGenerationException, IOException {
        generator.writeStartObject();
        generator.writeStringField("name", name);
        if (y != null) {
            generator.writeNumberField("y", y);
        } else {
            generator.writeNullField("y");
        }
        if (color != null) {
            generator.writeStringField("color", color);
        }
        generator.writeEndObject();
    }

    @Override
    public String toString() {
        return "NameValueItem [name=" + name + ", y=" + y + ", color=" + color + "]";
    }

}
