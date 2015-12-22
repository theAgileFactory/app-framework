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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This helper provides the components to manage an attribute that is an icon.
 * 
 * @author Johann Kohler
 * 
 */
public class Icon {

    public static Set<String> icons = new HashSet<String>(Arrays.asList("fa fa-check", "fa fa-bolt",
            "fa fa-trash", "fa fa-envelope", "fa fa-info-circle", "fa fa-bar-chart",
            "fa fa-paperclip", "fa fa-user", "fa fa-money", "fa fa-industry",
            "fa fa-newspaper-o", "fa fa-history", "fa fa-code-fork", "fa fa-cogs",
            "fa fa-university", "fa fa-thumbs-up", "fa fa-cubes", "fa fa-book",
            "fa fa-inbox", "fa fa-cloud", "fa fa-clock-o", "fa fa-folder"));

    /**
     * Get the the selectable icons as a value holder collection.
     */
    public static DefaultSelectableValueHolderCollection<CssValueForValueHolder> getIconsAsVHC() {

        DefaultSelectableValueHolderCollection<CssValueForValueHolder> selectableIcons;
        selectableIcons = new DefaultSelectableValueHolderCollection<CssValueForValueHolder>();
        int i = 0;
        for (String icon : icons) {
            selectableIcons.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(
                    new CssValueForValueHolder(icon, "<span class='" + icon + "'></span>", "default"), String.valueOf(i)));
            i++;
        }

        return selectableIcons;

    }

    /**
     * Get the label for an icon.
     * 
     * @param icon
     *            the icon
     * @return
     */
    public static String getLabel(String icon) {
        return "<span class='" + icon + "'></span>";
    }

}
