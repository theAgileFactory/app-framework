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
package framework.services.remote;

import play.libs.F.Promise;
import play.mvc.Result;
import play.twirl.api.Html;

/**
 * A service which manages the display of advertisement or information panels.
 * <p>
 * For each page it's possible to have a specific panel or the default one. Here
 * is the applied rules to determine which one is displayed on a page:<br/>
 * 1. A specific panel exists for the page => we display it<br/>
 * 2. The default panel exists => we display it<br/>
 * 3. No panel exists => we display nothing
 * </p>
 * <p>
 * The list of "panelisable pages" (pages which should display a panel) is
 * retrieved on a regular basis using a scheduler.<br/>
 * </p>
 * 
 * @author Johann Kohler
 * @author Pierre-Yves Cloux
 */
public interface IAdPanelManagerService {
    public static final String NAME = "adPanelService";

    /**
     * Launch the automated retrieval of the ad panel
     */
    public void init();

    /**
     * Clear the content cache and force the retrieval of a new panelisable list
     * of pages.
     */
    public void clearCache();

    /**
     * Check if the specified route is "panelisable" (a panel can be displayed)
     * 
     * @param route
     *            a page route (the request.path value)
     * @return
     */
    public boolean isPanelisable(String route);

    /**
     * Retrieve the panel associated with the specified route.<br/>
     * This method returns either directly the content or return an ajax
     * structure to be used to retrieve the content without blocking the
     * interface.
     * 
     * @param route
     *            a page route (the request.path value)
     * @return
     */
    public Html getPanel(String route);

    /**
     * Retrieve a panel from a remote location.<br/>
     * Store the result in the cache.
     * 
     * @param route
     *            a page route (the request.path value)
     * @return
     */
    public Promise<Result> getRemotePanel(String route);

    /**
     * Stop the service
     */
    public void destroy();
}
