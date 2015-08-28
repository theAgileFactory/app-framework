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
import org.apache.commons.lang3.StringUtils;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;

import akka.actor.Cancellable;
import framework.security.SecurityUtils;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.database.IDatabaseDependencyService;
import framework.services.ext.ExtensionDescriptor.I18nMessage;
import framework.services.ext.ExtensionDescriptor.MenuCustomizationDescriptor;
import framework.services.ext.ExtensionDescriptor.MenuItemDescriptor;
import framework.services.ext.ExtensionDescriptor.PluginDescriptor;
import framework.services.ext.api.AbstractExtensionController;
import framework.services.ext.api.WebCommandPath;
import framework.services.ext.api.WebControllerPath;
import framework.services.ext.api.WebParameter;
import framework.services.plugins.IPluginManagerService;
import framework.services.plugins.api.IPluginRunner;
import framework.services.plugins.api.PluginException;
import framework.services.router.ICustomRouterService;
import framework.services.system.ISysAdminUtils;
import framework.utils.Menu.ClickableMenuItem;
import framework.utils.Menu.HeaderMenuItem;
import framework.utils.Menu.MenuItem;
import framework.utils.TopMenuBar;
import framework.utils.Utilities;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Function0;
import play.libs.F.Promise;
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
    private IPluginManagerService pluginManagerService;
    private II18nMessagesPlugin iI18nMessagesPlugin;
    private Configuration configuration;
    private Environment environment;
    private ISysAdminUtils sysAdminUtils;
    private IImplementationDefinedObjectService implementationDefinedObjectService;
    private List<IExtension> extensions = Collections.synchronizedList(new ArrayList<IExtension>());
    private Map<Class<?>, Map<String, WebCommand>> extensionControllers = Collections.synchronizedMap(new HashMap<Class<?>, Map<String, WebCommand>>());
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
     * @param configuration
     *            the play application configuration
     * @param pluginManagerService
     *            the plugin manager service to be populated with the plugins
     *            loaded by some extensions
     * @param iI18nMessagesPlugin
     *            the service in charge of the internationalization
     * @param customRouterService
     *            the custom router service to register the extension
     * @param sysAdminUtils
     * @param implementationDefinedObjectService
     * @param databaseDependencyService
     * @throws ExtensionManagerException
     */
    @Inject
    public ExtensionManagerServiceImpl(ApplicationLifecycle lifecycle, Environment environment, Configuration configuration,
            IPluginManagerService pluginManagerService, II18nMessagesPlugin iI18nMessagesPlugin, ICustomRouterService customRouterService,
            ISysAdminUtils sysAdminUtils, IImplementationDefinedObjectService implementationDefinedObjectService,
            IDatabaseDependencyService databaseDependencyService) throws ExtensionManagerException {
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
        this.pluginManagerService = pluginManagerService;
        this.iI18nMessagesPlugin = iI18nMessagesPlugin;
        this.sysAdminUtils = sysAdminUtils;
        this.implementationDefinedObjectService = implementationDefinedObjectService;
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
        return SecurityUtils.checkHasSubject(ctx, new Function0<Result>() {
            @Override
            public Result apply() throws Throwable {
                return execute(path, ctx);
            }
        });
    }

    @Override
    public Result execute(String path, Context ctx) {
        for (WebCommand webCommand : webCommands) {
            if (webCommand.isCompatible(path, ctx)) {
                try {
                    return webCommand.call(path, ctx);
                } catch (Exception e) {
                    log.error("Error while calling the web command", e);
                    return Controller.badRequest();
                }
            }
        }
        log.info("No compatible command found for path " + path);
        if (log.isDebugEnabled()) {
            log.debug("No compatible command found for path " + path);
        }
        return Controller.badRequest();
    }

    @Override
    public String link(Class<?> controller, String commandId, Object... parameters) throws ExtensionManagerException {
        if (!getExtensionControllers().containsKey(controller)) {
            throw new ExtensionManagerException("Unknown controller " + controller);
        }
        WebCommand webCommand = getExtensionControllers().get(controller).get(commandId);
        if (webCommand == null) {
            throw new ExtensionManagerException("Unknown command " + commandId + " in the controller " + controller);
        }
        return getConfiguration().getString("play.http.context") + PATH_PREFIX + webCommand.generateLink(parameters);
    }

    @Override
    public synchronized List<IExtension> getLoadedExtensions() {
        return Collections.unmodifiableList(extensions);
    }

    @Override
    public synchronized void unload(IExtension extension) {
        if (getExtensions().contains(extension)) {
            Extension extensionObject = (Extension) extension;
            for (Class<?> controllerClass : extensionObject.getControllerClasses()) {
                for (WebCommand webCommand : getExtensionControllers().get(controllerClass).values()) {
                    log.info("Unloading web command " + webCommand.getId());
                    getWebCommands().remove(webCommand);
                }
                log.info("Unloading controller " + controllerClass);
                getExtensionControllers().remove(controllerClass);
            }

            // Unloading the i18 resources
            if (extensionObject.getDescriptorInternal().getI18nMessages() != null) {
                for (I18nMessage i18nMessage : extensionObject.getDescriptorInternal().getI18nMessages()) {
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

            getExtensions().remove(extension);

            log.info("Extension " + extension.getDescriptor().getName() + " unloaded");
        }
    }

    @Override
    public synchronized boolean load(String jarFilePath) {
        return addExtension(jarFilePath);
    }

    @Override
    public synchronized boolean customizeMenu() {
        getImplementationDefinedObjectService().resetTopMenuBar();
        for (IExtension extension : getExtensions()) {
            if (extension.getDescriptor().isMenuCustomized()) {
                log.info("Loading menu customization for extension " + extension.getDescriptor().getName());
                ExtensionDescriptor extensionDescriptor = (ExtensionDescriptor) extension.getDescriptor();
                return updateTopMenu(extensionDescriptor.getMenuCustomizationDescriptor());
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
        if (menuCustomizationDescriptor.getMenusToRemove() != null && menuCustomizationDescriptor.getMenusToRemove().size() != 0) {
            TopMenuBar.getInstance().getMain()
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
                            SecurityUtils.getListOfArray(menuItemDescriptor.getPermissions().toArray(new String[menuItemDescriptor.getPermissions().size()])));
                }
                if (menuItemDescriptor.getAddAfterUuid() != null) {
                    log.info("Adding menu " + menuItemDescriptor.getUuid() + " after " + menuItemDescriptor.getAddAfterUuid());
                    result = result && TopMenuBar.getInstance().getMain().addMenuItemAfter(menuItemDescriptor.getAddAfterUuid(), menuItem);
                    log.info("Menu edition status " + result);
                } else {
                    if (menuItemDescriptor.getAddBeforeUuid() != null) {
                        log.info("Adding menu " + menuItemDescriptor.getUuid() + " before " + menuItemDescriptor.getAddBeforeUuid());
                        result = result && TopMenuBar.getInstance().getMain().addMenuItemBefore(menuItemDescriptor.getAddBeforeUuid(), menuItem);
                        log.info("Menu edition status " + result);
                    } else {
                        if (menuItemDescriptor.getToUuid() != null) {
                            log.info("Adding menu " + menuItemDescriptor.getUuid() + " to " + menuItemDescriptor.getToUuid());
                            result = result && TopMenuBar.getInstance().getMain().addSubMenuItemTo(menuItemDescriptor.getToUuid(), menuItem);
                            log.info("Menu edition status " + result);
                        } else {
                            log.info("Adding menu " + menuItemDescriptor.getUuid() + " as top menu");
                            // Neither after nor before (top level menu)
                            TopMenuBar.getInstance().addMenuItem(menuItem);
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
            extension = new Extension(jarFile, getEnvironment());
        } catch (ExtensionManagerException e) {
            log.error("Unable to load the extension " + jarFile, e);
            return false;
        }

        // Prepare the controllers to listen to incoming requests
        List<Class<?>> successFullyLoadedControllers = new ArrayList<Class<?>>();
        for (Object controllerInstance : extension.getControllerInstances()) {
            try {
                addExtensionController(controllerInstance);
                successFullyLoadedControllers.add(controllerInstance.getClass());
            } catch (Exception e) {
                log.error("Error with controller " + controllerInstance, e);
                // Remove all the loaded controllers and their web command
                for (Class<?> successFullyLoadedController : successFullyLoadedControllers) {
                    for (WebCommand webCommand : getExtensionControllers().get(successFullyLoadedController).values()) {
                        getWebCommands().remove(webCommand);
                    }
                    getExtensionControllers().remove(successFullyLoadedController);
                }
                return false;
            }
        }

        // Load the plugins into the plugin manager
        if (extension.getDescriptorInternal().getPluginDescriptors() != null) {
            for (PluginDescriptor pluginDescriptor : extension.getDescriptorInternal().getPluginDescriptors()) {
                try {
                    getPluginManagerService().loadPluginExtension(extension, pluginDescriptor.getIdentifier(), pluginDescriptor.getClazz(),
                            pluginDescriptor.isAvailable());
                } catch (PluginException e) {
                    log.error("Error with plugin " + pluginDescriptor.getIdentifier(), e);
                }
            }
        }

        // Load the i18n keys into
        if (extension.getDescriptorInternal().getI18nMessages() != null) {
            for (I18nMessage i18nMessage : extension.getDescriptorInternal().getI18nMessages()) {
                if (getiI18nMessagesPlugin().isLanguageValid(i18nMessage.getLanguage())) {
                    Properties properties = new Properties();
                    try {
                        properties.load(new StringReader(i18nMessage.getMessages()));
                        for (String key : properties.stringPropertyNames()) {
                            getiI18nMessagesPlugin().add(key, properties.getProperty(key), i18nMessage.getLanguage());
                        }
                        log.info("Loaded i18n keys [" + i18nMessage.getLanguage() + "] for the extension" + extension.getDescriptor().getName());
                    } catch (IOException e) {
                        log.error("Unable to load the i18n keys [" + i18nMessage.getLanguage() + "] for the extension" + extension.getDescriptor().getName(),
                                e);
                    }
                }
            }
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
     * @throws ExtensionManagerException
     */
    private void addExtensionController(Object controllerInstance) throws ExtensionManagerException {
        Class<?> controllerClass = controllerInstance.getClass();
        if (controllerClass.isAnnotationPresent(WebControllerPath.class) && AbstractExtensionController.class.isAssignableFrom(controllerClass)) {
            WebControllerPath controllerAnnotation = controllerClass.getAnnotation(WebControllerPath.class);
            if (!StringUtils.isBlank(controllerAnnotation.path())) {
                try {
                    // Set the link generator
                    AbstractExtensionController extensionController = AbstractExtensionController.class.cast(controllerInstance);
                    extensionController.setLinkGenerationService(this);
                    // Parse the controller to discover its interface
                    Method[] methods = controllerClass.getDeclaredMethods();
                    if (methods != null) {
                        for (Method method : methods) {
                            if (method.isAnnotationPresent(WebCommandPath.class) && method.getReturnType().equals(Result.class)) {
                                WebCommandPath methodAnnotation = method.getAnnotation(WebCommandPath.class);
                                if (!StringUtils.isBlank(methodAnnotation.path())) {
                                    HashSet<String> permissions = new HashSet<String>();
                                    permissions.addAll(Arrays.asList(methodAnnotation.permissions()));
                                    permissions.addAll(Arrays.asList(controllerAnnotation.permissions()));
                                    String commandId = methodAnnotation.id();
                                    if (StringUtils.isBlank(commandId)) {
                                        commandId = UUID.randomUUID().toString();
                                    }
                                    WebCommand webCommand = new WebCommand(controllerInstance, commandId, controllerAnnotation.path() + methodAnnotation.path(),
                                            permissions.toArray(new String[permissions.size()]), methodAnnotation.httpMethod(), method);
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
     * Add a WebCommand to the service
     * 
     * @param controllerInstance
     *            a controller
     * @param webCommand
     *            a command
     */
    private void addWebCommands(Object controllerInstance, WebCommand webCommand) {
        Map<String, WebCommand> controllerCommands = null;
        Class<?> controllerClass = controllerInstance.getClass();
        if (getExtensionControllers().containsKey(controllerClass)) {
            controllerCommands = getExtensionControllers().get(controllerClass);
        } else {
            controllerCommands = Collections.synchronizedMap(new HashMap<String, WebCommand>());
        }
        controllerCommands.put(webCommand.getId(), webCommand);
        getExtensionControllers().put(controllerClass, controllerCommands);
        getWebCommands().add(webCommand);
    }

    private List<WebCommand> getWebCommands() {
        return webCommands;
    }

    private Map<Class<?>, Map<String, WebCommand>> getExtensionControllers() {
        return extensionControllers;
    }

    private List<IExtension> getExtensions() {
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

    private IPluginManagerService getPluginManagerService() {
        return pluginManagerService;
    }

    private II18nMessagesPlugin getiI18nMessagesPlugin() {
        return iI18nMessagesPlugin;
    }

    private ISysAdminUtils getSysAdminUtils() {
        return sysAdminUtils;
    }

    private IImplementationDefinedObjectService getImplementationDefinedObjectService() {
        return implementationDefinedObjectService;
    }

    private Environment getEnvironment() {
        return environment;
    }

    private Configuration getConfiguration() {
        return configuration;
    }

    /**
     * The class which encapsulate an extension
     * 
     * @author Pierre-Yves Cloux
     */
    public static class Extension implements IExtension {
        private Date loadingTime;
        private File jarFile;
        private JarClassLoader jarClassLoader;
        private ExtensionDescriptor descriptor;
        private List<Object> controllers;

        /**
         * Creates an extension using the specified JAR file
         * 
         * @param jarFile
         *            the jar file which contains the extension
         * @param environment
         *            the play environment required to get the play class loader
         */
        public Extension(File jarFile, Environment environment) throws ExtensionManagerException {
            super();
            this.loadingTime = new Date();
            parseJarFile(jarFile, environment);
            this.jarFile = jarFile;
        }

        @Override
        public synchronized IPluginRunner createPluginInstance(String pluginIdentifier) throws ExtensionManagerException {
            try {
                String pluginRunnerClassName = getDescriptorInternal().getPluginClassNameFromIdentifier(pluginIdentifier);
                JclObjectFactory factory = JclObjectFactory.getInstance();
                return (IPluginRunner) factory.create(getJarClassLoader(), pluginRunnerClassName);
            } catch (Exception e) {
                throw new ExtensionManagerException("Unable to create an instance for the specified plugin " + pluginIdentifier, e);
            }
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
         * Read the content of the specified JAR file
         * 
         * @param jarFile
         *            an extension JAR file
         * @param environment
         *            the play environment used to get the play classloader
         * @throws ExtensionManagerException
         */
        private void parseJarFile(File jarFile, Environment environment) throws ExtensionManagerException {
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
                JAXBContext jc = JAXBContext.newInstance(ExtensionDescriptor.class);
                Unmarshaller u = jc.createUnmarshaller();
                this.descriptor = (ExtensionDescriptor) u.unmarshal(inStream);
                log.info("Found descriptor for extension " + descriptor.getName());

                // Loading the controllers
                this.controllers = Collections.synchronizedList(new ArrayList<Object>());
                JclObjectFactory factory = JclObjectFactory.getInstance();
                for (String controllerClassName : getDescriptor().getDeclaredControllers()) {
                    log.info("Loading controller " + controllerClassName);
                    Object obj = factory.create(this.jarClassLoader, controllerClassName);
                    this.controllers.add(obj);
                }
            } catch (Exception e) {
                throw new ExtensionManagerException("Unable to read the JAR extension : " + jarFile, e);
            }
        }

        /**
         * Return the list of dynamically loaded controller classes
         * 
         * @return
         */
        public synchronized List<Class<?>> getControllerClasses() {
            List<Class<?>> controllerClasses = new ArrayList<Class<?>>();
            for (Object controllerInstance : getControllers()) {
                controllerClasses.add(controllerInstance.getClass());
            }
            return controllerClasses;
        }

        /**
         * Return the list of dynamically loaded controllers
         * 
         * @return
         */
        public synchronized List<Object> getControllerInstances() {
            return controllers;
        }

        @Override
        public synchronized IExtensionDescriptor getDescriptor() {
            return descriptor;
        }

        private ExtensionDescriptor getDescriptorInternal() {
            return descriptor;
        }

        public synchronized File getJarFile() {
            return jarFile;
        }

        public synchronized void setJarFile(File jarFile) {
            this.jarFile = jarFile;
        }

        private JarClassLoader getJarClassLoader() {
            return jarClassLoader;
        }

        private List<Object> getControllers() {
            return controllers;
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

        public WebCommand(Object controllerInstance, String id, String webPath, String[] permissions, WebCommandPath.HttpMethod httpMethod, Method command)
                throws ExtensionManagerException {
            super();
            this.id = id;
            this.controllerInstance = controllerInstance;
            findParametersMapping(webPath, command);
            this.httpMethod = httpMethod;
            this.permissions = permissions;
            this.command = command;
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
        public Result call(String path, Context ctx) throws ExtensionManagerException {
            try {
                // Check the permissions
                ArrayList<String[]> perms = new ArrayList<String[]>();
                perms.add(getPermissions());
                if (SecurityUtils.restrict(perms, ctx)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Call to path " + path + " but permissions are not sufficient");
                    }
                    return Controller.badRequest();
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
                    return Result.class.cast(result);
                }
                log.error("Invalid return type, Result is expected but was " + result);
            } catch (Exception e) {
                log.error("Unable to call the command with path " + path, e);
            }
            return Controller.badRequest();
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
