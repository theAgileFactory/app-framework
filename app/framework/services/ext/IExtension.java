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

import java.io.File;
import java.io.InputStream;
import java.util.Date;

import framework.services.ext.api.IExtensionDescriptor;

public interface IExtension {
    public static final String EXTENSION_MANAGER_DESCRIPTOR_FILE = "extension.xml";

    /**
     * Date when the extension was loaded
     * 
     * @return
     */
    public Date loadingTime();

    /**
     * Return the extension descriptor loaded from the extension JAR
     * 
     * @return a descriptor
     */
    public IExtensionDescriptor getDescriptor();

    /**
     * Return the JAR file which contains the extension
     * 
     * @return a file
     */
    public File getJarFile();

    /**
     * Return a resource included in this extension as an {@link InputStream}.
     * 
     * @param name
     *            the name of the resource (see {@link ClassLoader}) for the
     *            usual syntax of a resource name
     */
    public InputStream getResourceAsStream(String name);
}
