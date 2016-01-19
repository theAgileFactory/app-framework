package framework.services.plugins;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.fasterxml.jackson.annotation.JsonIgnore;

import models.framework_models.plugin.DashboardRowTemplate;

/**
 * A service to manage the user dashboard
 * @author Pierre-Yves Cloux
 */
public interface IDashboardService {
    public static final Long NO_WIDGET_ID=-1l;
    
    /**
     * Return the currently available widgets
     * @return a list of catalog entry
     * @throws DashboardException
     */
    public List<WidgetCatalogEntry> getWidgetCatalog() throws DashboardException;
    
    /**
     * Creates a new widget from the widget catalog entry
     * and return its unique id as well as its display URL.<br/>
     * This throws an exception of this widget entry is not consistent.
     * @param dashboardPageId the id of the dashboard page
     * @param uid the UID of a user or null (if null the current user is used)
     * @param widgetCatalogEntry an entry of the widget catalog (list of active and available widgets)
     * @param widgetTitle a title for the widget (as displayed in the widget header)
     */
    public Pair<Long,String> createNewWidget(Long dashboardPageId, String uid, WidgetCatalogEntry widgetCatalogEntry, String widgetTitle) throws DashboardException;

    /**
     * Delete a new widget from a dashboard page.
     * This throws an exception of this widget entry is not consistent.
     * @param dashboardPageId the id of the dashboard page
     * @param uid the UID of a user or null (if null the current user is used)
     * @param widgetId the unique id of the widget to be deleted
     */
    public void removeWidget(Long dashboardPageId, String uid, Long widgetId) throws DashboardException;

    /**
     * Return the list of dashboard pages for a named user.<br/>
     * This is a list of tuples:
     * <ul>
     * <li>[1] the page title/name</li>
     * <li>[2] true if the page is the user home page (default displayed)</li>
     * <li>[3] the dashboard page id</li>
     * </ul>
     * @param uid the UID of a user or null (if null the current user is used)
     * @return 
     */
    public List<Triple<String, Boolean, Long>> getDashboardPages(String uid) throws DashboardException;
    
    /**
     * Return the id of the dashboard page.
     * @param uid the UID of a user or null (if null the current user is used)
     * @return 
     */
    public Long getHomeDashboardPageId(String uid) throws DashboardException;
    
    /**
     * Return the DashboardPage configuration for the specified id and the specified user.<br/>
     * The code must check if the specified dashboard page belongs to the specified user.
     * This is a tuple:
     * <ul>
     * <li>[1] the page title/name</li>
     * <li>[2] true if the page is the user home page (default displayed)</li>
     * <li>[3] the page content configuration</li>
     * </ul>
     * @param dashboardPageId a unique dashboard page Id
     * @param uid the UID of a user or null (if null the current user is used)
     * @return 
     */
    public Triple<String, Boolean, List<DashboardRowConfiguration>> getDashboardPageConfiguration(Long dashboardPageId, String uid) throws DashboardException;
    
    /**
     * Update the configuration of the dashboard page.<br/>
     * The code must check if the specified dashboard page belongs to the specified user.
     * @param dashboardPageId the id of the dashboard page
     * @param uid the UID of a user or null (if null the current user is used)
     * @param config the page configuration
     * @throws DashboardException
     */
    public void updateDashboardPageConfiguration(Long dashboardPageId, String uid, List<DashboardRowConfiguration> config) throws DashboardException;
    
    /**
     * Update the name of a dashboard page.<br/>
     * The code must check if the specified dashboard page belongs to the specified user.
     * @param dashboardPageId the id of the dashboard page
     * @param uid the UID of a user or null (if null the current user is used)
     * @param name the name of the page to be updated
     * @throws DashboardException
     */
    public void updateDashboardPageName(Long dashboardPageId, String uid, String name) throws DashboardException;
    
    
    /**
     * Set the specified page as home.<br/>
     * The code must check if the specified dashboard page belongs to the specified user.
     * @param dashboardPageId the id of the dashboard page
     * @param uid the UID of a user or null (if null the current user is used)
     * @throws DashboardException
     */
    public void setDashboardPageAsHome(Long dashboardPageId, String uid) throws DashboardException;
    
    
    /**
     * Create the configuration of the dashboard page.<br/>
     * @param uid the UID of a user or null (if null the current user is used)
     * @param name the name/title of the page
     * @param isHome true if the page is the home page of the dashboard
     * @param config the page configuration
     * @return the unique id of the dashboard page
     * @throws DashboardException
     */
    public Long createDashboardPage(String uid, String name, Boolean isHome, List<DashboardRowConfiguration> config) throws DashboardException;
    
