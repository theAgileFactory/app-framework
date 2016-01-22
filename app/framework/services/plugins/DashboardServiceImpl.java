package framework.services.plugins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import framework.security.ISecurityService;
import framework.services.account.IUserAccount;
import framework.services.ext.api.IExtensionDescriptor.IWidgetDescriptor;
import framework.services.plugins.IPluginManagerService.IPluginInfo;
import models.framework_models.account.Principal;
import models.framework_models.plugin.DashboardPage;
import models.framework_models.plugin.DashboardWidget;
import models.framework_models.plugin.DashboardWidgetColor;
import models.framework_models.plugin.PluginConfiguration;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;

/**
 * The service which is managing the dashboards
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class DashboardServiceImpl implements IDashboardService {
    private static Logger.ALogger log = Logger.of(DashboardServiceImpl.class);
    private IPluginManagerService pluginManagerService;
    private ISecurityService securityService;
    private ObjectMapper mapper;

    @Inject
    public DashboardServiceImpl(ApplicationLifecycle lifecycle, Configuration configuration, IPluginManagerService pluginManagerService,
            ISecurityService securityService) {
        log.info("SERVICE>>> DashboardServiceImpl starting...");
        this.pluginManagerService = pluginManagerService;
        this.securityService = securityService;
        this.mapper = new ObjectMapper();
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> DashboardServiceImpl stopping...");
            log.info("SERVICE>>> DashboardServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> DashboardServiceImpl started");
    }

    @Override
    public List<WidgetCatalogEntry> getWidgetCatalog() throws DashboardException {
        if (log.isDebugEnabled()) {
            log.debug("Requesting the widget catalog");
        }
        List<WidgetCatalogEntry> catalog = new ArrayList<>();
        Map<Long, IPluginInfo> registeredPlugins = getPluginManagerService().getRegisteredPluginDescriptors();
        if (registeredPlugins != null) {
            for (Long pluginConfigurationId : registeredPlugins.keySet()) {
                IPluginInfo info = registeredPlugins.get(pluginConfigurationId);
                Map<String, IWidgetDescriptor> widgetDescriptors = info.getDescriptor().getWidgetDescriptors();
                if (widgetDescriptors != null) {
                    for (String identifier : widgetDescriptors.keySet()) {
                        IWidgetDescriptor widgetDescriptor = widgetDescriptors.get(identifier);
                        WidgetCatalogEntry entry = new WidgetCatalogEntry();
                        entry.setPluginConfigurationId(pluginConfigurationId);
                        entry.setPluginConfigurationName(info.getPluginConfigurationName());
                        entry.setIdentifier(identifier);
                        entry.setName(widgetDescriptor.getName());
                        entry.setDescription(widgetDescriptor.getDescription());
                        entry.setHasEditMode(widgetDescriptor.getHasEditMode());
                        catalog.add(entry);
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Returning the catalog " + catalog);
        }
        return catalog;
    }

    @Override
    public Pair<Long, String> createNewWidget(Long dashboardPageId, String uid, WidgetCatalogEntry widgetCatalogEntry, String widgetTitle)
            throws DashboardException {
        if (log.isDebugEnabled()) {
            log.debug("Creating widget " + widgetCatalogEntry + " for user " + (uid == null ? "current" : uid));
        }
        IUserAccount userAccount = getUserAccount(uid);

        Pair<Long, String> widgetConfig = null;
        Ebean.beginTransaction();
        try {
            DashboardPage dashboardPage = DashboardPage.find.where().eq("id", dashboardPageId).findUnique();
            if (dashboardPage == null || !userAccount.getUid().equals(dashboardPage.principal.uid)) {
                if (log.isDebugEnabled()) {
                    log.debug("No page found for id " + dashboardPageId + " and user " + userAccount.getUid());
                }
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug("Trying to find an active widget " + widgetCatalogEntry);
            }
            IPluginInfo info = getPluginManagerService().getRegisteredPluginDescriptors().get(widgetCatalogEntry.getPluginConfigurationId());
            if (log.isDebugEnabled()) {
                log.debug("Found an active plugin with id " + widgetCatalogEntry.getPluginConfigurationId());
            }
            IWidgetDescriptor widgetDescriptor = info.getDescriptor().getWidgetDescriptors().get(widgetCatalogEntry.getIdentifier());
            if (log.isDebugEnabled()) {
                log.debug("Widget " + widgetCatalogEntry + " found, creating the widget entry");
            }
            DashboardWidget widget = new DashboardWidget();
            widget.color = DashboardWidgetColor.PRIMARY.getColor();
            widget.dashboardPage = dashboardPage;
            widget.identifier = widgetDescriptor.getIdentifier();
            widget.pluginConfiguration = PluginConfiguration.getPluginById(widgetCatalogEntry.getPluginConfigurationId());
            widget.title = widgetTitle;
            widget.config = null;
            widget.save();
            if (log.isDebugEnabled()) {
                log.debug("Widget " + widgetCatalogEntry + " created with id " + widget.id);
            }
            String widgetUrl = info.getLinkToDisplayWidget(widgetDescriptor.getIdentifier(), widget.id);
            if (log.isDebugEnabled()) {
                log.debug("Widget " + widget.id + " is associated with display url " + widgetUrl);
            }
            widgetConfig = Pair.of(widget.id, widgetUrl);
            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Error while creating the widget " + widgetCatalogEntry + " for user uid=%s", userAccount.getUid());
            log.error(message, e);
            throw new DashboardException(message, e);
        } finally {
            Ebean.endTransaction();
        }
        return widgetConfig;
    }

    @Override
    public void removeWidget(Long dashboardPageId, String uid, Long widgetId) throws DashboardException {
        if (log.isDebugEnabled()) {
            log.debug("Request for dashboard pages for user " + (uid == null ? "current" : uid));
        }
        IUserAccount userAccount = getUserAccount(uid);

        Ebean.beginTransaction();
        try {
            DashboardWidget widget = DashboardWidget.find.where().eq("id", widgetId).eq("dashboardPage.id", dashboardPageId).findUnique();
            if (widget != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Found a widget with id " + widgetId + " in dashboard page " + dashboardPageId + " : deleting");
                }
                widget.delete();
            }
            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Error while getting the dashboard configuration for account uid=%s", userAccount.getUid());
            log.error(message, e);
            throw new DashboardException(message, e);
        } finally {
            Ebean.endTransaction();
        }
    }

    @Override
    public Long getHomeDashboardPageId(String uid) throws DashboardException {
        if (log.isDebugEnabled()) {
            log.debug("Request for dashboard pages for user " + (uid == null ? "current" : uid));
        }
        IUserAccount userAccount = getUserAccount(uid);

        Long dashboardPageId = null;
        Ebean.beginTransaction();
        try {
            DashboardPage dashboardHomePage = DashboardPage.find.where().eq("principal.id", userAccount.getMafUid()).eq("isHome", true).findUnique();
            if (dashboardHomePage != null) {
                dashboardPageId = dashboardHomePage.id;
                if (log.isDebugEnabled()) {
                    log.debug("Found " + dashboardPageId + " home page for user " + userAccount.getUid());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Found no home page for user " + userAccount.getUid());
                }
            }
            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Error while getting the dashboard configuration for account uid=%s", userAccount.getUid());
            log.error(message, e);
            throw new DashboardException(message, e);
        } finally {
            Ebean.endTransaction();
        }
        return dashboardPageId;
    }

    @Override
    public List<Triple<String, Boolean, Long>> getDashboardPages(String uid) throws DashboardException {
        if (log.isDebugEnabled()) {
            log.debug("Request for dashboard pages for user " + (uid == null ? "current" : uid));
        }
        IUserAccount userAccount = getUserAccount(uid);

        List<Triple<String, Boolean, Long>> dashboardConfig = null;
        Ebean.beginTransaction();
        try {
            List<DashboardPage> dashboardPages = DashboardPage.find.where().eq("principal.id", userAccount.getMafUid()).findList();
            if (log.isDebugEnabled()) {
                log.debug("Found " + dashboardPages.size() + " pages for user " + userAccount.getUid());
            }
            if (dashboardPages != null) {
                dashboardConfig = new ArrayList<>();
                for (DashboardPage dashboardPage : dashboardPages) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found dashboard page " + dashboardPage.name + " with id " + dashboardPage.id);
                    }
                    dashboardConfig.add(Triple.of(dashboardPage.name, dashboardPage.isHome, dashboardPage.id));
                }
            }
            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Error while getting the dashboard configuration for account uid=%s", userAccount.getUid());
            log.error(message, e);
            throw new DashboardException(message, e);
        } finally {
            Ebean.endTransaction();
        }
        return dashboardConfig;
    }

    @Override
    public Triple<String, Boolean, List<DashboardRowConfiguration>> getDashboardPageConfiguration(Long dashboardPageId, String uid)
            throws DashboardException {
        if (log.isDebugEnabled()) {
            log.debug("Request for dashboard page " + dashboardPageId + " configuration for user " + (uid == null ? "current" : uid));
        }
        IUserAccount userAccount = getUserAccount(uid);
        String pageName = null;
        Boolean isHome = null;
        List<DashboardRowConfiguration> rows = null;
        Ebean.beginTransaction();
        try {
            DashboardPage dashboardPage = DashboardPage.find.where().eq("id", dashboardPageId).findUnique();
            if (dashboardPage == null || !userAccount.getUid().equals(dashboardPage.principal.uid)) {
                if (log.isDebugEnabled()) {
                    log.debug("No page found for id " + dashboardPageId + " and user " + userAccount.getUid());
                }
                return null;
            }
            // Extract the data stored into the database
            try {
                rows = getMapper().readValue(dashboardPage.layout, new TypeReference<List<DashboardRowConfiguration>>() {
                });
                pageName = dashboardPage.name;
                isHome = dashboardPage.isHome;
            } catch (Exception exp) {
                log.error("Unable to read the content of the dashboard configuration from the database ", exp);
                throw exp;
            }
            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("No dashboard configuration page for %d", dashboardPageId);
            log.error(message, e);
            throw new DashboardException(message, e);
        } finally {
            Ebean.endTransaction();
        }
        return Triple.of(pageName, isHome, rows);
    }

    @Override
    public Long createDashboardPage(String uid, String name, Boolean isHome, List<DashboardRowConfiguration> config) throws DashboardException {
        if (log.isDebugEnabled()) {
            log.debug("Creating dashboard page for user " + (uid == null ? "current" : uid));
        }
        IUserAccount userAccount = getUserAccount(uid);
        Ebean.beginTransaction();
        Long dashboardPageId = null;
        try {
            if (isHome) {
                // Check if a home page is already defined and then change it
                DashboardPage existingHomePage = DashboardPage.find.where().eq("principal.id", userAccount.getMafUid()).eq("isHome", true).findUnique();
                if (existingHomePage != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found another home page " + existingHomePage.id + " updating");
                    }
                    existingHomePage.isHome = false;
                    existingHomePage.save();
                }
            }
            // Create a new dashboard page
            DashboardPage dashboardPage = new DashboardPage();
            dashboardPage.name = name;
            dashboardPage.isHome = isHome;
            dashboardPage.layout = getMapper().writeValueAsBytes(config);
            dashboardPage.principal = Principal.getPrincipalFromId(userAccount.getMafUid());
            dashboardPage.save();
            if (log.isDebugEnabled()) {
                log.debug("Dashboard page has been created with id " + dashboardPage.id);
            }
            dashboardPageId = dashboardPage.id;
            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Cannot create a new dashboard page for user %s", uid);
            log.error(message, e);
            throw new DashboardException(message, e);
        } finally {
            Ebean.endTransaction();
        }
        return dashboardPageId;
    }

    @Override
    public void updateDashboardPageConfiguration(Long dashboardPageId, String uid, List<DashboardRowConfiguration> config) throws DashboardException {
        if (log.isDebugEnabled()) {
            log.debug("Updating dashboard page configuration " + dashboardPageId + " for user " + (uid == null ? "current" : uid));
        }
        IUserAccount userAccount = getUserAccount(uid);
        Ebean.beginTransaction();
        try {
            DashboardPage dashboardPage = DashboardPage.find.where().eq("id", dashboardPageId).findUnique();
            // Check the user is the right one
            if (dashboardPage == null || !userAccount.getUid().equals(dashboardPage.principal.uid)) {
                if (log.isDebugEnabled()) {
                    log.debug("No page found for id " + dashboardPageId + " and user " + userAccount.getUid());
                }
                return;
            }
            // Listing all the widgets in the current dashboard to identify some
            // which must be deleted
            Set<Long> newWidgetIds = new HashSet<>();
            for (DashboardRowConfiguration newRow : config) {
                newWidgetIds.addAll(newRow.getWidgetIds());
            }
            // Identify deleted widgets (check for changes)
            for (DashboardWidget dashboardWidget : dashboardPage.dashboardWidgets) {
                if (!newWidgetIds.contains(dashboardWidget.id)) {
                    if (log.isDebugEnabled()) {
                        log.debug("The widget " + dashboardWidget.id + " hash been removed from the dashboard " + dashboardPageId);
                    }
                    // This is a widget which has been deleted (delete it from
                    // the database)
                    dashboardWidget.delete();
                }
                newWidgetIds.remove(dashboardWidget.id);
            }
            // Remove the "empty" widgets
            newWidgetIds.remove(-1l);
            // Check if there are some unknown widget ids
            if (newWidgetIds.size() > 0) {
                throw new DashboardException("Unknown widgets in the dashboard update set " + newWidgetIds);
            }
            // Update the layout
            dashboardPage.layout = getMapper().writeValueAsBytes(config);
            dashboardPage.save();
            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Cannot update dashboard page %d", dashboardPageId);
            log.error(message, e);
            throw new DashboardException(message, e);
        } finally {
            Ebean.endTransaction();
        }
    }

    @Override
    public void updateDashboardPageName(Long dashboardPageId, String uid, String name) throws DashboardException {
        if (log.isDebugEnabled()) {
            log.debug("Updating dashboard page name " + dashboardPageId + " for user " + (uid == null ? "current" : uid));
        }
        IUserAccount userAccount = getUserAccount(uid);
        Ebean.beginTransaction();
        try {
            DashboardPage dashboardPage = DashboardPage.find.where().eq("id", dashboardPageId).findUnique();
            // Check the user is the right one
            if (dashboardPage == null || !userAccount.getUid().equals(dashboardPage.principal.uid)) {
                if (log.isDebugEnabled()) {
                    log.debug("No page found for id " + dashboardPageId + " and user " + userAccount.getUid());
                }
                return;
            }
            dashboardPage.name = name;
            dashboardPage.save();
            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Cannot update dashboard page %d", dashboardPageId);
            log.error(message, e);
            throw new DashboardException(message, e);
        } finally {
            Ebean.endTransaction();
        }
    }

    @Override
    public void setDashboardPageAsHome(Long dashboardPageId, String uid) throws DashboardException {
        if (log.isDebugEnabled()) {
            log.debug("Changing dashboard home page to " + dashboardPageId + " for user " + (uid == null ? "current" : uid));
        }
        IUserAccount userAccount = getUserAccount(uid);
        Ebean.beginTransaction();
        try {
            DashboardPage dashboardPage = DashboardPage.find.where().eq("id", dashboardPageId).findUnique();
            // Check the user is the right one
            if (dashboardPage == null || !userAccount.getUid().equals(dashboardPage.principal.uid)) {
                if (log.isDebugEnabled()) {
                    log.debug("No page found for id " + dashboardPageId + " and user " + userAccount.getUid());
                }
                return;
            }

            // Check if a home page is already defined and then change it
            DashboardPage existingHomePage = DashboardPage.find.where().eq("principal.id", userAccount.getMafUid()).eq("isHome", true).findUnique();
            if (existingHomePage != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Changing the current home page " + existingHomePage.id);
                }
                existingHomePage.isHome = false;
                existingHomePage.save();
            }

            dashboardPage.isHome = true;
            dashboardPage.save();

            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Cannot update dashboard page %d", dashboardPageId);
            log.error(message, e);
            throw new DashboardException(message, e);
        } finally {
            Ebean.endTransaction();
        }
    }

    @Override
    public void deleteDashboardPage(Long dashboardPageId, String uid) throws DashboardException {
        if (log.isDebugEnabled()) {
            log.debug("Deleting dashboard page " + dashboardPageId + " for user " + (uid == null ? "current" : uid));
        }
        IUserAccount userAccount = getUserAccount(uid);
        Ebean.beginTransaction();
        try {
            DashboardPage dashboardPage = DashboardPage.find.where().eq("id", dashboardPageId).findUnique();
            // Check the user is the right one
            if (dashboardPage == null || !userAccount.getUid().equals(dashboardPage.principal.uid)) {
                if (log.isDebugEnabled()) {
                    log.debug("No page found for id " + dashboardPageId + " and user " + userAccount.getUid());
                }
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug("Deleting dashboard page " + dashboardPageId);
            }
            dashboardPage.delete();
            if (dashboardPage.isHome) {
                // If the page is "home" check if a new page can be elected
                // "home"
                List<DashboardPage> dashboardPages = DashboardPage.find.where().eq("principal.id", userAccount.getMafUid()).findList();
                if (dashboardPages != null && dashboardPages.size() != 0) {
                    DashboardPage newHomePage = dashboardPages.get(0);
                    if (log.isDebugEnabled()) {
                        log.debug("Changing home page to " + newHomePage.id);
                    }
                    newHomePage.isHome = true;
                    newHomePage.save();
                }
            }
            Ebean.commitTransaction();
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Cannot delete dashboard page %d", dashboardPageId);
            log.error(message, e);
            throw new DashboardException(message, e);
        } finally {
            Ebean.endTransaction();
        }
    }

    @Override
    public boolean isWidgetExists(Long widgetId) throws DashboardException {
        Ebean.beginTransaction();
        try {
            DashboardWidget widget = DashboardWidget.find.where().eq("id", widgetId).findUnique();
            boolean isWidgetExists = widget != null;
            Ebean.commitTransaction();
            return isWidgetExists;
        } catch (Exception e) {
            Ebean.rollbackTransaction();
            String message = String.format("Error while looking for the widget %d", widgetId);
            log.error(message, e);
            throw new DashboardException(message, e);
        } finally {
            Ebean.endTransaction();
        }
    }

    private IUserAccount getUserAccount(String uid) throws DashboardException {
        IUserAccount userAccount = null;
        try {
            if (uid == null) {
                userAccount = getSecurityService().getCurrentUser();
            } else {
                userAccount = getSecurityService().getUserFromUid(uid);
            }
        } catch (Exception e) {
            throw new DashboardException("Unable to retreive the pages of the specified user", e);
        }
        if (userAccount == null)
            throw new DashboardException("No user account available, cannot proceed with the requested operation");
        return userAccount;
    }

    private IPluginManagerService getPluginManagerService() {
        return pluginManagerService;
    }

    private ISecurityService getSecurityService() {
        return securityService;
    }

    private ObjectMapper getMapper() {
        return mapper;
    }

}
