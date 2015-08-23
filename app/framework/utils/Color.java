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

import framework.services.configuration.II18nMessagesPlugin;

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
     * 
     * @param messagesPlugin
     *            the service to i18n the color name
     */
    public static DefaultSelectableValueHolderCollection<CssValueForValueHolder> getColorsAsValueHolderCollection(II18nMessagesPlugin messagesPlugin) {

        DefaultSelectableValueHolderCollection<CssValueForValueHolder> selectableCssClasses;
        selectableCssClasses = new DefaultSelectableValueHolderCollection<CssValueForValueHolder>();
        selectableCssClasses.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(
                new CssValueForValueHolder("default", messagesPlugin.get("maf.color.default"), "light"), "1"));
        selectableCssClasses.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(
                new CssValueForValueHolder("info", messagesPlugin.get("maf.color.info"), "info"), "2"));
        selectableCssClasses.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(
                new CssValueForValueHolder("primary", messagesPlugin.get("maf.color.primary"), "primary"), "3"));
        selectableCssClasses.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(
                new CssValueForValueHolder("success", messagesPlugin.get("maf.color.success"), "success"), "4"));
        selectableCssClasses.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(
                new CssValueForValueHolder("warning", messagesPlugin.get("maf.color.warning"), "warning"), "5"));
        selectableCssClasses.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(
                new CssValueForValueHolder("danger", messagesPlugin.get("maf.color.danger"), "danger"), "6"));

        return selectableCssClasses;

    }

    /**
     * Get the label for a CSS color (default, info...).
     * 
     * @param cssColor
     *            the CSS color
     * @param messagesPlugin
     *            the service to i18n the color name
     * @return
     */
    public static String getLabel(String cssColor, II18nMessagesPlugin messagesPlugin) {
        return "<span class='label label-" + cssColor + "'>" + messagesPlugin.get("maf.color." + cssColor) + "</span>";
    }

}
