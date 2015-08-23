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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import framework.highcharts.HighchartsUtils;
import framework.highcharts.data.NameValueItem;
import framework.highcharts.data.Serie;
import framework.highcharts.data.SeriesContainer;
import framework.services.configuration.II18nMessagesPlugin;

/**
 * A distributed donut pattern is a pie chart with 2 series: the inner serie
 * represents a set of categories, and the outer serie a set of sub-categories
 * for each category.
 * 
 * @author Johann Kohler
 */
public class DistributedDonut {
    private II18nMessagesPlugin messagesPlugin;
    private List<Elem> elems = new ArrayList<Elem>();

    public DistributedDonut(II18nMessagesPlugin messagesPlugin){
        this.messagesPlugin=messagesPlugin;
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
     * Return true if the distributed donut is empty.
     */
    public boolean isEmpty() {
        return this.elems.size() == 0 ? true : false;
    }

    /**
     * Get the series of the distributed donut. It's simply 2 series with
     * "name/value" items.
     */
    public SeriesContainer<NameValueItem> getSeriesContainer() {

        Serie<NameValueItem> innerSerie = new Serie<NameValueItem>("inner");
        Serie<NameValueItem> outerSerie = new Serie<NameValueItem>("outer");

        int i = 0;

        for (Elem elem : this.elems) {

            NameValueItem item = new NameValueItem(elem.getName(), elem.getValue(), HighchartsUtils.COLORS[i]);
            innerSerie.add(item);

            Double subValuesTotal = 0.0;

            for (Entry<String, Double> subValue : elem.getSubValues().entrySet()) {
                NameValueItem subItem = new NameValueItem(subValue.getKey(), subValue.getValue(), HighchartsUtils.LIGHT_COLORS[i]);
                outerSerie.add(subItem);
                subValuesTotal += subValue.getValue();
            }

            // if the total of the sub values is lesser than the category value,
            // then we add a new item for the difference.
            if (subValuesTotal.compareTo(elem.getValue()) < 0) {
                NameValueItem subItem = new NameValueItem(getMessagesPlugin().get("other"), elem.getValue() - subValuesTotal, HighchartsUtils.LIGHT_COLORS[i]);
                outerSerie.add(subItem);
            }

            i++;

        }

        SeriesContainer<NameValueItem> data = new SeriesContainer<NameValueItem>();
        data.addSerie(innerSerie);
        data.addSerie(outerSerie);

        return data;
    }

    /**
     * An elem is the full-definition of a category: it contains its name, its
     * value and the list of sub-values.
     * 
     * @author Johann Kohler
     */
    public static class Elem {

        private String name;
        private Double value;

        private Map<String, Double> subValues = new LinkedHashMap<String, Double>();

        /**
         * Default constructor.
         * 
         * @param name
         *            the name of the elem.
         */
        public Elem(String name) {
            this.name = name;
            this.value = 0.0;
        }

        /**
         * Add a sub value to the elem.
         * 
         * @param name
         *            the name of the sub value
         * @param value
         *            the sub value
         */
        public void addSubValue(String name, Double value) {
            this.subValues.put(name, value);
        }

        /**
         * Get the name.
         */
        public String getName() {
            return this.name;
        }

        /**
         * Get the value.
         */
        public Double getValue() {
            return this.value;
        }

        /**
         * Set the value.
         * 
         * @param value
         *            the elem value
         */
        public void setValue(Double value) {
            this.value = value;
        }

        /**
         * Get the sub values.
         */
        public Map<String, Double> getSubValues() {
            return this.subValues;
        }

    }

    private II18nMessagesPlugin getMessagesPlugin() {
        return messagesPlugin;
    }

}
