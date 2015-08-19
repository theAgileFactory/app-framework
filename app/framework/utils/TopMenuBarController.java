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

import be.objectify.deadbolt.java.actions.SubjectPresent;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * Controller for the top menu bar.
 * 
 * @author Johann Kohler
 * 
 */
public class TopMenuBarController extends Controller {

    /**
     * Switch from a menu to another.
     * 
     * @param key
     *            the perspective key
     */
    @SubjectPresent
    public Result switchPerspective(String key) {
        TopMenuBar.getInstance().setPerspectiveFromPreference(key);
        return redirect(Play.application().configuration().getString("play.http.context"));
    }

}
