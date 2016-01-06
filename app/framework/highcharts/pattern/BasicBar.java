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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import framework.highcharts.data.Serie;
import framework.highcharts.data.SeriesContainer;
import framework.highcharts.data.SingleValueItem;
import play.Logger;

/**
 * A basic bar pattern is a bar chart with one or many categories (
 * "groups of bars").
 * 
 * @author Johann Kohler
 * 
 */
public class BasicBar {

    private List<String> categories = new ArrayList<String>();

    private List<Elem> elems = new ArrayList<Elem>();

    /**
     * Add a category.
     * 
     * @param category
     *            the category to add
     */
    public void addCategory(String category) {
        this.categories.add(category);
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
     * Get the categories as a json array.
     */
    public String getCategoriesAsJson() {
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            return ow.writeValueAsString(this.categories);
        } catch (JsonProcessingException e) {
            Logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * Get the series.
     */
    public SeriesContainer<SingleValueItem> getSeriesContainer() {

        SeriesContainer<SingleValueItem> data = new SeriesContainer<SingleValueItem>();

        for (Elem elem : this.elems) {
            Serie<SingleValueItem> serie = new Serie<SingleValueItem>(elem.getName());
            serie.setStack(elem.getStack());
            for (Double value : elem.getValues()) {
                serie.add(new SingleValueItem(value));
            }
            if (serie.size() != this.categories.size()) {
                Logger.warn("impossible to add the serie " + serie.toString() + " because is contains " + serie.size() + " values, but "
                        + this.categories.size() + " is expected");
            } else {
                data.addSerie(serie);
            }
        }

        return data;
    }

    /**
     * An elem is a full-definition of a bar. If the chart contains many
     * categories, then the elem has a bar value for each category.
     * 
     * @author Johann Kohler
     * 
     */
    public static class Elem {

        private String name;
        private List<Double> values;
        private String stack;

        /**
         * Default constructor.
         * 
         * @param name
         *            the bar name
         */
        public Elem(String name) {
            this.name = name;
            this.values = new ArrayList<Double>();
            this.stack = null;
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

    }

}
