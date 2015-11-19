package framework.services.configuration;

import java.util.Map;

import framework.utils.Menu;
import framework.utils.Menu.MenuItem;
import play.twirl.api.Html;

/**
 * The service which is managing the top menu bar for the application
 * 
 * @author Pierre-Yves Cloux
 */
public interface ITopMenuBarService {
    /**
     * Key to be used to the "top level" menus Ids
     */
    public static final String MAIN_PERSPECTIVE_KEY = "main";

    /**
     * Return true if a user is connected
     * 
     * @return
     */
    public boolean isUserConnected();

    /**
     * Get the perspective key from the user preference.
     */
    public String getPerspectiveFromPreference();

    /**
     * Set the perspective in the user preference.
     * 
     * @param key
     *            the perspective key
     */
    public void setPerspectiveFromPreference(String key);

    /**
     * Add a secondary perspective.
     * 
     * @param key
     *            the key to identify the perspective
     * @param label
     *            the displayed label to switch from a perspective to another
     *            (usually a logo)
     */
    public void addPerspective(String key, Html label);

    /**
     * Get all perspectives.
     */
    public Map<String, TopMenuBarPerspective> getAllPerspectives();

    /**
     * Get a perspective by key.
     * 
     * @param key
     *            the key
     */
    public TopMenuBarPerspective getPerspective(String key);

    /**
     * Get the current perspective of the sign-in user.
     * 
     * If the user is not sign-in then return the main.
     */
    public TopMenuBarPerspective getCurrentPerspective();

    /**
     * Return true if there are secondary perspectives.
     */
    public boolean hasSecondaryPerspectives();

    /**
     * Clear all perspectives.
     */
    public void clearAllPerspectives();

    /**
     * Get the main perspective.
     */
    public TopMenuBarPerspective getMainPerspective();

    /**
     * Add a menu item to the main perspective.
     * 
     * @param menuItem
     *            the menu item to add
     */
    public void addMenuItemToMainPerspective(MenuItem menuItem);

    /**
     * Add dynamic tool menu.<br/>
     * This action is to be used to modify a menu which is welcoming some
     * external tools (example : plugins).
     * 
     * @param menuItem
     *            the menu item to add
     */
    public void addToolMenuItem(MenuItem menuItem);

    /**
     * Remove a dynamic tool menu.
     * 
     * @param uuid
     *            the uuid of the menu item to be removed
     */
    public void removeToolMenuItem(String uuid);

    /**
     * Reset the top menu bar to defaults
     */
    public void resetTopMenuBar();

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
        public TopMenuBarPerspective(boolean isMain, Html label) {
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
