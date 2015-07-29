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

import java.util.LinkedHashMap;
import java.util.Map;

import framework.commons.IFrameworkConstants;
import framework.services.ServiceManager;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.configuration.ImplementationDefineObjectServiceFactory;
import framework.services.session.IUserSessionManagerPlugin;
import framework.utils.Menu.MenuItem;
import play.mvc.Http.Context;
import play.twirl.api.Html;

/**
 * The top menu bar is a set of perspectives. Each perspective represents a top
 * menu. If there are more than one perspective, then the user can switch from
 * one to another.
 * 
 * @author Johann Kohler
 */
public class TopMenuBar {

    private static final String MAIN_KEY = "main";

    private static TopMenuBar instance;

    /**
     * Get the singleton instance.
     */
    public static TopMenuBar getInstance() {
        if (instance == null) {
            instance = new TopMenuBar();
        }
        return instance;
    }

    private Map<String, TopMenuBarPerspective> perspectives;

    /**
     * Default constructor: a main perspective is automatically created with the
     * standard BizDock logo.
     */
    private TopMenuBar() {
        this.perspectives = new LinkedHashMap<>();
        this.perspectives.put(MAIN_KEY, new TopMenuBarPerspective(true, views.html.framework_views.parts.menubars.logo.render()));
    }

    /**
     * Get the switching route for a key.
     * 
     * @param key
     *            the perspective key
     */
    public String getSwitchingRoute(String key) {
        IImplementationDefinedObjectService implementationDefinedObjectService = ImplementationDefineObjectServiceFactory.getInstance();
        return implementationDefinedObjectService.getRouteForSwitchingTopMenuBarPerspective(key).url();
    }

    /**
     * Set the perspective in the user preference.
     * 
     * @param key
     *            the perspective key
     */
    public void setPerspectiveFromPreference(String key) {
        IPreferenceManagerPlugin preferenceManagerPlugin = ServiceManager.getService(IPreferenceManagerPlugin.NAME, IPreferenceManagerPlugin.class);
        preferenceManagerPlugin.updatePreferenceValue(IFrameworkConstants.CURRENT_PERSPECTIVE_PREFERENCE, key);
    }

    /**
     * Get the perspective key from the user preference.
     */
    public String getPerspectiveFromPreference() {
        IPreferenceManagerPlugin preferenceManagerPlugin = ServiceManager.getService(IPreferenceManagerPlugin.NAME, IPreferenceManagerPlugin.class);
        String key = preferenceManagerPlugin.getPreferenceValueAsString(IFrameworkConstants.CURRENT_PERSPECTIVE_PREFERENCE);
        if (key == null || key.equals("")) {
            return MAIN_KEY;
        } else {
            return key;
        }
    }

    /**
     * Add a secondary perspective.
     * 
     * @param key
     *            the key to identify the perspective
     * @param label
     *            the displayed label to switch from a perspective to another
     *            (usually a logo)
     */
    public void add(String key, Html label) {
        this.perspectives.put(key, new TopMenuBarPerspective(false, label));
    }

    /**
     * Get all perspectives.
     */
    public Map<String, TopMenuBarPerspective> get() {
        return this.perspectives;
    }

    /**
     * Get a perspective by key.
     * 
     * @param key
     *            the key
     */
    public TopMenuBarPerspective get(String key) {
        return this.perspectives.get(key);
    }

    /**
     * Get the current perspective of the sign-in user.
     * 
     * If the user is not sign-in then return the main.
     */
    public TopMenuBarPerspective getCurrent() {
        if (this.hasUser()) {
            return this.perspectives.get(this.getPerspectiveFromPreference());
        } else {
            return this.getMain();
        }

    }

    /**
     * Get the main perspective.
     */
    public TopMenuBarPerspective getMain() {
        return this.perspectives.get(MAIN_KEY);
    }

    /**
     * Return true if there is a login user.
     */
    public boolean hasUser() {
        IUserSessionManagerPlugin userSessionManagerPlugin = framework.services.ServiceManager.getService(IUserSessionManagerPlugin.NAME,
                IUserSessionManagerPlugin.class);
        return userSessionManagerPlugin.getUserSessionId(Context.current()) != null;
    }

    /**
     * Return true if there are secondary perspectives.
     */
    public boolean hasSecondaryPerspectives() {
        return this.perspectives.size() > 1 ? true : false;
    }

    /**
     * Clear all perspectives.
     */
    public void clear() {
        for (Map.Entry<String, TopMenuBarPerspective> entry : this.perspectives.entrySet()) {
            entry.getValue().clear();
        }
    }

    /**
     * Add a menu item to the main perspective.
     * 
     * @param menuItem
     *            the menu item to add
     */
    public void addMenuItem(MenuItem menuItem) {
        this.perspectives.get(MAIN_KEY).addMenuItem(menuItem);
    }

    /**
     * A top menu bar perspective.
     * 
     * @author Johann Kohler
     * 
     */
    public static class TopMenuBarPerspective extends Menu {

        private boolean isMain;
        private Html label;

        /**
         * Construct a perspective.
         * 
         * @param isMain
         *            true if the perspective is the main (only one should be
         *            the main)
         * @param label
         *            the displayed label to switch from a perspective to
         *            another (usually a logo)
         */
        private TopMenuBarPerspective(boolean isMain, Html label) {
            this.isMain = isMain;
            this.label = label;
        }

        /**
         * @return the isMain
         */
        public boolean isMain() {
            return isMain;
        }

        /**
         * @return the label
         */
        public Html getLabel() {
            return label;
        }

    }

}
