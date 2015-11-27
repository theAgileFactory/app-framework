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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.TxIsolation;

import akka.actor.Cancellable;
import framework.commons.DataType;
import framework.commons.IFrameworkConstants;
import framework.security.ISecurityService;
import framework.security.ISecurityServiceConfiguration;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.ITopMenuBarService;
import framework.services.database.IDatabaseDependencyService;
import framework.services.ext.ExtensionManagerServiceImpl.Extension.PluginResources;
import framework.services.ext.XmlExtensionDescriptor.I18nMessage;
import framework.services.ext.XmlExtensionDescriptor.MenuCustomizationDescriptor;
import framework.services.ext.XmlExtensionDescriptor.MenuItemDescriptor;
import framework.services.ext.api.AbstractExtensionController;
import framework.services.ext.api.IExtensionDescriptor;
import framework.services.ext.api.IExtensionDescriptor.IPluginDescriptor;
import framework.services.ext.api.WebCommandPath;
import framework.services.ext.api.WebControllerPath;
import framework.services.ext.api.WebParameter;
import framework.services.plugins.api.IPluginContext;
import framework.services.plugins.api.IPluginRunner;
import framework.services.router.ICustomRouterService;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.system.ISysAdminUtils;
import framework.utils.Menu;
import framework.utils.Menu.ClickableMenuItem;
import framework.utils.Menu.HeaderMenuItem;
import framework.utils.Menu.MenuItem;
import framework.utils.Utilities;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.inject.Injector;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.ws.WSClient;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Result;
import scala.concurrent.duration.Duration;

