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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import play.Configuration;
import play.Logger;
import play.cache.Cache;
import play.inject.ApplicationLifecycle;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import play.twirl.api.Html;
import scala.concurrent.duration.Duration;
import akka.actor.Cancellable;

import com.fasterxml.jackson.databind.JsonNode;

import framework.commons.IFrameworkConstants;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.database.IDatabaseDependencyService;
import framework.services.system.ISysAdminUtils;

/**
 * Default implementation of the {@link IAdPanelManagerService}.<br/>
 * This one loads the configuration from a JSON file.
 * 
 * <pre>
 * {@code
 *      {
 *      "panelisables":["default","roadmap"]
 *      }
 * }
 * </pre>
 * 
 * @author Johann Kohler
 * @author Pierre-Yves Cloux
 * 
 */
@Singleton
public class AdPanelServiceImpl implements IAdPanelManagerService {
    private static Logger.ALogger log = Logger.of(AdPanelServiceImpl.class);

    /**
     * AdConfig file download timeout (in ms).
     */
    private static final int ADCONFIG_DOWNLOAD_TIMEOUT = 4000;

    /**
     * How much seconds to wait before starting looking for the configuration.
     */
    private static final int WAIT_BEFORE_START = 30;

    /**
     * Name of the JSON file which contains the configuration for the AD
     * service.
     */
    private static final String AD_CONFIG_FILE = "adconfig.json";

    /**
     * Pattern of the HTML file for a panel.
     */
    private static final String AD_HTML_FILE_PATTERN = "{name}_{language}.html";

    /**
     * Name of the default panel.
     * 
     * So the HTML file for the default panel is:<br/>
     * {AD_DEFAULT_NAME}_{language}, for example default_en.html
     */
    private static final String AD_DEFAULT_NAME = "default";

    /**
     * Page name of the home page (because the route is empty).
     * 
     * So the HTML file for the specific panel of the home page is:<br/>
     * {AD_HOME_PAGE_NAME}_{language}.html, for example home_fr.html
     */
    private static final String AD_HOME_PAGE_NAME = "home";

    /**
     * Define if the service is active.
     */
    private boolean isActive;

    /**
     * The names of the pages which are associated with a fragment.
     */
    private Set<String> panelisablePages;

    /**
     * The duration of the cache (after this time the cache is cleared)
     */
    private int cacheDuration;

    /**
     * Root URL of the panels container (to get the content)
     */
    private String rootUrl;

    /**
     * Scheduler which regularly retrieve the list of panelisable pages
     */
    private Cancellable currentScheduler;

    private ISysAdminUtils sysAdminUtils;
    private IImplementationDefinedObjectService implementationDefinedObjectService;
    private II18nMessagesPlugin i18nMessagesPlugin;

    public enum Config {
        PANEL_IS_ACTIVE("maf.ad_panel.is_active"), PANEL_URL("maf.ad_panel.url"), PANEL_CACHE_TTL("maf.ad_panel.cache_ttl");

        private String configurationKey;

        private Config(String configurationKey) {
            this.configurationKey = configurationKey;
        }

        public String getConfigurationKey() {
            return configurationKey;
        }
    }

    /**
     * Creates an new Ads service
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param sysAdminUtils
     *            the sysadmin utilities
     * @param implementationDefinedObjectService
     * @param databaseDependencyService
     */
    @Inject
    public AdPanelServiceImpl(ApplicationLifecycle lifecycle, Configuration configuration, ISysAdminUtils sysAdminUtils,
            IImplementationDefinedObjectService implementationDefinedObjectService, II18nMessagesPlugin i18nMessagesPlugin,
            IDatabaseDependencyService databaseDependencyService) {
        log.info("SERVICE>>> AdPanelServiceImpl starting...");
        this.sysAdminUtils = sysAdminUtils;
        this.i18nMessagesPlugin = i18nMessagesPlugin;
        this.implementationDefinedObjectService = implementationDefinedObjectService;
        this.isActive = configuration.getBoolean(Config.PANEL_IS_ACTIVE.getConfigurationKey());
        this.cacheDuration = configuration.getInt(Config.PANEL_CACHE_TTL.getConfigurationKey());
        this.rootUrl = configuration.getString(Config.PANEL_URL.getConfigurationKey());
        this.panelisablePages = Collections.synchronizedSet(new HashSet<String>());
        init();
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> AdPanelServiceImpl stopping...");
            destroy();
            log.info("SERVICE>>> AdPanelServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> AdPanelServiceImpl started");
    }

    public void init() {
        if (this.isActive) {
            // Initialize the scheduler which retrieves regularly the list of
            // panelisable pages
            this.currentScheduler = getSysAdminUtils().scheduleRecurring(true, "ADPANEL", Duration.create(WAIT_BEFORE_START, TimeUnit.SECONDS),
                    Duration.create(getCacheDuration(), TimeUnit.SECONDS), new Runnable() {
                        @Override
                        public void run() {
                            updatePanelisablePages();
                        }
                    }, true);
        }
    }

    public void destroy() {
        try {
            clearCache();
            if (getCurrentScheduler() != null) {
                getCurrentScheduler().cancel();
            }
        } catch (Exception e) {
            log.error("Error while stopping the adpanel service");
        }
    }