    /**
     * Delete the configuration of the dashboard page.<br/>
     * The code must check if the specified dashboard page belongs to the specified user.
     * @param dashboardPageId the id of the dashboard page
     * @param uid the UID of a user or null (if null the current user is used)
     * @throws DashboardException
     */
    public void deleteDashboardPage(Long dashboardPageId, String uid) throws DashboardException;
    
    
    /**
     * A data structure which represents a dashboard row configuration.<br/>
     * It consists in :
     * <ul>
     * <li>a layout : instance of {@link DashboardRowTemplate}</li>
     * <li>widgetIds : an ordered list of widgets ids</li>
     * </ul>
     * @author Pierre-Yves Cloux
     */
    public static class DashboardRowConfiguration{        
        private DashboardRowTemplate layout;
        private List<WidgetConfiguration> widgets;
        
        public DashboardRowConfiguration() {
            super();
        }

        public DashboardRowTemplate getLayout() {
            return layout;
        }
        public void setLayout(DashboardRowTemplate layout) {
            this.layout = layout;
        }
        public List<WidgetConfiguration> getWidgets() {
            return widgets;
        }
        public void setWidgets(List<WidgetConfiguration> widgets) {
            this.widgets = widgets;
        }
        
        @JsonIgnore
        public Set<Long> getWidgetIds(){
            HashSet<Long> widgetIds=new HashSet<>();
            if(widgets!=null){
                for(WidgetConfiguration widgetConfig:widgets){
                    widgetIds.add(widgetConfig.getId());
                }
            }
            return widgetIds;
        }
        
        public static class WidgetConfiguration{
            private Long id;
            private String url;
            
            public WidgetConfiguration() {
                super();
            }
            public WidgetConfiguration(Long id, String url) {
                super();
                this.id = id;
                this.url = url;
            }
            public Long getId() {
                return id;
            }
            public void setId(Long id) {
                this.id = id;
            }
            public String getUrl() {
                return url;
            }
            public void setUrl(String url) {
                this.url = url;
            }
        }
    }
    
    /**
     * A data structure which is to be used to manage a widget catalog entry.<br/>
     * This one contains:
     * <ul>
     * <li>pluginConfigurationName : the name of the plugin configuration</li>
     * <li>pluginConfigurationId : the plugin configuration id</li>
     * <li>identifier : the widget identifier</li>
     * <li>name : the name of the widget</li>
     * <li>description : the description of the widget</li>
     * </ul>
     * @author Pierre-Yves Cloux
     */
    public static class WidgetCatalogEntry{
        private String pluginConfigurationName;
        private Long pluginConfigurationId;
        private String identifier;
        private String name;
        private String description;
        public String getPluginConfigurationName() {
            return pluginConfigurationName;
        }
        public void setPluginConfigurationName(String pluginConfigurationName) {
            this.pluginConfigurationName = pluginConfigurationName;
        }
        public Long getPluginConfigurationId() {
            return pluginConfigurationId;
        }
        public void setPluginConfigurationId(Long pluginConfigurationId) {
            this.pluginConfigurationId = pluginConfigurationId;
        }
        public String getIdentifier() {
            return identifier;
        }
        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        @Override
        public String toString() {
            return "WidgetCatalogEntry [pluginConfigurationName=" + pluginConfigurationName + ", pluginConfigurationId=" + pluginConfigurationId
                    + ", identifier=" + identifier + ", name=" + name + ", description=" + description + "]";
        }
    }
}
