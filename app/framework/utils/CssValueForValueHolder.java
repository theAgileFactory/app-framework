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
 * A specific value object which contains a label and a CSS class that is used
 * as the value of a {@link ISelectableValueHolder}.<br/>
 * The CSS element is to be used by some "compatible" graphical components such as "parts.radiobuttongroup".
 * 
 * @author Johann Kohler
 */
public class CssValueForValueHolder {
    // the field value
    public String value;

    // the field label
    public String label;

    // the field CSS class
    public String cssClass;

    public CssValueForValueHolder(String value, String label, String cssClass) {
        this.value = value;
        this.label = label;
        this.cssClass = cssClass;
    }

}
