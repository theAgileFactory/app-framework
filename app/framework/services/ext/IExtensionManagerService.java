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

import framework.services.router.IRequestListener;
import play.mvc.Http.Context;
import play.mvc.Result;

/**
 * The extension manager service is responsible for the management of the
 * BizDock dynamic extensions.<br/>
 * 
 * @author Pierre-Yves Cloux
 */
public interface IExtensionManagerService extends IRequestListener, ILinkGenerationService {
    public static final String PATH_PREFIX = "/ext";

    /**
     * Return the list of the loaded extensions
     * 
     * @return a list of extensions
     */
    public List<IExtension> getLoadedExtensions();

    /**
     * Unload the specified extension
     * 
     * @param extension
     *            a loaded extension
     */
    public void unload(IExtension extension);

    /**
     * Load the specified JAR file
     * 
     * @param jarFilePath
     *            the path to a JAR extension
     * @return a boolean if the extension has been loaded successfully
     */
    public boolean load(String jarFilePath);

    /**
     * Look for each menu customization in each extension and execute them.<br/>
     */
    public boolean customizeMenu();

    /**
     * Look for an extension matching this path and execute is using the
     * specified context.
     * 
     * @param path
     *            the path to the extension
     * @param ctx
     *            the Play context
     * @return a Result
     */
    public Result execute(String path, Context ctx);

    /**
     * Get the size (bytes) of the extensions folder.
     */
    public Long getSize();
}
