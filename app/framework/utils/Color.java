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

/**
 * This helper provides the components to manage an attribute that is a
 * bootstrap color.
 * 
 * @author Johann Kohler
 * 
 */
public class Color {

    /**
     * Get the the selectable colors as a value holder collection.
     */
    public static DefaultSelectableValueHolderCollection<CssValueForValueHolder> getColorsAsValueHolderCollection() {

        DefaultSelectableValueHolderCollection<CssValueForValueHolder> selectableCssClasses;
        selectableCssClasses = new DefaultSelectableValueHolderCollection<CssValueForValueHolder>();
        selectableCssClasses.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(new CssValueForValueHolder("default", Msg.get("maf.color.default"),
                "light"), "1"));
        selectableCssClasses.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(new CssValueForValueHolder("info", Msg.get("maf.color.info"),
                "info"), "2"));
        selectableCssClasses.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(new CssValueForValueHolder("primary", Msg.get("maf.color.primary"),
                "primary"), "3"));
        selectableCssClasses.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(new CssValueForValueHolder("success", Msg.get("maf.color.success"),
                "success"), "4"));
        selectableCssClasses.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(new CssValueForValueHolder("warning", Msg.get("maf.color.warning"),
                "warning"), "5"));
        selectableCssClasses.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(new CssValueForValueHolder("danger", Msg.get("maf.color.danger"),
                "danger"), "6"));

        return selectableCssClasses;

    }

    /**
     * Get the label for a CSS color (default, info...).
     * 
     * @param cssColor
     *            the CSS color
     * @return
     */
    public static String getLabel(String cssColor) {
        return "<span class='label label-" + cssColor + "'>" + Msg.get("maf.color." + cssColor) + "</span>";
    }

}