/**
 * The default implementation for the {@link IExtensionManagerService}
 * interface.<br/>
 * Here are the properties:
 * <ul>
 * <li>autoRefreshMode : if true the system will regularly scan the file system
 * for extension changes and load them as they are available</li>
 * <li>autoRefreshFrequency : the frequency of scanning of the extension folder
 * if the autorefresh is configured</li>
 * <li>extensions : the loaded extensions (one JAR per extension)</li>
 * <li>extensionControllers : a map [a controller Class, Map[unique id of a
 * command, a WebCommand instance]]</li>
 * <li>webCommands : the list of registered {@link WebCommand}</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class ExtensionManagerServiceImpl implements IExtensionManagerService {
    private static Logger.ALogger log = Logger.of(ExtensionManagerServiceImpl.class);

    /**
     * True if the system is in "autorefresh" mode (should be used only for
     * development)
     */
    private boolean autoRefreshMode;

    /**
     * The frequency (in seconds) of scanning of the extension folder if the
     * autorefresh is configured
     */
    private int autoRefreshFrequency;

    /**
     * The path which contains the extension jars
     */
    private File extensionDirectory;

    private Cancellable autoRefreshScheduler;
    private II18nMessagesPlugin iI18nMessagesPlugin;
    private Configuration configuration;
    private Environment environment;
    private ISysAdminUtils sysAdminUtils;
    private ISecurityService securityService;
    private ISecurityServiceConfiguration securityServiceConfiguration;
    private Injector injector;
    private IPreferenceManagerPlugin preferenceManagerPlugin;
    private ITopMenuBarService topMenuBarService;
    private List<Extension> extensions = Collections.synchronizedList(new ArrayList<Extension>());
    private Map<Object, Map<String, WebCommand>> extensionControllers = Collections.synchronizedMap(new HashMap<Object, Map<String, WebCommand>>());
    private List<WebCommand> webCommands = Collections.synchronizedList(new ArrayList<WebCommand>());

    public enum Config {
        DIRECTORY_PATH("maf.ext.directory"), AUTO_REFRESH_ACTIVE("maf.ext.auto.refresh.status"), AUTO_REFRESH_FREQUENCY("maf.ext.auto.refresh.frequency");

        private String configurationKey;

        private Config(String configurationKey) {
            this.configurationKey = configurationKey;
        }

        public String getConfigurationKey() {
            return configurationKey;
        }
    }

    /**
     * Creates a new extension manager
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param environment
     *            the play environment
     * @param injector
     *            the application injector
     * @param configuration
     *            the play application configuration
     * @param iI18nMessagesPlugin
     *            the service in charge of the internationalization
     * @param customRouterService
     *            the custom router service to register the extension
     * @param sysAdminUtils
     *            ensure that the service is loaded before being possibly used
     *            by an extension
     * @param implementationDefinedObjectService
     * @param databaseDependencyService
     *            ensure that the service is loaded before being possibly used
     *            by an extension
     * @param securityService
     * @param securityServiceConfiguration
     * @param preferenceManagerPlugin
     * @param topMenuBarService
     * @param wsClient
     *            ensure that the service is loaded before being possibly used
     *            by an extension
     * @throws ExtensionManagerException
     */
    @Inject
    public ExtensionManagerServiceImpl(ApplicationLifecycle lifecycle, Environment environment, Injector injector, Configuration configuration,
            II18nMessagesPlugin iI18nMessagesPlugin, ICustomRouterService customRouterService, ISysAdminUtils sysAdminUtils,
            IDatabaseDependencyService databaseDependencyService, ISecurityService securityService, ISecurityServiceConfiguration securityServiceConfiguration,
            IPreferenceManagerPlugin preferenceManagerPlugin, ITopMenuBarService topMenuBarService, WSClient wsClient) throws ExtensionManagerException {
        log.info("SERVICE>>> ExtensionManagerServiceImpl starting...");
        this.autoRefreshMode = configuration.getBoolean(Config.AUTO_REFRESH_ACTIVE.getConfigurationKey());
        this.autoRefreshFrequency = configuration.getInt(Config.AUTO_REFRESH_FREQUENCY.getConfigurationKey());
        String extensionDirectoryPath = configuration.getString(Config.DIRECTORY_PATH.getConfigurationKey());
        this.extensionDirectory = new File(extensionDirectoryPath);
        if (!this.extensionDirectory.exists() || !this.extensionDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid extension directory " + extensionDirectoryPath);
        }
        this.environment = environment;
        this.configuration = configuration;
        this.injector = injector;
        this.iI18nMessagesPlugin = iI18nMessagesPlugin;
        this.sysAdminUtils = sysAdminUtils;
        this.securityService = securityService;
        this.securityServiceConfiguration = securityServiceConfiguration;
        this.preferenceManagerPlugin = preferenceManagerPlugin;
        this.topMenuBarService = topMenuBarService;
        init();
        // Register to the custom router so that the extension web components
        // could be supported
        customRouterService.addListener(PATH_PREFIX, this);
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> ExtensionManagerServiceImpl stopping...");
            destroy();
            log.info("SERVICE>>> ExtensionManagerServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> ExtensionManagerServiceImpl started");
    }

    /**
     * Initialize the resources for this extension manager service.<br/>
     * By default all the extensions in the extension folder are loaded.
     */
    public void init() throws ExtensionManagerException {
        File[] extensionFiles = getExtensionDirectory().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jar");
            }
        });
        for (File extensionFile : extensionFiles) {
            if (addExtension(extensionFile.getAbsolutePath())) {
                log.info("Extension successfully loaded " + extensionFile);
            } else {
                log.error("Failure while loading the extension " + extensionFile);
            }
        }

        // Customize the menu if required
        customizeMenu();

        if (isAutoRefreshMode()) {
            startAutoRefresh();
        }
    }

    /**
     * Triggers a scheduler which scans the extensions to detect any change (and
     * thus reload the extensions which where changed).<br/>
     * WARNING: the autorefresh only scans for changes of extensions which are
     * already loaded.
     */
    private void startAutoRefresh() {
        this.autoRefreshScheduler = getSysAdminUtils().scheduleRecurring(true, "AutoRefreshExtensionScheduler", Duration.create(0, TimeUnit.MILLISECONDS),
                Duration.create(getAutoRefreshFrequency(), TimeUnit.SECONDS), new Runnable() {
                    @Override
                    public void run() {
                        for (IExtension extension : getExtensions()) {
                            File file = extension.getJarFile();
                            Date lastModified = new Date(file.lastModified());
                            if (extension.loadingTime().before(lastModified)) {
                                log.info("Extension " + extension.getDescriptor().getName() + " updated, reloading");
                                unload(extension);
                                load(file.getAbsolutePath());
                            }
                        }
                    }
                }, true);
    }

    /**
     * Free the resources for this extension manager service.<br/>
     * This will unload all the loaded extensions
     */
    public void destroy() {
        List<IExtension> extensionsCopy = new ArrayList<IExtension>(getExtensions());
        for (IExtension extension : extensionsCopy) {
            try {
                this.unload(extension);
            } catch (Exception e) {
                log.error("Error while unloading an extension", e);
            }
        }
        if (isAutoRefreshMode() && getAutoRefreshScheduler() != null) {
            getAutoRefreshScheduler().cancel();
        }
    }

    @Override
    public Promise<Result> notifyRequest(final Context ctx) {
        final String path = StringUtils.removeStart(ctx.request().path(), PATH_PREFIX);
        return getSecurityService().checkHasSubject(new Function0<Promise<Result>>() {
            @Override
            public Promise<Result> apply() throws Throwable {
                return execute(path, ctx);
            }
        });
    }

    @Override
    public Promise<Result> execute(String path, Context ctx) {
        for (WebCommand webCommand : webCommands) {
            if (webCommand.isCompatible(path, ctx)) {
                try {
                    return webCommand.call(path, ctx);
                } catch (Exception e) {
                    log.error("Error while calling the web command", e);
                    return Promise.promise(() -> Controller.badRequest());
                }
            }
        }
        log.info("No compatible command found for path " + path);
        if (log.isDebugEnabled()) {
            log.debug("No compatible command found for path " + path);
        }
        return Promise.promise(() -> Controller.badRequest());
    }

    @Override
    public String link(Object controllerInstance, String commandId, Object... parameters) throws ExtensionManagerException {
        if (!getExtensionControllers().containsKey(controllerInstance)) {
            throw new ExtensionManagerException("Unknown controller " + controllerInstance);
        }
        WebCommand webCommand = getExtensionControllers().get(controllerInstance).get(commandId);
        if (webCommand == null) {
            throw new ExtensionManagerException("Unknown command " + commandId + " in the controller " + controllerInstance);
        }
        String pathContext = getConfiguration().getString("play.http.context");
        if (pathContext == null || pathContext.equals("/")) {
            pathContext = "";
        }
        String publicUrl = getPreferenceManagerPlugin().getPreferenceElseConfigurationValue(IFrameworkConstants.PUBLIC_URL_PREFERENCE, "maf.public.url");
        return publicUrl + pathContext + PATH_PREFIX + webCommand.generateLink(parameters);
    }

    @Override
    public synchronized List<IExtension> getLoadedExtensions() {
        return Collections.unmodifiableList(extensions);
    }

    @Override
    public synchronized void unload(IExtension extension) {
        if (getExtensions().contains(extension)) {
            Extension extensionObject = (Extension) extension;

            // Remove the standalone controllers
            for (Object extensionController : extensionObject.getStandaloneControllersInstances()) {
                removeExtensionController(extensionController);
                log.info("Unloading controller " + extensionObject.getClass());
            }

            // Remove the plugin controllers
            for (PluginResources pluginResources : extensionObject.getPluginResources().values()) {
                if (pluginResources.getCustomConfigurationController() != null) {
                    removeExtensionController(pluginResources.getCustomConfigurationController());
                }
                if (pluginResources.getRegistrationConfigurationControllers() != null) {
                    for (Object controllerInstance : pluginResources.getRegistrationConfigurationControllers().values()) {
                        removeExtensionController(controllerInstance);
                    }
                }
            }

            // Unloading the i18 resources
            Ebean.beginTransaction(TxIsolation.SERIALIZABLE);
            try {
                if (extensionObject.getDescriptorInternal().getXmlExtensionDescriptor().getI18nMessages() != null) {
                    for (I18nMessage i18nMessage : extensionObject.getDescriptorInternal().getXmlExtensionDescriptor().getI18nMessages()) {
                        if (getiI18nMessagesPlugin().isLanguageValid(i18nMessage.getLanguage())) {
                            Properties properties = new Properties();
                            try {
                                properties.load(new StringReader(i18nMessage.getMessages()));
                                for (String key : properties.stringPropertyNames()) {
                                    getiI18nMessagesPlugin().delete(key, i18nMessage.getLanguage());
                                }
                                log.info("Unloaded i18n keys [" + i18nMessage.getLanguage() + "] for the extension" + extension.getDescriptor().getName());
                            } catch (IOException e) {
                                log.error("Unable to unload the i18n keys [" + i18nMessage.getLanguage() + "] for the extension"
                                        + extension.getDescriptor().getName(), e);
                            }
                        }
                    }
                }
                Ebean.commitTransaction();
            } catch (Exception e) {
                log.error("Error while unloading the i18n keys of the extensions");
                Ebean.rollbackTransaction();
            } finally {
                Ebean.endTransaction();
            }

            getExtensions().remove(extension);
            log.info("Extension " + extension.getDescriptor().getName() + " unloaded");
        }
    }

    @Override
    public synchronized boolean load(String jarFilePath) {
        return addExtension(jarFilePath);
    }

    @Override
    public Triple<IPluginRunner, Object, Map<DataType, Object>> loadAndInitPluginInstance(String pluginIdentifier, Long pluginConfigurationId,
            IPluginContext pluginContext) throws ExtensionManagerException {
        try {
            // Find the plugin runner instanciate
            Pair<Extension, IPluginDescriptor> result = findPluginExtensionFromIdentifier(pluginIdentifier);
            if (result == null) {
                throw new ExtensionManagerException("No plugin implementation found for the plugin identifier " + pluginIdentifier);
            }

            Pair<IPluginRunner, PluginResources> plugin = result.getLeft().loadPluginInstance(pluginIdentifier, pluginConfigurationId, pluginContext,
                    result.getRight());
            log.info("Plugin instance [" + pluginConfigurationId + "] for the unique id [" + pluginIdentifier + "] in the extension "
                    + result.getLeft().getDescriptor().getName() + " : PluginRunner instanciated");

            // Look for the configuration standaloneControllers and register
            // them to the
            // controller manager
            if (plugin.getRight().getCustomConfigurationController() != null) {
                try {
                    log.info("Found a custom configuration controller, registering as web command...");
                    addExtensionController(plugin.getRight().getCustomConfigurationController(), "/" + pluginConfigurationId + "/custom",
                            IFrameworkConstants.ADMIN_PLUGIN_MANAGER_PERMISSION);
                    log.info("Custom configuration controller registered !");
                } catch (ExtensionManagerException e) {
                    log.warn("Error while loading the configuration controller" + result.getRight().getCustomConfiguratorControllerClassName(), e);
                }
            } else {
                log.info("No custom configuration controller !");
            }
            if (plugin.getRight().getRegistrationConfigurationControllers() != null) {
                for (DataType dataType : plugin.getRight().getRegistrationConfigurationControllers().keySet()) {
                    try {
                        Object registrationController = plugin.getRight().getRegistrationConfigurationControllers().get(dataType);
                        log.info("Found a registration configuration controller for DataType " + dataType + " ...");
                        addExtensionController(registrationController, "/" + pluginConfigurationId + "/register" + dataType.getDataName().toLowerCase(),
                                IFrameworkConstants.ADMIN_PLUGIN_MANAGER_PERMISSION);
                        log.info("Custom registration configuration controller loaded !");
                    } catch (ExtensionManagerException e) {
                        log.warn("Error while loading the registration configuration controller"
                                + result.getRight().getRegistrationConfiguratorControllerClassNames().get(dataType), e);
                    }
                }
            } else {
                log.info("No registration configuration controller !");
            }
            log.info("Plugin instance [" + pluginConfigurationId + "] for the unique id [" + pluginIdentifier + "] in the extension "
                    + result.getLeft().getDescriptor().getName() + " loaded");
            return Triple.of(plugin.getLeft(), plugin.getRight().getCustomConfigurationController(),
                    plugin.getRight().getRegistrationConfigurationControllers());
        } catch (Exception e) {
            throw new ExtensionManagerException("Unable to create an instance for the specified plugin " + pluginIdentifier, e);
        }
    }

    @Override
    public void unloadPluginInstance(IPluginRunner pluginRunner, String pluginIdentifier, Long pluginConfigurationId) throws ExtensionManagerException {
        Pair<Extension, IPluginDescriptor> result = findPluginExtensionFromIdentifier(pluginIdentifier);
        if (result == null) {
            throw new ExtensionManagerException(
                    "No plugin implementation found for the plugin identifier " + pluginIdentifier + " and instance " + pluginConfigurationId);
        }

        // Look for the configuration standaloneControllers and unregister them
        // to
        // the
        // extension manager
        PluginResources pluginResources = result.getLeft().unloadPluginInstance(pluginIdentifier, pluginConfigurationId);
        if (pluginResources.getCustomConfigurationController() != null) {
            removeExtensionController(pluginResources.getCustomConfigurationController());
            log.info("Un-registering a custom configuration controller !");
        }
        if (pluginResources.getRegistrationConfigurationControllers() != null) {
            for (DataType dataType : pluginResources.getRegistrationConfigurationControllers().keySet()) {
                Object registrationController = pluginResources.getRegistrationConfigurationControllers().get(dataType);
                removeExtensionController(registrationController);
                log.info("Un-registering a registration configuration controller for data type" + dataType);
            }
        }

        log.info("Plugin instance [" + pluginConfigurationId + "] for the unique id [" + pluginIdentifier + "] in the extension "
                + result.getLeft().getDescriptor().getName() + " unlloaded");
    }

    /**
     * Find the {@link Extension} and the {@link IPluginDescriptor} associated
     * with the unique plugin identifier
     * 
     * @param pluginIdentifier
     *            a unique plugin identifier
     * @return a pair
     */
    private Pair<Extension, IPluginDescriptor> findPluginExtensionFromIdentifier(String pluginIdentifier) {
        for (Extension extension : getExtensions()) {
            if (extension.getDescriptor().getDeclaredPlugins().containsKey(pluginIdentifier)) {
                return Pair.of(extension, extension.getDescriptor().getDeclaredPlugins().get(pluginIdentifier));
            }
        }
        return null;
    }

    @Override
    public synchronized boolean customizeMenu() {
        boolean menuReseted = false;
        for (Extension extension : getExtensions()) {
            if (extension.getDescriptor().isMenuCustomized()) {
                if (!menuReseted) {
                    menuReseted = true;
                    log.info("Reseting the toolbar before adding menu customizations");
                    getTopMenuBarService().resetTopMenuBar();
                }
                log.info("Loading menu customization for extension " + extension.getDescriptor().getName());
                return updateTopMenu(extension.getDescriptorInternal().getXmlExtensionDescriptor().getMenuCustomizationDescriptor());
            }
        }
        return false;
    }

    @Override
    public Long getSize() {
        return Utilities.folderSize(this.getExtensionDirectory());
    }

    private boolean updateTopMenu(MenuCustomizationDescriptor menuCustomizationDescriptor) {
        boolean result = true;
        // Remove some menu items
        Menu mainPerspective = getTopMenuBarService().getMainPerspective();
        if (menuCustomizationDescriptor.getMenusToRemove() != null && menuCustomizationDescriptor.getMenusToRemove().size() != 0) {
            mainPerspective
                    .removeMenuItem(menuCustomizationDescriptor.getMenusToRemove().toArray(new String[menuCustomizationDescriptor.getMenusToRemove().size()]));
            log.info("Remove the menus " + menuCustomizationDescriptor.getMenusToRemove());
        }
        // Add some new menu items
        if (menuCustomizationDescriptor.getMenusToAdd() != null && menuCustomizationDescriptor.getMenusToAdd().size() != 0) {
            for (MenuItemDescriptor menuItemDescriptor : menuCustomizationDescriptor.getMenusToAdd()) {
                MenuItem menuItem = null;
                if (menuItemDescriptor.getUrl() != null) {
                    menuItem = new ClickableMenuItem(menuItemDescriptor.getUuid(), menuItemDescriptor.getLabel(), menuItemDescriptor.getUrl());
                } else {
                    menuItem = new HeaderMenuItem(menuItemDescriptor.getUuid(), menuItemDescriptor.getLabel());
                }
                if (menuItemDescriptor.getPermissions() != null) {
                    menuItem.setAuthorizedPermissions(
                            Utilities.getListOfArray(menuItemDescriptor.getPermissions().toArray(new String[menuItemDescriptor.getPermissions().size()])));
                }
                if (menuItemDescriptor.getAddAfterUuid() != null) {
                    log.info("Adding menu " + menuItemDescriptor.getUuid() + " after " + menuItemDescriptor.getAddAfterUuid());
                    result = result && mainPerspective.addMenuItemAfter(menuItemDescriptor.getAddAfterUuid(), menuItem);
                    log.info("Menu edition status " + result);
                } else {
                    if (menuItemDescriptor.getAddBeforeUuid() != null) {
                        log.info("Adding menu " + menuItemDescriptor.getUuid() + " before " + menuItemDescriptor.getAddBeforeUuid());
                        result = result && mainPerspective.addMenuItemBefore(menuItemDescriptor.getAddBeforeUuid(), menuItem);
                        log.info("Menu edition status " + result);
                    } else {
                        if (menuItemDescriptor.getToUuid() != null) {
                            log.info("Adding menu " + menuItemDescriptor.getUuid() + " to " + menuItemDescriptor.getToUuid());
                            result = result && mainPerspective.addSubMenuItemTo(menuItemDescriptor.getToUuid(), menuItem);
                            log.info("Menu edition status " + result);
                        } else {
                            log.info("Adding menu " + menuItemDescriptor.getUuid() + " as top menu");
                            // Neither after nor before (top level menu)
                            getTopMenuBarService().addMenuItemToMainPerspective(menuItem);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Add an extension using the specified JAR
     * 
     * @param jarFilePath
     *            a JAR file path
     * @return a boolean true if the extension was loaded
     */
    private boolean addExtension(String jarFilePath) {
        File jarFile = new File(jarFilePath);
        if (!jarFile.exists() || !jarFile.isFile()) {
            log.error("Unknown JAR file " + jarFilePath);
            return false;
        }
        Extension extension = null;
        try {
            extension = new Extension(jarFile, getEnvironment(), getInjector(), this);
        } catch (ExtensionManagerException e) {
            log.error("Unable to load the extension " + jarFile, e);
            return false;
        }

        // Prepare the standaloneControllers to listen to incoming requests
        List<Object> successFullyLoadedControllers = new ArrayList<Object>();
        for (Object controllerInstance : extension.getStandaloneControllersInstances()) {
            try {
                addExtensionController(controllerInstance, null);
                successFullyLoadedControllers.add(controllerInstance.getClass());
            } catch (Exception e) {
                log.error("Error with controller " + controllerInstance, e);
                // Remove all the loaded standaloneControllers and their web
                // command
                for (Object successFullyLoadedController : successFullyLoadedControllers) {
                    for (WebCommand webCommand : getExtensionControllers().get(successFullyLoadedController).values()) {
                        getWebCommands().remove(webCommand);
                    }
                    getExtensionControllers().remove(successFullyLoadedController);
                }
                return false;
            }
        }

        // Load the i18n keys into
        Ebean.beginTransaction(TxIsolation.SERIALIZABLE);
        try {
            if (extension.getDescriptorInternal().getXmlExtensionDescriptor().getI18nMessages() != null) {
                for (I18nMessage i18nMessage : extension.getDescriptorInternal().getXmlExtensionDescriptor().getI18nMessages()) {
                    if (getiI18nMessagesPlugin().isLanguageValid(i18nMessage.getLanguage())) {
                        Properties properties = new Properties();
                        try {
                            properties.load(new StringReader(i18nMessage.getMessages()));
                            for (String key : properties.stringPropertyNames()) {
                                getiI18nMessagesPlugin().add(key, properties.getProperty(key), i18nMessage.getLanguage());
                            }
                            log.info("Loaded i18n keys [" + i18nMessage.getLanguage() + "] for the extension" + extension.getDescriptor().getName());
                        } catch (IOException e) {
                            log.error(
                                    "Unable to load the i18n keys [" + i18nMessage.getLanguage() + "] for the extension" + extension.getDescriptor().getName(),
                                    e);
                        }
                    }
                }
            }
            Ebean.commitTransaction();
        } catch (Exception e) {
            log.error("Error while adding the i18n keys of the extensions");
            Ebean.rollbackTransaction();
        } finally {
            Ebean.endTransaction();
        }

        getExtensions().add(extension);
        log.info("Extension " + extension.getDescriptor().getName() + " loaded");
        return true;
    }

    /**
     * Add an extension controller to the list of web commands
     * 
     * @param controllerInstance
     *            a controller instance (which class must be annotated by
     *            {@link WebCommandPath} as well as a method)
     * @param pathPrefix
     *            add a path prefix to the web commands of the specified
     *            controller (this is used for instance, for the plugins which
     *            may declare a controller possibly existing for multiple
     *            instances)
     * @param defaultPermissions
     *            the default permissions to be associated with the
     *            standaloneControllers
     * @throws ExtensionManagerException
     */
    private void addExtensionController(Object controllerInstance, String pathPrefix, String... defaultPermissions) throws ExtensionManagerException {
        Class<?> controllerClass = controllerInstance.getClass();
        if (controllerClass.isAnnotationPresent(WebControllerPath.class) && AbstractExtensionController.class.isAssignableFrom(controllerClass)) {
            WebControllerPath controllerAnnotation = controllerClass.getAnnotation(WebControllerPath.class);
            if (!StringUtils.isBlank(controllerAnnotation.path())) {
                try {
                    // Parse the controller to discover its interface
                    Method[] methods = controllerClass.getMethods();
                    if (methods != null) {
                        for (Method method : methods) {
                            if (method.isAnnotationPresent(WebCommandPath.class)
                                    && (method.getReturnType().equals(Result.class) || method.getReturnType().equals(Promise.class))) {
                                WebCommandPath methodAnnotation = method.getAnnotation(WebCommandPath.class);
                                if (!StringUtils.isBlank(methodAnnotation.path())) {
                                    HashSet<String> permissions = new HashSet<String>();
                                    permissions.addAll(Arrays.asList(methodAnnotation.permissions()));
                                    permissions.addAll(Arrays.asList(controllerAnnotation.permissions()));
                                    if (defaultPermissions.length != 0) {
                                        permissions.addAll(Arrays.asList(defaultPermissions));
                                    }
                                    String commandId = methodAnnotation.id();
                                    if (StringUtils.isBlank(commandId)) {
                                        commandId = UUID.randomUUID().toString();
                                    }
                                    String pathPrefixAsString = pathPrefix != null ? pathPrefix : "";
                                    WebCommand webCommand = new WebCommand(controllerInstance, commandId,
                                            pathPrefixAsString + controllerAnnotation.path() + methodAnnotation.path(),
                                            permissions.toArray(new String[permissions.size()]), methodAnnotation.httpMethod(), method, getSecurityService(),
                                            getSecurityServiceConfiguration());
                                    addWebCommands(controllerInstance, webCommand);
                                    log.info("Registered web command " + webCommand);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new ExtensionManagerException("Unable to configure the extension controller" + controllerClass.getName(), e);
                }
            }
        } else {
            throw new ExtensionManagerException(
                    "Invalid extension controller, no WebCommandPath or do not extends AbstractExtensionController" + controllerClass.getName());
        }
    }

    /**
     * Remove the specified extension controller instance
     * 
     * @param controllerInstance
     *            a controller instance (which class must be annotated by
     *            {@link WebCommandPath} as well as a method)
     */
    private void removeExtensionController(Object controllerInstance) {
        if (controllerInstance == null || !getExtensionControllers().containsKey(controllerInstance)) {
            log.warn("Attempt to remove a non existing controller instance " + controllerInstance);
            return;
        }
        for (WebCommand webCommand : getExtensionControllers().get(controllerInstance).values()) {
            log.info("Unloading web command " + webCommand);
            getWebCommands().remove(webCommand);
        }
        getExtensionControllers().remove(controllerInstance);
    }

    /**
     * Add a WebCommand to the service
     * 
     * @param controllerInstance
     *            a controller
     * @param webCommand
     *            a command
     */
    private void addWebCommands(Object controllerInstance, WebCommand webCommand) {
        Map<String, WebCommand> controllerCommands = null;
        if (getExtensionControllers().containsKey(controllerInstance)) {
            controllerCommands = getExtensionControllers().get(controllerInstance);
        } else {
            controllerCommands = Collections.synchronizedMap(new HashMap<String, WebCommand>());
        }
        controllerCommands.put(webCommand.getId(), webCommand);
        getExtensionControllers().put(controllerInstance, controllerCommands);
        getWebCommands().add(webCommand);
    }

    private List<WebCommand> getWebCommands() {
        return webCommands;
    }

    private Map<Object, Map<String, WebCommand>> getExtensionControllers() {
        return extensionControllers;
    }

    private List<Extension> getExtensions() {
        return extensions;
    }

    private boolean isAutoRefreshMode() {
        return autoRefreshMode;
    }

    private Cancellable getAutoRefreshScheduler() {
        return autoRefreshScheduler;
    }

    private int getAutoRefreshFrequency() {
        return autoRefreshFrequency;
    }

    private File getExtensionDirectory() {
        return extensionDirectory;
    }

    private II18nMessagesPlugin getiI18nMessagesPlugin() {
        return iI18nMessagesPlugin;
    }

    private ISysAdminUtils getSysAdminUtils() {
        return sysAdminUtils;
    }

    private Environment getEnvironment() {
        return environment;
    }

    private Configuration getConfiguration() {
        return configuration;
    }

    private Injector getInjector() {
        return injector;
    }

    private ISecurityService getSecurityService() {
        return securityService;
    }

    private ISecurityServiceConfiguration getSecurityServiceConfiguration() {
        return securityServiceConfiguration;
    }

    private IPreferenceManagerPlugin getPreferenceManagerPlugin() {
        return preferenceManagerPlugin;
    }

    private ITopMenuBarService getTopMenuBarService() {
        return topMenuBarService;
    }

    /**
     * The class which encapsulate an extension.<br/>
     * This class deals with all the object creations using a custom class
     * loader.<br/>
     * It references the created controller classes or plugin resources (
     * {@link PluginResources}) in order to "unload" them when the extension is
     * not used anymore.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class Extension implements IExtension {
        private static final List<Class<?>> AUTHORIZED_INJECTED_SERVICE = Arrays.asList(ISecurityService.class, IUserSessionManagerPlugin.class,
                ILinkGenerationService.class, II18nMessagesPlugin.class, ISysAdminUtils.class, IPluginContext.class, IPluginRunner.class, WSClient.class);

        private Date loadingTime;
        private File jarFile;
        private JarClassLoader jarClassLoader;
        private ReadOnlyExtensionDescriptor descriptor;
        private Injector injector;
        private ILinkGenerationService linkGenerationService;
        private List<Object> standaloneControllers;
        private Map<String, PluginResources> pluginResources;

        /**
         * Creates an extension using the specified JAR file
         * 
         * @param jarFile
         *            the jar file which contains the extension
         * @param environment
         *            the play environment required to get the play class loader
         * @param injector
         *            the application injector
         * @param linkGenerationService
         *            the service in charge of genering links for controllers
         */
        public Extension(File jarFile, Environment environment, Injector injector, ILinkGenerationService linkGenerationService)
                throws ExtensionManagerException {
            super();
            this.injector = injector;
            this.linkGenerationService = linkGenerationService;
            this.loadingTime = new Date();
            this.jarFile = jarFile;
            this.pluginResources = new HashMap<>();
            init(jarFile, environment);
        }

        @Override
        public synchronized InputStream getResourceAsStream(String name) {
            try {
                InputStream inStream = getJarClassLoader().getResourceAsStream(name);
                if (inStream == null) {
                    log.warn("Resource " + name + " not founf in extension " + getDescriptor().getName());
                }
                return inStream;
            } catch (Exception e) {
                log.error("Unable to get the specified resource " + name + " from extension " + getDescriptor().getName(), e);
            }
            return null;
        }

        @Override
        public synchronized Date loadingTime() {
            return loadingTime;
        }

        /**
         * Creates a plugin instance for the specified plugin.<br/>
         * This creates the required classes:
         * <ul>
         * <li>The plugin runner</li>
         * <li>The configuration controllers (if any)</li>
         * </ul>
         * The controllers are retained in the extension for a possible cleanup
         * at a later stage.
         * 
         * @param pluginIdentifier
         *            a unique plugin identifier
         * @param pluginConfigurationId
         *            a unique plugin instance identifier
         * @return a resources holder
         * @throws ClassNotFoundException
         */
        public synchronized Pair<IPluginRunner, PluginResources> loadPluginInstance(String pluginIdentifier, Long pluginConfigurationId,
                IPluginContext pluginContext, IPluginDescriptor pluginDescriptor) throws ClassNotFoundException {
            JclObjectFactory factory = JclObjectFactory.getInstance();
            String pluginRunnerClassName = pluginDescriptor.getClazz();

            Map<Class<?>, Object> injectableValues = new HashMap<>();
            injectableValues.put(ILinkGenerationService.class, getLinkGenerationService());
            injectableValues.put(IPluginContext.class, pluginContext);

            // Creates the plugin runner
            IPluginRunner pluginRunner = IPluginRunner.class.cast(createInstanceOfClass(pluginRunnerClassName, factory, injectableValues));

            // Add the plugin runner to the injectable values for the
            // configuration controllers
            injectableValues.put(IPluginRunner.class, pluginRunner);

            // Creates the custom configuration controller (if any)
            Object customConfiguratorController = null;
            if (pluginDescriptor.getCustomConfiguratorControllerClassName() != null) {
                customConfiguratorController = createInstanceOfClass(pluginDescriptor.getCustomConfiguratorControllerClassName(), factory, injectableValues);
            }

            // Creates the registration configuration controllers (if any)
            Map<DataType, Object> registrationConfiguratorControllers = null;
            if (pluginDescriptor.getRegistrationConfiguratorControllerClassNames().size() != 0) {
                registrationConfiguratorControllers = Collections.synchronizedMap(new HashMap<>());
                for (DataType dataType : pluginDescriptor.getRegistrationConfiguratorControllerClassNames().keySet()) {
                    String controllerClassName = pluginDescriptor.getRegistrationConfiguratorControllerClassNames().get(dataType);
                    log.info("Loading controller " + controllerClassName);
                    registrationConfiguratorControllers.put(dataType, createInstanceOfClass(controllerClassName, factory, injectableValues));
                }
            }
            PluginResources pluginResources = new PluginResources(pluginIdentifier, pluginConfigurationId, customConfiguratorController,
                    registrationConfiguratorControllers);
            getPluginResources().put(pluginResources.getUniquePluginResourceKey(), pluginResources);
            return Pair.of(pluginRunner, pluginResources);
        }

        /**
         * Remove the plugin instance resources from the extension
         * 
         * @param pluginIdentifier
         * @param pluginConfigurationId
         * @return
         */
        public synchronized PluginResources unloadPluginInstance(String pluginIdentifier, Long pluginConfigurationId) {
            return pluginResources.remove(PluginResources.createUniqueResourceKey(pluginIdentifier, pluginConfigurationId));
        }

        @Override
        public synchronized IExtensionDescriptor getDescriptor() {
            return descriptor;
        }

        /**
         * The jar file which contains this extension
         */
        public synchronized File getJarFile() {
            return jarFile;
        }

        /**
         * A list of standalone controller instances loaded by this extension at
         * startup
         * 
         * @return
         */
        public synchronized List<Object> getStandaloneControllersInstances() {
            return standaloneControllers;
        }

        /**
         * A map of plugin resources.<br/>
         * The key of the map is a unique {@link PluginResources} id (see the
         * class implementation)
         * 
         * @return
         */
        public synchronized Map<String, PluginResources> getPluginResources() {
            return pluginResources;
        }

        /**
         * Read the content of the specified JAR file
         * 
         * @param jarFile
         *            an extension JAR file
         * @param environment
         *            the play environment used to get the play classloader
         * @throws ExtensionManagerException
         */
        private synchronized void init(File jarFile, Environment environment) throws ExtensionManagerException {
            try {
                // Reading the descriptor
                log.info("Loading extension " + jarFile);
                this.jarClassLoader = new JarClassLoader();
                PlayProxyClassLoader proxyClassLoader = new PlayProxyClassLoader(environment.classLoader());
                proxyClassLoader.setOrder(6);// After the other default class
                                             // loaders
                this.jarClassLoader.addLoader(proxyClassLoader);
                this.jarClassLoader.add(jarFile.getAbsolutePath());
                InputStream inStream = jarClassLoader.getResourceAsStream(EXTENSION_MANAGER_DESCRIPTOR_FILE);
                if (inStream == null) {
                    throw new ExtensionManagerException("No descriptor file in the JAR");
                }
                JAXBContext jc = JAXBContext.newInstance(XmlExtensionDescriptor.class);
                Unmarshaller u = jc.createUnmarshaller();
                this.descriptor = new ReadOnlyExtensionDescriptor((XmlExtensionDescriptor) u.unmarshal(inStream));
                log.info("Found descriptor for extension " + descriptor.getName());

                // Loading the standalone web standaloneControllers
                this.standaloneControllers = Collections.synchronizedList(new ArrayList<Object>());
                JclObjectFactory factory = JclObjectFactory.getInstance();
                for (String controllerClassName : getDescriptor().getDeclaredStandaloneControllers()) {
                    log.info("Loading controller " + controllerClassName);
                    Map<Class<?>, Object> injectableValues = new HashMap<>();
                    injectableValues.put(ILinkGenerationService.class, getLinkGenerationService());
                    this.standaloneControllers.add(createInstanceOfClass(controllerClassName, factory, injectableValues));
                }
            } catch (Exception e) {
                throw new ExtensionManagerException("Unable to read the JAR extension : " + jarFile, e);
            }
        }

        /**
         * Create dynamically an instance of the specified object class
         * 
         * @param objectClass
         *            an object class name
         * @param factory
         *            a JCL factory
         * @param injectableValues
         *            some values to be dynamically injected into the created
         *            classes
         * @return an instance of object
         * @throws ClassNotFoundException
         */
        private Object createInstanceOfClass(String objectClassName, JclObjectFactory factory, Map<Class<?>, Object> injectableValues)
                throws ClassNotFoundException {
            if (log.isDebugEnabled()) {
                log.debug("Attempting to create a class " + objectClassName);
            }
            Class<?> pluginRunnerClass = getJarClassLoader().loadClass(objectClassName);
            // Look for the constructor injection tag
            Pair<Object[], Class<?>[]> injectableParameters = getInjectableConstructorParameters(pluginRunnerClass, injectableValues);
            if (injectableParameters != null) {
                return factory.create(getJarClassLoader(), objectClassName, injectableParameters.getLeft(), injectableParameters.getRight());
            }
            // Attempt to create using a default constructor
            return factory.create(getJarClassLoader(), objectClassName);
        }

        /**
         * Return the parameters to be injected for creating an instance of the
         * specified class
         * 
         * @param clazz
         *            a class
         * @param injectableValues
         *            some values to be dynamically injected (if the class
         *            request it)
         * @return an array of objects
         */
        private Pair<Object[], Class<?>[]> getInjectableConstructorParameters(Class<?> clazz, Map<Class<?>, Object> injectableValues) {
            Constructor<?> injectableConstructor = null;
            Constructor<?>[] constructors = clazz.getConstructors();

            // Look for injectable constructor
            boolean hasDefaultConstructor = false;
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterTypes().length == 0) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found a default constructor");
                    }
                    hasDefaultConstructor = true;
                }
                if (constructor.isAnnotationPresent(Inject.class)) {
                    if (injectableConstructor == null) {
                        injectableConstructor = constructor;
                    } else {
                        throw new IllegalArgumentException("Multiple injectable constructor defined, please correct : only one injectable constructor allowed");
                    }
                }
            }
            if (injectableConstructor == null) {
                if (log.isDebugEnabled()) {
                    log.debug("No injectable constructor");
                }
                if (!hasDefaultConstructor) {
                    throw new IllegalArgumentException(
                            "There is no injectable nor default constructor for the class " + clazz.getName() + " cannot instanciate");
                }
                return null;// No Inject
            }

            if (log.isDebugEnabled()) {
                log.debug("Found a constructor for the class " + clazz.getName() + " : " + injectableConstructor);
            }

            // Check if the Injected services are authorized
            Class<?>[] parameterClasses = injectableConstructor.getParameterTypes();
            if (parameterClasses != null && parameterClasses.length != 0) {
                Object[] parameters = new Object[parameterClasses.length];
                if (log.isDebugEnabled()) {
                    log.debug("Constructor expects " + parameterClasses.length + " arguments : " + ArrayUtils.toString(parameterClasses));
                }
                int count = 0;
                for (Class<?> parameterClass : parameterClasses) {
                    if (!AUTHORIZED_INJECTED_SERVICE.contains(parameterClass)) {
                        throw new IllegalArgumentException(parameterClass.getName() + " cannot be injected in the extension constructor");
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Looking for an instance of injector");
                        }
                        if (injectableValues != null && injectableValues.containsKey(parameterClass)) {
                            parameters[count] = injectableValues.get(parameterClass);
                        } else {
                            parameters[count] = getInjector().instanceOf(parameterClass);
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Class " + parameterClass + " injected !");
                        }
                        if (parameters[count] == null) {
                            throw new IllegalArgumentException("Cannot inject " + parameterClass.getName() + " unknown reason");
                        }
                        count++;
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Created an array of parameters : " + ArrayUtils.toString(parameters));
                }
                return Pair.of(parameters, parameterClasses);
            }

            return null;
        }

        private ReadOnlyExtensionDescriptor getDescriptorInternal() {
            return descriptor;
        }

        private JarClassLoader getJarClassLoader() {
            return jarClassLoader;
        }

        private Injector getInjector() {
            return injector;
        }

        private ILinkGenerationService getLinkGenerationService() {
            return linkGenerationService;
        }

        /**
         * The resources associated with a plugin.<br/>
         * The extension is holder some references to these resources so that
         * their life cycle is correctly managed.
         * 
         * @author Pierre-Yves Cloux
         */
        public static class PluginResources {
            private String uniquePluginResourceKey;
            private Object customConfigurationController;
            private Map<DataType, Object> registrationConfigurationControllers;

            public PluginResources(String pluginIdentifier, Long pluginConfigurationId, Object customConfigurationController,
                    Map<DataType, Object> registrationConfigurationControllers) {
                super();
                this.uniquePluginResourceKey = createUniqueResourceKey(pluginIdentifier, pluginConfigurationId);
                this.customConfigurationController = customConfigurationController;
                this.registrationConfigurationControllers = registrationConfigurationControllers;
            }

            public Object getCustomConfigurationController() {
                return customConfigurationController;
            }

            public Map<DataType, Object> getRegistrationConfigurationControllers() {
                return registrationConfigurationControllers;
            }

            public String getUniquePluginResourceKey() {
                return uniquePluginResourceKey;
            }

            /**
             * A unique key for the plugin resources.<br/>
             * This is a concatenation of:
             * <ul>
             * <li>the unique plugin identifier</li>
             * <li>the plugin configuration identifier (specific from a plugin
             * instance)</li>
             * </ul>
             * 
             * @return
             */
            public static String createUniqueResourceKey(String pluginIdentifier, Long pluginConfigurationId) {
                return pluginIdentifier + "#" + pluginConfigurationId;
            }
        }
    }

    /**
     * A command handler.<br/>
     * This one is able to "test the compatibility" with a path and to execute
     * the command.<br/>
     * <ul>
     * <li>commandPattern : the pattern to validate the compatibility of a path
     * with a command</li>
     * <li>permissions : an array of permissions to be checked before accessing
     * a method</li>
     * <li>httpMethod : the HTTP method compatible with the command</li>
     * <li>command : the method to be executed when the command is called</li>
     * <li>parametersMapping : the mapping between the path components index and
     * the method parameters index</li>
     * </ul>
     * 
     * @author Pierre-Yves Cloux
     * 
     */
    public static class WebCommand {
        public static final String PARAMETER_REGEXPR = "(.+)";
        private String id;
        private Object controllerInstance;
        private Pattern commandPattern;
        private String[] permissions;
        private WebCommandPath.HttpMethod httpMethod;
        private Method command;
        private ParameterMeta[] parametersMapping;
        private ISecurityService securityService;
        private ISecurityServiceConfiguration securityServiceConfiguration;

        public WebCommand(Object controllerInstance, String id, String webPath, String[] permissions, WebCommandPath.HttpMethod httpMethod, Method command,
                ISecurityService securityService, ISecurityServiceConfiguration securityServiceConfiguration) throws ExtensionManagerException {
            super();
            this.id = id;
            this.controllerInstance = controllerInstance;
            findParametersMapping(webPath, command);
            this.httpMethod = httpMethod;
            this.permissions = permissions;
            this.command = command;
            this.securityService = securityService;
            this.securityServiceConfiguration = securityServiceConfiguration;
        }

        /**
         * Extract the annotated parameters for the method (see
         * {@link WebParameter})
         * 
         * @param webPath
         *            the web path for the command
         * @param command
         *            the command itself (a {@link Method} of the extension
         *            controller)
         * @throws ExtensionManagerException
         */
        private void findParametersMapping(String webPath, Method command) throws ExtensionManagerException {
            Map<String, ParameterMeta> parametersIndex = Collections.synchronizedMap(new HashMap<String, ParameterMeta>());
            Annotation[][] annotations = command.getParameterAnnotations();
            if (annotations != null) {
                int count = 0;
                for (Annotation[] ann : annotations) {
                    if (ann.length != 0 && ann[0] instanceof WebParameter) {
                        WebParameter webParameter = (WebParameter) ann[0];
                        Class<?> clazz = command.getParameterTypes()[count];
                        parametersIndex.put(webParameter.name(), new ParameterMeta(webParameter.name(), count, clazz));
                        count++;
                    }
                }
            }
            ParameterMeta[] mapping = new ParameterMeta[parametersIndex.size()];
            String pathPattern = webPath;
            Pattern pattern = Pattern.compile("\\B(:\\w+)\\b");
            Matcher matcher = pattern.matcher(webPath);
            int count = 0;
            while (matcher.find()) {
                String parameterName = matcher.group();
                parameterName = parameterName.replace(":", "");
                ParameterMeta parameterMeta = parametersIndex.get(parameterName);
                if (parameterMeta == null) {
                    throw new ExtensionManagerException("No match between method annotation and path parameters : unknown parameter " + parameterName);
                }
                mapping[count++] = parameterMeta;
                pathPattern = pathPattern.replace(":" + parameterName, PARAMETER_REGEXPR);
            }
            if (mapping.length != parametersIndex.size()) {
                throw new ExtensionManagerException("No match between method annotation and path parameters");
            }
            this.commandPattern = Pattern.compile(pathPattern);
            this.parametersMapping = mapping;
        }

        /**
         * Return true if the specified request can be handled by this command:
         * <ul>
         * <li>The path must be compatible</li>
         * <li>The HTTP method must be compatible</li>
         * </ul>
         * 
         * @param path
         *            an extension command path
         * @param ctx
         *            a context instance
         * @return a boolean
         */
        public boolean isCompatible(String path, Context ctx) {
            if (!getHttpMethod().isSupported(ctx.request().method())) {
                return false;
            }
            Matcher matcher = getCommandPattern().matcher(path);
            return matcher.matches();
        }

        /**
         * Execute the web command and returns a {@link Result}
         * 
         * @param path
         *            the extension command path
         * @param ctx
         *            a context instance
         * @return a Result instance
         * @throws ExtensionManagerException
         */
        @SuppressWarnings("unchecked")
        public Promise<Result> call(String path, Context ctx) throws ExtensionManagerException {
            try {
                // Check the permissions
                ArrayList<String[]> perms = new ArrayList<String[]>();
                perms.add(getPermissions());
                if (!getSecurityService().restrict(perms)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Call to path " + path + " but permissions are not sufficient");
                    }
                    return Promise.promise(() -> getSecurityServiceConfiguration().displayAccessForbidden());
                }

                // Execute
                Object[] args = new Object[getParametersMapping().length];
                Matcher matcher = getCommandPattern().matcher(path);
                int count = 0;
                if (matcher.matches()) {
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        String parameterValue = matcher.group(i);
                        ParameterMeta parameterMeta = getParametersMapping()[count++];
                        try {
                            args[parameterMeta.getRealIndex()] = ConvertUtils.convert(parameterValue, parameterMeta.getParameterType());
                        } catch (Exception e) {
                            log.error("Invalid type for the parameter " + parameterMeta.getParameterName() + " expected " + parameterMeta.getParameterType()
                                    + " but value is " + parameterValue);
                            throw e;
                        }
                    }
                }
                Object result = getCommand().invoke(getControllerInstance(), args);
                if (result instanceof Result) {
                    return Promise.promise(() -> Result.class.cast(result));
                }
                if (result instanceof Promise) {
                    return (Promise<Result>) result;
                }
                log.error("Invalid return type, Result is expected but was " + result);
            } catch (Exception e) {
                log.error("Unable to call the command with path " + path, e);
            }
            return Promise.promise(() -> Controller.badRequest());
        }

        /**
         * Generate a link using the provided parameters
         * 
         * @param parameters
         *            a list of parameters
         * @return
         * @throws ExtensionManagerException
         */
        public String generateLink(Object... parameters) throws ExtensionManagerException {
            if (getParametersMapping().length == 0) {
                return getCommandPattern().pattern();
            }
            if (parameters == null || parameters.length != getParametersMapping().length) {
                throw new ExtensionManagerException("Invalid number of parameters, unable to generate a link");
            }
            try {
                String linkFormat = getCommandPattern().pattern().replace(PARAMETER_REGEXPR, "%s");
                int count = 0;
                Object[] linkFormatParameters = new Object[parameters.length];
                for (ParameterMeta parameterMeta : getParametersMapping()) {
                    linkFormatParameters[count] = String.valueOf(parameters[parameterMeta.getRealIndex()]);
                    count++;
                }
                return String.format(linkFormat, linkFormatParameters);
            } catch (Exception e) {
                throw new ExtensionManagerException("Unable to generate the link", e);
            }
        }

        public String getId() {
            return id;
        }

        private Method getCommand() {
            return command;
        }

        private Pattern getCommandPattern() {
            return commandPattern;
        }

        private String[] getPermissions() {
            return permissions;
        }

        private WebCommandPath.HttpMethod getHttpMethod() {
            return httpMethod;
        }

        private Object getControllerInstance() {
            return controllerInstance;
        }

        private ParameterMeta[] getParametersMapping() {
            return parametersMapping;
        }

        private ISecurityService getSecurityService() {
            return securityService;
        }

        private ISecurityServiceConfiguration getSecurityServiceConfiguration() {
            return securityServiceConfiguration;
        }

        @Override
        public String toString() {
            return "WebCommand [controllerInstance=" + controllerInstance + ", commandPattern=" + commandPattern + ", permissions="
                    + Arrays.toString(permissions) + ", httpMethod=" + httpMethod + ", command=" + command + ", parametersMapping="
                    + Arrays.toString(parametersMapping) + "]";
        }

        /**
         * Meta data for a command parameter.<br/>
         * <ul>
         * <li>parameterName : the name of the parameter</li>
         * <li>realIndex : index of the parameter in the method which implements
         * the command</li>
         * <li>parameterType : the type of the parameter</li>
         * </ul>
         * 
         * @author Pierre-Yves Cloux
         * 
         */
        public class ParameterMeta {
            private String parameterName;
            private int realIndex;
            private Class<?> parameterType;

            public ParameterMeta(String parameterName, int realIndex, Class<?> parameterType) {
                super();
                this.parameterName = parameterName;
                this.realIndex = realIndex;
                this.parameterType = parameterType;
            }

            public int getRealIndex() {
                return realIndex;
            }

            public Class<?> getParameterType() {
                return parameterType;
            }

            public String getParameterName() {
                return parameterName;
            }

            @Override
            public String toString() {
                return "ParameterMeta [parameterName=" + parameterName + ", realIndex=" + realIndex + ", parameterType=" + parameterType + "]";
            }
        }
    }
}
