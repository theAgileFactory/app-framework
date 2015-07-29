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
package framework.services.ext;

import java.util.List;

/**
 * The public interface for the extensions descriptors
 * 
 * @author Pierre-Yves Cloux
 */
public interface IExtensionDescriptor {
    /**
     * The name of the extension
     * 
     * @return
     */
    public String getName();

    /**
     * The list of controllers which are associated with this extension.<br/>
     * Return a list of Java class names.
     * 
     * @return
     */
    public List<String> getDeclaredControllers();

    /**
     * The list of plugins which are associated with this extension.<br/>
     * Return a list of plugin description (identifier and java class).
     * 
     * @return
     */
    public List<String> getDeclaredPlugins();

    /**
     * True if the extension is associated with a menu customization
     * 
     * @return
     */
    public boolean isMenuCustomized();
}