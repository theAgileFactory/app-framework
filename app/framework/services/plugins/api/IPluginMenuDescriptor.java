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
package framework.services.plugins.api;

/**
 * The meta-descriptor of a menu entry which is pointing to an URL.<br/>
 * A single plugin may offer multiple menu items.
 * 
 * @author Pierre-Yves Cloux
 */
public interface IPluginMenuDescriptor {
    /**
     * The i18n key of the label of the menu
     * 
     * @return a String
     */
    public String getLabel();

    /**
     * The path to the graphical user interface
     * 
     * @return a String
     */
    public String getPath();
}
