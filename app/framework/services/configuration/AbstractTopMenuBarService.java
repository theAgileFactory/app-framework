package framework.services.configuration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import framework.commons.IFrameworkConstants;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.session.IUserSessionManagerPlugin;
import framework.utils.Menu.MenuItem;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import play.mvc.Http.Context;
import play.twirl.api.Html;

/**
 * The root implementation for any {@link ITopMenuBarService}.
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public abstract class AbstractTopMenuBarService implements ITopMenuBarService {
    private static Logger.ALogger log = Logger.of(AbstractTopMenuBarService.class);
    private Map<String, TopMenuBarPerspective> perspectives;

    private IPreferenceManagerPlugin preferenceManagerPlugin;
    private IUserSessionManagerPlugin userSessionManagerPlugin;

    @Inject
    public AbstractTopMenuBarService(ApplicationLifecycle lifecycle, Configuration configuration, IPreferenceManagerPlugin preferenceManagerPlugin,
            IUserSessionManagerPlugin userSessionManagerPlugin) {
        log.info("SERVICE>>> AbstractTopMenuBarService starting...");
        this.preferenceManagerPlugin = preferenceManagerPlugin;
        this.userSessionManagerPlugin = userSessionManagerPlugin;
        this.perspectives = Collections.synchronizedMap(new LinkedHashMap<>());
        this.perspectives.put(MAIN_PERSPECTIVE_KEY, new TopMenuBarPerspective(true, views.html.framework_views.parts.menubars.logo.render()));
        resetTopMenuBar();
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> AbstractTopMenuBarService stopping...");
            log.info("SERVICE>>> AbstractTopMenuBarService stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> AbstractTopMenuBarService started");
    }

    /**
     * Get the perspective key from the user preference.
     */
    public String getPerspectiveFromPreference() {
        String key = getPreferenceManagerPlugin().getPreferenceValueAsString(IFrameworkConstants.CURRENT_PERSPECTIVE_PREFERENCE);
        if (key == null || key.equals("")) {
            return MAIN_PERSPECTIVE_KEY;
        } else {
            return key;
        }
    }

    /**
     * Set the perspective in the user preference.
     * 
     * @param key
     *            the perspective key
     */
    public void setPerspectiveFromPreference(String key) {
        getPreferenceManagerPlugin().updatePreferenceValue(IFrameworkConstants.CURRENT_PERSPECTIVE_PREFERENCE, key);
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
    public void addPerspective(String key, Html label) {
        this.perspectives.put(key, new TopMenuBarPerspective(false, label));
    }

    /**
     * Get all perspectives.
     */
    public Map<String, TopMenuBarPerspective> getAllPerspectives() {
        return this.perspectives;
    }

    /**
     * Get a perspective by key.
     * 
     * @param key
     *            the key
     */
    public TopMenuBarPerspective getPerspective(String key) {
        return this.perspectives.get(key);
    }

    /**
     * Get the current perspective of the sign-in user.
     * 
     * If the user is not sign-in then return the main.
     */
    public TopMenuBarPerspective getCurrentPerspective() {
        if (this.isUserConnected()) {
            return this.perspectives.get(this.getPerspectiveFromPreference());
        } else {
            return this.getMainPerspective();
        }

    }

    /**
     * Return true if a user is connected
     * 
     * @return
     */
    public boolean isUserConnected() {
        return getUserSessionManagerPlugin().getUserSessionId(Context.current()) != null;
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
    public void clearAllPerspectives() {
        for (Map.Entry<String, TopMenuBarPerspective> entry : this.perspectives.entrySet()) {
            entry.getValue().clear();
        }
    }

    /**
     * Get the main perspective.
     */
    public TopMenuBarPerspective getMainPerspective() {
        return this.perspectives.get(MAIN_PERSPECTIVE_KEY);
    }

    /**
     * Add a menu item to the main perspective.
     * 
     * @param menuItem
     *            the menu item to add
     */
    public void addMenuItemToMainPerspective(MenuItem menuItem) {
        this.perspectives.get(MAIN_PERSPECTIVE_KEY).addMenuItem(menuItem);
    }

    /**
     * Reset the top menu bar to defaults
     */
    public abstract void resetTopMenuBar();

    private IPreferenceManagerPlugin getPreferenceManagerPlugin() {
        return preferenceManagerPlugin;
    }

    private IUserSessionManagerPlugin getUserSessionManagerPlugin() {
        return userSessionManagerPlugin;
    }
}