    /**
     * Retrieve the list of panelisable pages
     */
    private void updatePanelisablePages() {
        try {
            String url = getRootUrl() + AD_CONFIG_FILE;
            WSResponse response = WS.url(url).get().get(ADCONFIG_DOWNLOAD_TIMEOUT);
            if (response.getStatus() == 200) {
                String adConfigJson = response.getBody();
                loadPanelisablePages(adConfigJson);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No panelisable pages available, clearing the entries");
                    clearCache();
                }
            }
        } catch (Exception e) {
            log.error("Error while retreiving the list of panelisable pages", e);
        }
    }

    /**
     * Load the panelisable pages from the specified JSON structure
     * 
     * @param adConfigJson
     *            a JSON file which contains the configuration for the ad
     *            manager
     */
    private void loadPanelisablePages(String adConfigJson) {
        JsonNode jsonConfig = Json.parse(adConfigJson);
        Iterator<JsonNode> panelisables = jsonConfig.get("panelisables").iterator();
        clearCache();
        while (panelisables.hasNext()) {
            getPanelisablePages().add(panelisables.next().asText());
        }
    }

    @Override
    public void clearCache() {
        for (String panelisablePageName : panelisablePages) {
            Cache.remove(pageNameToCacheKey(panelisablePageName));
        }
        panelisablePages.clear();
    }

    @Override
    public boolean isPanelisable(String route) {
        String page = routeToPage(route);
        return getPanelisablePages().contains(page) || getPanelisablePages().contains(AD_DEFAULT_NAME);
    }

    @Override
    public Html getPanel(String route) {
        String page = routeToPage(route);
        // Check if a content is available for the specified page
        if (getPanelisablePages().contains(page)) {
            return getContentForPageIfAny(page);
        }
        // Alternatively retrieve the default content (if any)
        if (getPanelisablePages().contains(AD_DEFAULT_NAME)) {
            return getContentForPageIfAny(AD_DEFAULT_NAME);
        }
        // No content available
        return new Html("");
    }

    /**
     * Return the content associated with the specified page either from the
     * cache or return an HTML fragment which will download it asynchronously.
     * 
     * @param page
     *            a page name
     * @return
     */
    private Html getContentForPageIfAny(String page) {
        String specificKey = pageNameToCacheKey(page);
        // try to get the specific panel from the cache, if not found then
        // render a script to retrieve it from the remote site
        String specificPanel = (String) Cache.get(specificKey);
        if (specificPanel != null) {
            if (log.isDebugEnabled()) {
                log.debug("found in cache panel for the page '" + page);
            }
            return new Html(specificPanel);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("request the load of panel for the page '" + page);
            }
            // Display a javascript which will retrieve the missing content
            // element without blocking the page
            return views.html.framework_views.parts.asynchronous_text.render("maf_adpanel",
                    getImplementationDefinedObjectService().getRouteForAdPanelContent(page).url());
        }
    }

    @Override
    public Promise<Result> getRemotePanel(final String page) {
        String lang = getI18nMessagesPlugin().getCurrentLanguage().getCode();
        String url = getRootUrl() + AD_HTML_FILE_PATTERN.replace("{name}", page).replace("{language}", lang);
        try {
            final Promise<Result> resultPromise = WS.url(url).get().map(new Function<WSResponse, Result>() {
                public Result apply(WSResponse response) {
                    String content = "";
                    if (response.getStatus() == 200) {
                        if (log.isDebugEnabled()) {
                            log.debug("No content for page " + page);
                        }
                        content = response.getBody();
                    }
                    String cacheKey = pageNameToCacheKey(page);
                    Cache.set(cacheKey, content, getCacheDuration());
                    return Controller.ok(content);
                }
            });
            return resultPromise;
        } catch (Exception e) {
            log.error("Error while retrieving the page " + page);
        }
        return Promise.promise(new Function0<Result>() {
            @Override
            public Result apply() throws Throwable {
                return Controller.ok();
            }
        });
    }

    /**
     * Convert a route into a page identifier (this is the one used to retrieve
     * the fragment)
     */
    private static String routeToPage(String route) {
        String page = route.replaceAll("[^a-zA-Z0-9_]", "");
        if (page.equals("")) {
            page = AD_HOME_PAGE_NAME;
        }
        return page;
    }

    /**
     * Convert a page name into a cache key
     * 
     * @return
     */
    private String pageNameToCacheKey(String page) {
        String lang = getI18nMessagesPlugin().getCurrentLanguage().getCode();
        return IFrameworkConstants.AD_CACHE_PREFIX + lang + "." + page;
    }

    private Set<String> getPanelisablePages() {
        return panelisablePages;
    }

    private int getCacheDuration() {
        return cacheDuration;
    }

    private String getRootUrl() {
        return rootUrl;
    }

    private Cancellable getCurrentScheduler() {
        return currentScheduler;
    }

    private ISysAdminUtils getSysAdminUtils() {
        return sysAdminUtils;
    }

    private IImplementationDefinedObjectService getImplementationDefinedObjectService() {
        return implementationDefinedObjectService;
    }

    private II18nMessagesPlugin getI18nMessagesPlugin() {
        return i18nMessagesPlugin;
    }
}
