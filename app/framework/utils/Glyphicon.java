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
 * This helper provides the components to manage an attribute that is a
 * bootstrap glyphicon.
 * 
 * @author Johann Kohler
 * 
 */
public class Glyphicon {

    public static Set<String> glyphicons = new HashSet<String>(Arrays.asList("glyphicons glyphicons-ok-2", "glyphicons glyphicons-electricity",
            "glyphicons glyphicons-bin", "glyphicons glyphicons-envelope", "glyphicons glyphicons-circle-info", "glyphicons glyphicons-charts",
            "glyphicons glyphicons-paperclip", "glyphicons glyphicons-parents", "glyphicons glyphicons-coins", "glyphicons glyphicons-cargo",
            "glyphicons glyphicons-log-book", "glyphicons glyphicons-history", "glyphicons glyphicons-git-branch", "glyphicons glyphicons-cogwheels",
            "glyphicons glyphicons-cluster", "glyphicons glyphicons-thumbs-up", "glyphicons glyphicons-package", "glyphicons glyphicons-address-book",
            "glyphicons glyphicons-inbox", "glyphicons glyphicons-cloud", "glyphicons glyphicons-clock", "glyphicons glyphicons-sort"));

    /**
     * Get the the selectable glyphicons as a value holder collection.
     */
    public static DefaultSelectableValueHolderCollection<CssValueForValueHolder> getGlyphiconsAsVHC() {

        DefaultSelectableValueHolderCollection<CssValueForValueHolder> selectableGlyphicons;
        selectableGlyphicons = new DefaultSelectableValueHolderCollection<CssValueForValueHolder>();
        int i = 0;
        for (String glyphicon : glyphicons) {
            selectableGlyphicons.add(new DefaultSelectableValueHolder<CssValueForValueHolder>(new CssValueForValueHolder(glyphicon, "<span class='"
                    + glyphicon + " glyphicons-menu-small'></span>", "default"), String.valueOf(i)));
            i++;
        }

        return selectableGlyphicons;

    }

    /**
     * Get the label for a glyphicon.
     * 
     * @param glyphicon
     *            the glyphicon
     * @return
     */
    public static String getLabel(String glyphicon) {
        return "<span class='" + glyphicon + "'></span>";
    }

}
