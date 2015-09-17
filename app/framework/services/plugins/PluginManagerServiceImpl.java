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
package framework.services.plugins;

import static akka.actor.SupervisorStrategy.resume;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Identify;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.japi.Function;
import akka.pattern.AskableActorSelection;
import akka.routing.RoundRobinPool;
import akka.util.Timeout;
import framework.commons.DataType;
import framework.commons.IFrameworkConstants;
import framework.commons.message.EventMessage;
import framework.commons.message.EventMessage.MessageType;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.database.IDatabaseDependencyService;
import framework.services.ext.IExtension;
import framework.services.ext.IExtensionManagerService;
import framework.services.ext.api.IExtensionDescriptor.IPluginDescriptor;
import framework.services.plugins.api.AbstractConfiguratorController;
import framework.services.plugins.api.AbstractRegistrationConfiguratorController;
import framework.services.plugins.api.IPluginActionDescriptor;
import framework.services.plugins.api.IPluginContext;
import framework.services.plugins.api.IPluginMenuDescriptor;
import framework.services.plugins.api.IPluginRunner;
import framework.services.plugins.api.PluginException;
import framework.services.storage.ISharedStorageService;
import framework.services.system.ISysAdminUtils;
import framework.utils.Utilities;
import models.framework_models.plugin.PluginConfiguration;
import models.framework_models.plugin.PluginDefinition;
import models.framework_models.plugin.PluginLog;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * This service implementation manages the lifecycle of the plugins.<br/>
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class PluginManagerServiceImpl implements IPluginManagerService, IEventBroadcastingService {
    private static Logger.ALogger log = Logger.of(PluginManagerServiceImpl.class);
    private ActorSystem actorSystem;
    private ActorRef pluginStatusCallbackActorRef;
    private II18nMessagesPlugin messagesPlugin;
    private ISharedStorageService sharedStorageService;
    private IExtensionManagerService extensionManagerService;
    private IPreferenceManagerPlugin preferenceManagerPlugin;
    private Configuration configuration;

    /**
     * Map : key=plugin id , value= {@link PluginRegistrationEntry}.
     */
    private Map<Long, PluginRegistrationEntry> pluginByIds;

    /**
     * Creates a {@link OneForOneStrategy} using the specified parameters.
     * 
     * @param numberOfRetry
     *            a number of retry
     * @param withinTimeRange
     *            the time range
     * @param pluginConfigurationId
     *            the unique id of the plugin configuration
     */
    private static SupervisorStrategy getSupervisorStrategy(int numberOfRetry, Duration withinTimeRange, Long pluginConfigurationId) {
        final String errorMessage = String.format("An provisioning processor of the plugin %d reported an exception, retry", pluginConfigurationId);
        return new OneForOneStrategy(numberOfRetry, withinTimeRange, new Function<Throwable, Directive>() {
            @Override
            public Directive apply(Throwable t) {
                log.error(errorMessage, t);
                return resume();
            }
        });
    }

    /**
     * Default constructor.
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param sysAdminUtils
     *            scheduler which may be used by some plugins
     * @param messagesPlugin
     *            the i18n plugin
     * @param databaseDependencyService
     *            the service which ensures that the database is available
     * @param sharedStorageService
     *            the service which is managing the shared storage for the
     *            plugins
     * @param extensionManagerService
     *            the extension manager to be used to "load" the plugin
     *            instances
     * @param preferenceManagerPlugin
     *            the plugin which is managing preferences
     * @param configuration
     *            the play configuration
     */
    @Inject
    public PluginManagerServiceImpl(ApplicationLifecycle lifecycle, ISysAdminUtils sysAdminUtils, II18nMessagesPlugin messagesPlugin,
            IDatabaseDependencyService databaseDependencyService, ISharedStorageService sharedStorageService, IExtensionManagerService extensionManagerService,
            IPreferenceManagerPlugin preferenceManagerPlugin, Configuration configuration) {
        log.info("SERVICE>>> PluginManagerServiceImpl starting...");
        this.messagesPlugin = messagesPlugin;
        this.sharedStorageService = sharedStorageService;
        this.extensionManagerService = extensionManagerService;
        this.preferenceManagerPlugin = preferenceManagerPlugin;
        this.configuration = configuration;
        pluginByIds = Collections.synchronizedMap(new HashMap<Long, PluginRegistrationEntry>());
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> PluginManagerServiceImpl stopping...");
            shutdown();
            log.info("SERVICE>>> PluginManagerServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> PluginManagerServiceImpl started");
    }

    /**
     * Get the list of plugins available in the loaded extensions.<br/>
     * The map returned is indexed using the unique plugin identifier. The
     * structure returned is a Triple of
     * <ul>
     * <li>a boolean : true if the plugin is available</li>
     * <li>an extention : the one which contains the plugin</li>
     * <li>a plugin descriptor which describes the specified plugin</li>
     * </ul>
     * 
     * @return a map
     * @throws PluginException
     */
    private Map<String, Triple<Boolean, IExtension, IPluginDescriptor>> getExtensionPlugins() {
        Map<String, Triple<Boolean, IExtension, IPluginDescriptor>> extensionPluginDescriptors = new HashMap<>();
        for (IExtension extension : getExtensionManagerService().getLoadedExtensions()) {
            for (IPluginDescriptor pluginDescriptor : extension.getDescriptor().getDeclaredPlugins().values()) {
                PluginDefinition pluginDefinition = PluginDefinition.getPluginDefinitionFromIdentifier(pluginDescriptor.getIdentifier());
                if (pluginDefinition != null) {
                    // If the plugin is not currently listed into the database,
                    // do not load it
                    extensionPluginDescriptors.put(pluginDescriptor.getIdentifier(), Triple.of(pluginDefinition.isAvailable, extension, pluginDescriptor));
                }
            }
        }
        return extensionPluginDescriptors;
    }

    @Override
    public InputStream getPluginSmallImageSrc(String pluginDefinitionIdentifier) {
        IExtension extension = getExtensionPlugins().get(pluginDefinitionIdentifier).getMiddle();
        if (extension == null)
            return null;
        return extension.getResourceAsStream(String.format(IFrameworkConstants.PLUGIN_SMALL_IMAGE_TEMPLATE, pluginDefinitionIdentifier));
    }

    @Override
    public InputStream getPluginBigImageSrc(String pluginDefinitionIdentifier) {
        IExtension extension = getExtensionPlugins().get(pluginDefinitionIdentifier).getMiddle();
        if (extension == null)
            return null;
        return extension.getResourceAsStream(String.format(IFrameworkConstants.PLUGIN_BIG_IMAGE_TEMPLATE, pluginDefinitionIdentifier));
    }

    @Override
    public Map<String, Pair<Boolean, IPluginDescriptor>> getAllPluginDescriptors() {
        Map<String, Triple<Boolean, IExtension, IPluginDescriptor>> extensionPlugins = getExtensionPlugins();
        Map<String, Pair<Boolean, IPluginDescriptor>> pluginDescriptions = new HashMap<>();
        for (String pluginIdentifier : extensionPlugins.keySet()) {
            try {
                Triple<Boolean, IExtension, IPluginDescriptor> extensionPluginRecord = extensionPlugins.get(pluginIdentifier);
                pluginDescriptions.put(pluginIdentifier, Pair.of(extensionPluginRecord.getLeft(), extensionPluginRecord.getRight()));
            } catch (Exception e) {
                String message = String.format("Unable to instanciate the plugin %s", pluginIdentifier);
                log.error(message, e);
            }
        }
        return pluginDescriptions;
    }

    @Override
    public boolean isPluginAvailable(String pluginDefinitionIdentifier) {
        PluginDefinition pluginDefinition = PluginDefinition.getPluginDefinitionFromIdentifier(pluginDefinitionIdentifier);
        return pluginDefinition != null && pluginDefinition.isAvailable;
    }

    @Override
    public IPluginDescriptor getAvailablePluginDescriptor(String pluginDefinitionIdentifier) {
        Triple<Boolean, IExtension, IPluginDescriptor> extensionPluginRecord = getExtensionPlugins().get(pluginDefinitionIdentifier);
        if (extensionPluginRecord == null || !extensionPluginRecord.getLeft()) {
            return null;
        }
        return extensionPluginRecord.getRight();
    }

    @Override
    public IPluginDescriptor getPluginDescriptor(String pluginDefinitionIdentifier) {
        Triple<Boolean, IExtension, IPluginDescriptor> extensionPluginRecord = getExtensionPlugins().get(pluginDefinitionIdentifier);
        if (extensionPluginRecord == null) {
            return null;
        }
        return extensionPluginRecord.getRight();
    }

    @Override
    public void createActors(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;

        // Check the plugin definitions before starting
        try {
            hideNotLoadedPluginDefinitions();
        } catch (PluginException e) {
            throw new RuntimeException("UNEXPECTED ERROR : unable to check the plugin definitions", e);
        }

        // Start the actor which will receive the notifications from the actors
        // (I am started, I am stopped)
        this.pluginStatusCallbackActorRef = getActorSystem().actorOf(Props.create(new PluginStatusCallbackActorCreator(getPluginByIds())));
        List<PluginConfiguration> pluginConfigurations = PluginConfiguration.getAllAvailablePlugins();
        if (pluginConfigurations != null) {
            for (PluginConfiguration pluginConfiguration : pluginConfigurations) {
                try {
                    registerPluginRunner(pluginConfiguration.id);
                    if (pluginConfiguration.isAutostart) {
                        // If the plugin is maked as "autostart" then auto-start
                        // it
                        startPluginRunner(pluginConfiguration.id);
                    }
                } catch (Exception e) {
                    log.error("Error while starting the PluginManagerService", e);
                }
            }
        }
    }

    /**
     * Set all the plugin definitions for which no class exist in the manager to
     * "not available"
     * 
     * @throws PluginException
     */
    private void hideNotLoadedPluginDefinitions() throws PluginException {
        // Set all the NOT LOADED plugin definitions to "unavailable"
        List<PluginDefinition> pluginDefinitions = PluginDefinition.getAllPluginDefinitions();
        Map<String, Triple<Boolean, IExtension, IPluginDescriptor>> extensionPlugins = getExtensionPlugins();
        if (pluginDefinitions != null) {
            for (PluginDefinition pluginDefinition : pluginDefinitions) {
                if (!extensionPlugins.containsKey(pluginDefinition.identifier)) {
                    log.warn("Make plugin definition " + pluginDefinition.identifier + " unavailable since it is not loaded");
                    pluginDefinition.isAvailable = false;
                    pluginDefinition.save();

                    // Check if this definition has a configuration
                    if (pluginDefinition.pluginConfigurations != null) {
                        for (PluginConfiguration pluginConfiguration : pluginDefinition.pluginConfigurations) {
                            log.error("WARNING>>> A plugin configuration " + pluginConfiguration.name + " is loaded for the plugin "
                                    + pluginDefinition.identifier + " while this one is not loaded and not avalable");
                        }
                    }
                }
            }
        }
    }

    public void shutdown() {
        if (isActorSystemReady()) {
            try {
                stopAll();
            } catch (Exception e) {
                log.error("Exception while stopping all the plugins at shutdown");
            }
            try {
                getActorSystem().stop(getPluginStatusCallbackActorRef());
            } catch (Exception e) {
                log.error("Exception stopping the status callback actor");
            }
        }
    }

    @Override
    public void registerPlugin(Long pluginConfigurationId) throws PluginException {
        if (isActorSystemReady()) {
            registerPluginRunner(pluginConfigurationId);
        }
    }

    /**
     * Register the plugin (if it is not yet registered).<br/>
     * It instantiate the plugin runner class and record it in memory into the
     * plugin manager registry.
     * 
     * @param pluginConfigurationId
     *            a plugin configuration id
     * @throws PluginException
     */
    private void registerPluginRunner(Long pluginConfigurationId) throws PluginException {
        synchronized (getPluginByIds()) {
            if (!getPluginByIds().containsKey(pluginConfigurationId)) {
                PluginConfiguration pluginConfiguration = PluginConfiguration.getAvailablePluginById(pluginConfigurationId);
                PluginRegistrationEntry pluginRegistrationEntry = initializePlugin(pluginConfiguration);
                if (pluginRegistrationEntry != null) {
                    getPluginByIds().put(pluginConfigurationId, pluginRegistrationEntry);
                }
            }
        }
    }

    @Override
    public void unregisterPlugin(Long pluginConfigurationId) throws PluginException {
        if (isActorSystemReady()) {
            synchronized (getPluginByIds()) {
                PluginRegistrationEntry pluginRegistrationEntry = getPluginByIds().get(pluginConfigurationId);
                if (pluginRegistrationEntry != null && pluginRegistrationEntry.getPluginStatus().equals(PluginStatus.STOPPED)) {
                    unInitializePlugin(pluginRegistrationEntry);
                    getPluginByIds().remove(pluginConfigurationId);
                } else {
                    throw new PluginException("Cannot unregister a plugin which is already running");
                }
            }
        }
    }

    @Override
    public Map<Long, IPluginInfo> getRegisteredPluginDescriptors() {
        HashMap<Long, IPluginInfo> registeredPluginDescriptors = new HashMap<Long, IPluginInfo>();
        for (PluginRegistrationEntry pluginRegistrationEntry : getPluginByIds().values()) {
            registeredPluginDescriptors.put(pluginRegistrationEntry.getPluginConfigurationId(), pluginRegistrationEntry);
        }
        return registeredPluginDescriptors;
    }

    /**
     * Load the plugin class, call the "init" method by passing the
     * {@link IPluginContext} to the plugin, start the lifecycle management
     * actor and create the {@link PluginRegistrationEntry}.
     * 
     * @param pluginConfiguration
     *            a plugin configuration extracted from the database.
     * @return a plugin registration entry ready to be added to the plugin
     *         register
     * @throws PluginException
     */
    private PluginRegistrationEntry initializePlugin(PluginConfiguration pluginConfiguration) throws PluginException {
        log.info(String.format("[BEGIN] initialize the plugin %d", pluginConfiguration.id));
        try {
            String pluginIdentifier = pluginConfiguration.pluginDefinition.identifier;
            IPluginDescriptor pluginDescriptor = getExtensionPlugins().get(pluginIdentifier).getRight();
            IPluginContext pluginContext = new PluginContextImpl(pluginConfiguration, pluginDescriptor, this, this, getSharedStorageService(),
                    getPreferenceManagerPlugin(), getConfiguration());
            Triple<IPluginRunner, Object, Map<DataType, Object>> plugin = getExtensionManagerService().loadAndInitPluginInstance(pluginIdentifier,
                    pluginConfiguration.id, pluginContext);
            log.info(String.format("The class for the plugin %d has been found and instanciated", pluginConfiguration.id));
            ActorRef pluginLifeCycleControllingActorRef = getActorSystem()
                    .actorOf(Props.create(new PluginLifeCycleControllingActorCreator(pluginConfiguration.id, plugin.getLeft(),
                            getPluginStatusCallbackActorRef(), getMessagesPlugin())));
            log.info(String.format("[END] the plugin %d has been initialized", pluginConfiguration.id));
            return new PluginRegistrationEntry(pluginConfiguration.id, plugin.getLeft(), plugin.getMiddle(), plugin.getRight(), pluginDescriptor,
                    pluginLifeCycleControllingActorRef);
        } catch (Exception e) {
            String message = String.format("Unable to initialize the plugin %d", pluginConfiguration.id);
            log.error(message, e);
            throw new PluginException(message, e);
        }
    }

    /**
     * This method stop the plugin lifecycle management actor.
     * 
     * @param pluginRegistrationEntry
     */
    private void unInitializePlugin(PluginRegistrationEntry pluginRegistrationEntry) {
        try {
            getExtensionManagerService().unloadPluginInstance(pluginRegistrationEntry.getPluginRunner(),
                    pluginRegistrationEntry.getDescriptor().getIdentifier(), pluginRegistrationEntry.getPluginConfigurationId());
        } catch (Exception e) {
            log.error(String.format("Unable to uninitialize the plugin %d", pluginRegistrationEntry.getPluginConfigurationId()), e);
        }
        try {
            getActorSystem().stop(pluginRegistrationEntry.getLifeCycleControllingRouter());
        } catch (Exception e) {
            log.error(String.format("Unable to uninitialize the plugin %d", pluginRegistrationEntry.getPluginConfigurationId()), e);
        }
    }

    @Override
    public PluginStatus getPluginStatus(Long pluginConfigurationId) throws PluginException {
        PluginRegistrationEntry pluginRegistrationEntry = getPluginByIds().get(pluginConfigurationId);
        if (pluginRegistrationEntry != null) {
            return pluginRegistrationEntry.getPluginStatus();
        } else {
            throw new PluginException(String.format("Unknown plugin %d cannot get status", pluginConfigurationId));
        }
    }

    @Override
    public void startPlugin(Long pluginConfigurationId) throws PluginException {
        if (isActorSystemReady()) {
            startPluginRunner(pluginConfigurationId);
        }
    }

    /**
     * Start the specified plugin.<br/>
     * Actually this method
     * <ol>
     * <li>send the START message to the plugin lifecycle management actor</li>
     * <li>start the actor managing the IN interface (if any)</li>
     * <li>start the actor managing the OUT interface (if any)</li>
     * </ol>
     * This requires all the associated actors (lifecycle management and IN/OUT
     * interfaces) to be fully stopped.
     * 
     * @param pluginConfigurationId
     *            the plugin configuration id
     * @throws PluginException
     */
    private void startPluginRunner(Long pluginConfigurationId) {
        PluginRegistrationEntry pluginRegistrationEntry = getPluginByIds().get(pluginConfigurationId);
        if (pluginRegistrationEntry == null) {
            log.error(String.format("Attempt to start an unknown or unregistered plugin %d", pluginConfigurationId));
            return;
        }
        synchronized (pluginRegistrationEntry) {
            ActorRef outActorRef = null;
            ActorRef inActorRef = null;
            // Check if the plugin is stopped
            if (pluginRegistrationEntry.getPluginStatus().equals(PluginStatus.STOPPED)
                    && isActorStopped(FlowType.IN.getRouterPrefix() + pluginRegistrationEntry.getPluginConfigurationId(), getActorSystem())
                    && isActorStopped(FlowType.OUT.getRouterPrefix() + pluginRegistrationEntry.getPluginConfigurationId(), getActorSystem())) {
                try {
                    // Set the plugin as Starting
                    pluginRegistrationEntry.setPluginStatus(PluginStatus.STARTING);

                    // Send START message to the lifecycle management router
                    pluginRegistrationEntry.getLifeCycleControllingRouter().tell(LifeCycleMessage.START, ActorRef.noSender());
                    // Start the OUT interface routing actor (if any)
                    outActorRef = startEventMessageProcessingActor(pluginRegistrationEntry, FlowType.OUT);
                    pluginRegistrationEntry.setOutEventMessageProcessingActorRef(outActorRef);
                    // Start the IN interface routing actor (if any)
                    inActorRef = startEventMessageProcessingActor(pluginRegistrationEntry, FlowType.IN);
                    pluginRegistrationEntry.setInEventMessageProcessingActorRef(inActorRef);

                    log.info(String.format("The plugin %d is starting", pluginConfigurationId));
                } catch (Exception e) {
                    String uuid = UUID.randomUUID().toString();
                    log.error(String.format("The plugin %d cannot be started, unexpected error %s", pluginConfigurationId, uuid), e);
                    PluginLog.saveStartPluginLog(pluginConfigurationId, getMessagesPlugin().get("plugin.failed.start", pluginConfigurationId, uuid), true);
                }
            } else {
                log.error(String.format("The router for the plugin configuration %d is not stopped, cannot start it", pluginConfigurationId));
                return;
            }
        }
    }

    /**
     * Start the router associated with the specified flow type (IN or OUT).
     * 
     * @param pluginRegistrationEntry
     *            a plugin registration entry
     * @param flowType
     *            a type of flow
     * @return the actor ref created
     */
    private ActorRef startEventMessageProcessingActor(PluginRegistrationEntry pluginRegistrationEntry, FlowType flowType) {
        ActorRef actorRef = null;
        EventInterfaceConfiguration eventInterfaceConfiguration = null;

        if (flowType.equals(FlowType.IN)) {
            eventInterfaceConfiguration = (pluginRegistrationEntry.getDescriptor().hasInMessageInterface() ? new EventInterfaceConfiguration() : null);
        } else {
            eventInterfaceConfiguration = (pluginRegistrationEntry.getDescriptor().hasOutMessageInterface() ? new EventInterfaceConfiguration() : null);
        }
        if (eventInterfaceConfiguration != null) {
            actorRef = getActorSystem().actorOf(
                    (new RoundRobinPool(eventInterfaceConfiguration.getPoolSize()))
                            .withSupervisorStrategy(getSupervisorStrategy(eventInterfaceConfiguration.getNumberOfRetry(),
                                    eventInterfaceConfiguration.getRetryDuration(), pluginRegistrationEntry.getPluginConfigurationId()))
                    .props(Props.create(new EventMessageProcessingActorCreator(pluginRegistrationEntry.getPluginConfigurationId(),
                            pluginRegistrationEntry.getPluginRunner(), FlowType.OUT))),
                    flowType.getRouterPrefix() + pluginRegistrationEntry.getPluginConfigurationId());
            String message = "The %s interface for the plugin %d has been started";
            log.info(String.format(message, flowType.name(), pluginRegistrationEntry.getPluginConfigurationId()));
            return actorRef;
        }
        return null;
    }

    @Override
    public void stopPlugin(Long pluginConfigurationId) {
        if (isActorSystemReady()) {
            stopPluginRunner(pluginConfigurationId);
        }
    }

    /**
     * Stop the specified plugin.<br/>
     * <b>It is assumed that the plugin exists.</b> Here is the sequence which
     * is executed:
     * <ol>
     * <li>Stopping the IN interface (if any)</li>
     * <li>Stopping the OUT interface (if any)</li>
     * <li>Sending to the plugin lifecycle management actor the STOP message
     * </li>
     * </ol>
     * 
     * <b>WARNING</b> : if the stop process succeed, the plugin status is
     * STOPPING. The status may be checked later for STOPPED status (which can
     * occur asynchronously).
     * 
     * @param pluginConfigurationId
     *            the unique id of the plugin configuration
     * @throws PluginException
     */
    private void stopPluginRunner(Long pluginConfigurationId) {
        PluginRegistrationEntry pluginRegistrationEntry = getPluginByIds().get(pluginConfigurationId);
        if (pluginRegistrationEntry == null) {
            log.error(String.format("Attempt to start an unknown or unregistered plugin %d", pluginConfigurationId));
            return;
        }
        synchronized (pluginRegistrationEntry) {
            if (pluginRegistrationEntry.getPluginStatus().equals(PluginStatus.STARTED)
                    || pluginRegistrationEntry.getPluginStatus().equals(PluginStatus.START_FAILED)) {
                try {
                    pluginRegistrationEntry.setPluginStatus(PluginStatus.STOPPING);

                    // Send STOP message to the lifecycle management router
                    pluginRegistrationEntry.getLifeCycleControllingRouter().tell(LifeCycleMessage.STOP, ActorRef.noSender());
                    // Stop the listening interfaces
                    stopEventMessageProcessingActor(pluginRegistrationEntry, FlowType.IN);
                    stopEventMessageProcessingActor(pluginRegistrationEntry, FlowType.OUT);

                    log.info(String.format("The plugin %d is stopping", pluginConfigurationId));
                } catch (Exception e) {
                    String uuid = UUID.randomUUID().toString();
                    log.error(String.format("The plugin %d cannot be stopped, status is unknown, id of error is %s", pluginConfigurationId, uuid), e);
                    PluginLog.saveStopPluginLog(pluginConfigurationId, getMessagesPlugin().get("plugin.failed.stop", pluginConfigurationId, uuid), true);
                }
            } else {
                log.error(String.format("The plugin %d is not started, cannot stop it", pluginConfigurationId));
            }
        }
    }

    /**
     * Stop the actor which is associated with an event processing interface.
     * 
     * @param pluginRegistrationEntry
     * @param flowType
     */
    private void stopEventMessageProcessingActor(PluginRegistrationEntry pluginRegistrationEntry, FlowType flowType) {
        ActorRef router = null;
        if (flowType.equals(FlowType.IN)) {
            router = pluginRegistrationEntry.getInEventMessageProcessingActorRef();
        } else {
            router = pluginRegistrationEntry.getOutEventMessageProcessingActorRef();
        }
        if (router != null) {
            // Stop the out interface
            getActorSystem().stop(router);
            log.info(String.format("The %s interface router for the plugin %d has been stopped", flowType.name(),
                    pluginRegistrationEntry.getPluginConfigurationId()));
        }
    }

    /**
     * Stop all the registered plugin.
     */
    private void stopAllPluginFlows() {
        synchronized (getPluginByIds()) {
            for (Long pluginConfigurationId : getPluginByIds().keySet()) {
                stopPluginRunner(pluginConfigurationId);
            }
        }
    }

    @Override
    public void stopAll() {
        if (isActorSystemReady()) {
            stopAllPluginFlows();
        }
    }

    /**
     * Post a provisioning message which will be handled asynchronously.
     * 
     * @param eventMessage
     *            a event message
     */
    public void postOutMessage(EventMessage eventMessage) {
        postMessage(FlowType.OUT, eventMessage);
    }

    @Override
    public void postInMessage(EventMessage eventMessage) {
        postMessage(FlowType.IN, eventMessage);
    }

    @Override
    public List<Triple<Long, String, IPluginInfo>> getPluginSupportingRegistrationForDataType(DataType dataType) {
        List<Triple<Long, String, IPluginInfo>> pluginsSupportingRegistration = new ArrayList<Triple<Long, String, IPluginInfo>>();
        for (PluginRegistrationEntry pluginRegistrationEntry : getPluginByIds().values()) {
            if (pluginRegistrationEntry.getDescriptor().getRegistrationConfiguratorControllerClassNames() != null) {
                Set<DataType> supportedDataTypes = pluginRegistrationEntry.getDescriptor().getRegistrationConfiguratorControllerClassNames().keySet();
                if (supportedDataTypes != null && supportedDataTypes.contains(dataType)) {
                    pluginsSupportingRegistration.add(Triple.of(pluginRegistrationEntry.getPluginConfigurationId(),
                            PluginConfiguration.getAvailablePluginById(pluginRegistrationEntry.getPluginConfigurationId()).name,
                            (IPluginInfo) pluginRegistrationEntry));
                }
            }
        }
        return pluginsSupportingRegistration;
    }

    /**
     * Post a provisioning message which will be handled asynchronously. There
     * is two possible scenarios:
     * <ul>
     * <li>The message is CUSTOM : this means that one specific plugin must be
     * notified and its identifier should be hold by the {@link EventMessage}
     * </li>
     * <li>The message is not CUSTOM : then all the plugins which are
     * "compatible" with the {@link DataType} of the message are notified</li>
     * </ul>
     * 
     * @param flowType
     *            the type of the event flow (IN or OUT)
     * @param eventMessage
     *            a event message
     * @throws PluginException
     */
    private void postMessage(FlowType flowType, EventMessage eventMessage) {
        if (isActorSystemReady()) {
            if (eventMessage != null && eventMessage.isConsistent()) {
                if (eventMessage.getMessageType().equals(MessageType.CUSTOM)) {
                    log.info(String.format("Dispatching the event %s to the plugin %d through the %s interface", eventMessage.getTransactionId(),
                            eventMessage.getPluginConfigurationId(), flowType.name()));
                    PluginRegistrationEntry pluginRegistrationEntry = getPluginByIds().get(eventMessage.getPluginConfigurationId());
                    if (pluginRegistrationEntry != null && pluginRegistrationEntry.getPluginStatus().equals(PluginStatus.STARTED)) {
                        dispatchMessage(flowType, eventMessage, pluginRegistrationEntry, eventMessage.getPluginConfigurationId());
                    } else {
                        log.error(String.format(
                                "Attempt to dispatch the event %s to the plugin %d while this one is not started or not existing. Here is the message : %s.",
                                eventMessage.getTransactionId(), eventMessage.getPluginConfigurationId(), eventMessage.toString()));
                    }
                } else {
                    log.info(String.format("Dispatching the event %s to all the plugins %s interface", eventMessage.getTransactionId(), flowType.name()));
                    for (PluginRegistrationEntry pluginRegistrationEntry : getPluginByIds().values()) {
                        // Check if the plugin is compatible with the message
                        // data type and dispatch
                        if (pluginRegistrationEntry.getPluginStatus().equals(PluginStatus.STARTED)
                                && pluginRegistrationEntry.isPluginCompatible(eventMessage.getDataType())) {
                            dispatchMessage(flowType, eventMessage, pluginRegistrationEntry, pluginRegistrationEntry.getPluginConfigurationId());
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("Invalid message posted " + eventMessage);
            }
        }
    }

    /**
     * Dispatch a message according to the specified flow and to the specified
     * plugin.
     * 
     * @param flowType
     *            a flow type (IN or OUT)
     * @param eventMessage
     *            a message to dispatch
     * @param pluginRegistrationEntry
     *            a registration entry
     * @param pluginConfigurationId
     *            the plugin configuration id
     */
    private void dispatchMessage(FlowType flowType, EventMessage eventMessage, PluginRegistrationEntry pluginRegistrationEntry, Long pluginConfigurationId) {
        ActorRef actorRef;
        if (flowType.equals(FlowType.OUT)) {
            actorRef = pluginRegistrationEntry.getOutEventMessageProcessingActorRef();
        } else {
            actorRef = pluginRegistrationEntry.getInEventMessageProcessingActorRef();
        }
        if (actorRef != null) {
            actorRef.tell(eventMessage, ActorRef.noSender());
        } else {
            log.info(String.format("No event dispatched %s to the plugin %d since its %s interface is not started", eventMessage.getTransactionId(),
                    pluginConfigurationId, flowType.name()));
        }
    }

    private ActorSystem getActorSystem() {
        return actorSystem;
    }

    private boolean isActorSystemReady() {
        return getActorSystem() != null && !getActorSystem().isTerminated();
    }

    private Map<Long, PluginRegistrationEntry> getPluginByIds() {
        return pluginByIds;
    }

    private ActorRef getPluginStatusCallbackActorRef() {
        return pluginStatusCallbackActorRef;
    }

    /**
     * Return true if the specified actor exists.
     * 
     * @param actorPath
     *            the path to an actor
     * @param actorSystem
     *            the Akka actor system
     * @return a boolean
     */
    private static boolean isActorStopped(String actorPath, ActorSystem actorSystem) {
        ActorSelection actorSelection = actorSystem.actorSelection(actorPath);
        Timeout t = new Timeout(5, TimeUnit.SECONDS);
        AskableActorSelection asker = new AskableActorSelection(actorSelection);
        Future<Object> fut = asker.ask(new Identify(1), t);
        ActorIdentity ident;
        try {
            ident = (ActorIdentity) Await.result(fut, t.duration());
            ActorRef ref = ident.getRef();
            return ref == null;
        } catch (Exception e) {
            log.error(String.format("Error while searching for an actor path %s to check if the actor is running or not", actorPath), e);
        }
        return false;
    }

    private II18nMessagesPlugin getMessagesPlugin() {
        return messagesPlugin;
    }

    private ISharedStorageService getSharedStorageService() {
        return sharedStorageService;
    }

    private IExtensionManagerService getExtensionManagerService() {
        return extensionManagerService;
    }

    private IPreferenceManagerPlugin getPreferenceManagerPlugin() {
        return preferenceManagerPlugin;
    }

    private Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Direction of the plugin configuration (OUT or IN).
     * 
     * @author Pierre-Yves Cloux
     */
    public enum FlowType {
        IN("in-router-"), OUT("out-router-");

        private String routerPrefix;

        /**
         * Default constructor.
         * 
         * @param routerPrefix
         */
        private FlowType(String routerPrefix) {
            this.routerPrefix = routerPrefix;
        }

        /**
         * Return the prefix for the Akka router associated with the specified
         * interface (IN or OUT)
         * 
         * @return a string
         */
        public String getRouterPrefix() {
            return routerPrefix;
        }
    }

    /**
     * The lifecycle message which could receive a plugin control interface.
     */
    public enum LifeCycleMessage {
        START, STOP
    }

    /**
     * The lifecycle message which are notified by the lifecycle controlling
     * actor of a plugin to the {@link PluginStatusCallbackActor}.
     */
    public static class CallbackLifeCycleMessage implements Serializable {
        private static final long serialVersionUID = -1298110917944515230L;
        private PluginStatus pluginStatus;
        private Long pluginConfigurationId;

        public CallbackLifeCycleMessage(PluginStatus pluginStatus, Long pluginConfigurationId) {
            super();
            this.pluginStatus = pluginStatus;
            this.pluginConfigurationId = pluginConfigurationId;
        }

        public Long getPluginConfigurationId() {
            return pluginConfigurationId;
        }

        public PluginStatus getPluginStatus() {
            return pluginStatus;
        }
    }

    /**
     * A plugin registration entry which holds the data related to a registered
     * plugin.<br/>
     * <ul>
     * <li>pluginConfigurationId : the unique id of the plugin configuration
     * </li>
     * <li>pluginStatus : the status of the plugin (started, stopped)</li>
     * <li>inEventMessageProcessingActorRef : the reference to the actor which
     * is managing the IN interface of the plugin (if any)</li>
     * <li>outEventMessageProcessingActorRef : the reference to the actor which
     * is managing the OUT interface of the plugin (if any)</li>
     * <li>lifeCycleControllingRouter : the reference to the actor which is
     * managing the interface which controls the life cycle (start, stop) of the
     * plugin</li>
     * <li>pluginRunner : a reference to the plugin itself</li>
     * <li>getLinkToCustomConfiguration : get a link to the controller which is
     * managing the custom configuration of the plugin</li>
     * <li>getLinkToRegistrationConfiguration : get a link to the controller
     * which is managing a defined object registration</li>
     * </ul>
     */
    public static class PluginRegistrationEntry implements IPluginInfo {
        private Long pluginConfigurationId;
        private PluginStatus pluginStatus;
        private ActorRef inEventMessageProcessingActorRef;
        private ActorRef outEventMessageProcessingActorRef;
        private ActorRef lifeCycleControllingRouter;
        private IPluginDescriptor descriptor;
        private IPluginRunner pluginRunner;
        private Object customConfigurationController;
        private Map<DataType, Object> registrationConfigurationControllers;

        public PluginRegistrationEntry(Long pluginConfigurationId, IPluginRunner pluginRunner, Object customConfigurationController,
                Map<DataType, Object> registrationConfigurationControllers, IPluginDescriptor descriptor, ActorRef lifeCycleControllingRouter) {
            super();
            this.pluginConfigurationId = pluginConfigurationId;
            this.pluginRunner = pluginRunner;
            this.customConfigurationController = customConfigurationController;
            this.registrationConfigurationControllers = registrationConfigurationControllers;
            this.descriptor = descriptor;
            this.lifeCycleControllingRouter = lifeCycleControllingRouter;
            pluginStatus = PluginStatus.STOPPED;
        }

        public synchronized ActorRef getOutEventMessageProcessingActorRef() {
            return outEventMessageProcessingActorRef;
        }

        public synchronized ActorRef getInEventMessageProcessingActorRef() {
            return inEventMessageProcessingActorRef;
        }

        public synchronized void setInEventMessageProcessingActorRef(ActorRef inEventMessageProcessingActorRef) {
            this.inEventMessageProcessingActorRef = inEventMessageProcessingActorRef;
        }

        public synchronized void setOutEventMessageProcessingActorRef(ActorRef outEventMessageProcessingActorRef) {
            this.outEventMessageProcessingActorRef = outEventMessageProcessingActorRef;
        }

        public synchronized Long getPluginConfigurationId() {
            return pluginConfigurationId;
        }

        public synchronized boolean isPluginCompatible(DataType dataType) {
            if (getDescriptor().getSupportedDataTypes() == null) {
                return false;
            }
            return getDescriptor().getSupportedDataTypes().contains(dataType);
        }

        public synchronized void setPluginStatus(PluginStatus pluginStatus) {
            this.pluginStatus = pluginStatus;
        }

        public synchronized PluginStatus getPluginStatus() {
            return pluginStatus;
        }

        @Override
        public IPluginDescriptor getDescriptor() {
            return descriptor;
        }

        public synchronized IPluginRunner getPluginRunner() {
            return pluginRunner;
        }

        public synchronized ActorRef getLifeCycleControllingRouter() {
            return lifeCycleControllingRouter;
        }

        @Override
        public synchronized IPluginMenuDescriptor getMenuDescriptor() {
            return pluginRunner.getMenuDescriptor();
        }

        @Override
        public synchronized Map<String, IPluginActionDescriptor> getActionDescriptors() {
            return pluginRunner.getActionDescriptors();
        }

        @Override
        public synchronized String getLinkToCustomConfiguration() {
            @SuppressWarnings("rawtypes")
            AbstractConfiguratorController ctrl = (AbstractConfiguratorController) customConfigurationController;
            if (ctrl == null) {
                return "";
            }
            return ctrl.linkDefault();
        }

        @Override
        public synchronized String getLinkToRegistrationConfiguration(DataType dataType, Long objectId) {
            if (registrationConfigurationControllers == null) {
                return "";
            }
            @SuppressWarnings("rawtypes")
            AbstractRegistrationConfiguratorController ctrl = (AbstractRegistrationConfiguratorController) registrationConfigurationControllers.get(dataType);
            if (ctrl == null) {
                return "";
            }
            return ctrl.linkDefault(objectId);
        }

        @Override
        public synchronized boolean hasCustomConfigurator() {
            return customConfigurationController != null;
        }

        @Override
        public synchronized boolean isRegistrableWith(DataType dataType) {
            return registrationConfigurationControllers != null && registrationConfigurationControllers.containsKey(dataType);
        }

        @Override
        public String toString() {
            return "PluginRegistrationEntry [pluginConfigurationId=" + pluginConfigurationId + ", pluginStatus=" + pluginStatus
                    + ", inEventMessageProcessingActorRef=" + inEventMessageProcessingActorRef + ", outEventMessageProcessingActorRef="
                    + outEventMessageProcessingActorRef + ", lifeCycleControllingRouter=" + lifeCycleControllingRouter + ", descriptor=" + descriptor
                    + ", pluginRunner=" + pluginRunner + ", customConfigurationController=" + customConfigurationController
                    + ", registrationConfigurationControllers=" + registrationConfigurationControllers + "]";
        }
    }

    /**
     * A creator class for the actor {@link PluginStatusCallbackActor}
     * 
     * @author Pierre-Yves Cloux
     */
    public static class PluginStatusCallbackActorCreator implements Creator<PluginStatusCallbackActor> {
        private static final long serialVersionUID = 4075638451954038626L;
        private Map<Long, PluginRegistrationEntry> pluginByIds;

        public PluginStatusCallbackActorCreator(Map<Long, PluginRegistrationEntry> pluginByIds) {
            this.pluginByIds = pluginByIds;
        }

        @Override
        public PluginStatusCallbackActor create() throws Exception {
            return new PluginStatusCallbackActor(pluginByIds);
        }
    }

    /**
     * The actor which is notified from the status of the of the plugins
     * asynchronously.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class PluginStatusCallbackActor extends UntypedActor {
        private Map<Long, PluginRegistrationEntry> pluginByIds;

        public PluginStatusCallbackActor(Map<Long, PluginRegistrationEntry> pluginByIds) {
            this.pluginByIds = pluginByIds;
        }

        @Override
        public void onReceive(Object message) throws Exception {
            if (message instanceof CallbackLifeCycleMessage) {
                CallbackLifeCycleMessage callbackLifeCycleMessage = (CallbackLifeCycleMessage) message;
                switch (callbackLifeCycleMessage.getPluginStatus()) {
                case STARTED:
                    getPluginByIds().get(callbackLifeCycleMessage.getPluginConfigurationId()).setPluginStatus(PluginStatus.STARTED);
                    log.info(String.format("The plugin %d has reported as successfull start", callbackLifeCycleMessage.getPluginConfigurationId()));
                    break;
                case STOPPED:
                    getPluginByIds().get(callbackLifeCycleMessage.getPluginConfigurationId()).setPluginStatus(PluginStatus.STOPPED);
                    log.info(String.format("The plugin %d has reported a successfull stop", callbackLifeCycleMessage.getPluginConfigurationId()));
                    break;
                case START_FAILED:
                    getPluginByIds().get(callbackLifeCycleMessage.getPluginConfigurationId()).setPluginStatus(PluginStatus.STOPPED);
                    log.info(String.format("The plugin %d has reported an error at startup", callbackLifeCycleMessage.getPluginConfigurationId()));
                    break;
                default:
                    break;
                }
            } else {
                unhandled(message);
            }
        }

        private Map<Long, PluginRegistrationEntry> getPluginByIds() {
            return pluginByIds;
        }
    }

    /**
     * A creator class for a plugin runner lifecycle management actor. This
     * class is used to create some controlling actors.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class PluginLifeCycleControllingActorCreator implements Creator<PluginLifeCycleControllingActor> {
        private static final long serialVersionUID = -5423956994117942818L;
        private Long pluginConfigurationId;
        private IPluginRunner pluginRunner;
        private ActorRef pluginStatusCallbackActorRef;
        private II18nMessagesPlugin messagesPlugin;

        public PluginLifeCycleControllingActorCreator(Long pluginConfigurationId, IPluginRunner pluginRunner, ActorRef pluginStatusCallbackActorRef,
                II18nMessagesPlugin messagesPlugin) {
            this.pluginStatusCallbackActorRef = pluginStatusCallbackActorRef;
            this.pluginConfigurationId = pluginConfigurationId;
            this.pluginRunner = pluginRunner;
            this.messagesPlugin = messagesPlugin;
        }

        @Override
        public PluginLifeCycleControllingActor create() throws Exception {
            return new PluginLifeCycleControllingActor(pluginConfigurationId, pluginRunner, pluginStatusCallbackActorRef, getMessagesPlugin());
        }

        private II18nMessagesPlugin getMessagesPlugin() {
            return messagesPlugin;
        }
    }

    /**
     * This actor is to manage the lifecycle of a plugin.<br/>
     * It received messages such as:
     * <ul>
     * <li>START</li>
     * <li>STOP</li>
     * </ul>
     * 
     * @author Pierre-Yves Cloux
     */
    public static class PluginLifeCycleControllingActor extends UntypedActor {
        private Long pluginConfigurationId;
        private IPluginRunner pluginRunner;
        private ActorRef pluginStatusCallbackActorRef;
        private II18nMessagesPlugin messagesPlugin;

        public PluginLifeCycleControllingActor(Long pluginConfigurationId, IPluginRunner pluginRunner, ActorRef pluginStatusCallbackActorRef,
                II18nMessagesPlugin messagesPlugin) {
            super();
            this.messagesPlugin = messagesPlugin;
            this.pluginConfigurationId = pluginConfigurationId;
            this.pluginRunner = pluginRunner;
            this.pluginStatusCallbackActorRef = pluginStatusCallbackActorRef;
        }

        @Override
        public void onReceive(Object message) throws Exception {
            if (message instanceof LifeCycleMessage) {
                switch ((LifeCycleMessage) message) {
                case START:
                    try {
                        getPluginRunner().start();
                        getPluginStatusCallbackActorRef().tell(new CallbackLifeCycleMessage(PluginStatus.STARTED, getPluginConfigurationId()),
                                ActorRef.noSender());
                        PluginLog.saveStartPluginLog(getPluginConfigurationId(), getMessagesPlugin().get("plugin.success.start", getPluginConfigurationId()),
                                false);
                    } catch (Exception e) {
                        getPluginStatusCallbackActorRef().tell(new CallbackLifeCycleMessage(PluginStatus.START_FAILED, getPluginConfigurationId()),
                                ActorRef.noSender());
                        log.error(String.format("The plugin %d cannot be started, unexpected error", pluginConfigurationId), e);
                        PluginLog.saveStartPluginLog(pluginConfigurationId,
                                getMessagesPlugin().get("plugin.failed.start", pluginConfigurationId, Utilities.getExceptionAsString(e)), true);
                    }
                    break;
                case STOP:
                    try {
                        getPluginRunner().stop();
                        PluginLog.saveStopPluginLog(pluginConfigurationId, getMessagesPlugin().get("plugin.success.stop", pluginConfigurationId), false);
                    } catch (Exception e) {
                        String uuid = UUID.randomUUID().toString();
                        log.error(String.format("The plugin %d has reported an error while stopping, id of error is %s", pluginConfigurationId, uuid), e);
                        PluginLog.saveStopPluginLog(pluginConfigurationId, getMessagesPlugin().get("plugin.failed.stop", pluginConfigurationId, uuid), true);
                    }
                    getPluginStatusCallbackActorRef().tell(new CallbackLifeCycleMessage(PluginStatus.STOPPED, getPluginConfigurationId()), ActorRef.noSender());
                    break;
                }
            } else {
                unhandled(message);
            }
        }

        private Long getPluginConfigurationId() {
            return pluginConfigurationId;
        }

        private IPluginRunner getPluginRunner() {
            return pluginRunner;
        }

        private ActorRef getPluginStatusCallbackActorRef() {
            return pluginStatusCallbackActorRef;
        }

        private II18nMessagesPlugin getMessagesPlugin() {
            return messagesPlugin;
        }
    }

    /**
     * A creator class for the interface management actors (IN/OUT).<br/>
     * This one is used to instantiate the message processing actors.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class EventMessageProcessingActorCreator implements Creator<EventMessageProcessingActor> {
        private static final long serialVersionUID = 2033974676603900632L;
        private Long pluginConfigurationId;
        private IPluginRunner pluginRunner;
        private FlowType flowType;

        public EventMessageProcessingActorCreator(Long pluginConfigurationId, IPluginRunner pluginRunner, FlowType flowType) {
            this.pluginConfigurationId = pluginConfigurationId;
            this.pluginRunner = pluginRunner;
            this.flowType = flowType;
        }

        @Override
        public EventMessageProcessingActor create() throws Exception {
            return new EventMessageProcessingActor(pluginConfigurationId, pluginRunner, flowType);
        }
    }

    /**
     * An actor which is to be used to forward the {@link EventMessage}
     * asynchronously to the OUT interface of the plugin
     * 
     * @author Pierre-Yves Cloux
     */
    public static class EventMessageProcessingActor extends UntypedActor {
        private Long pluginConfigurationId;
        private IPluginRunner pluginRunner;
        private FlowType flowType;

        public EventMessageProcessingActor(Long pluginConfigurationId, IPluginRunner pluginRunner, FlowType flowType) {
            this.pluginConfigurationId = pluginConfigurationId;
            this.pluginRunner = pluginRunner;
            this.flowType = flowType;
        }

        @Override
        public void postStop() throws Exception {
            super.postStop();
            log.info(String.format("Stopping event message processing actor [%s] for plugin %d", getSelf().path().toString(), getPluginConfigurationId()));
        }

        @Override
        public void preStart() throws Exception {
            super.preStart();
            log.info(String.format("Starting event message processing actor [%s] for plugin %d", getSelf().path().toString(), getPluginConfigurationId()));
        }

        @Override
        public void onReceive(Object message) throws Exception {
            if (message != null && message instanceof EventMessage) {
                EventMessage eventMessage = (EventMessage) message;
                try {
                    log.info(String.format("[BEGIN] Transaction %s for event message processing actor for plugin %d with message type %s",
                            eventMessage.getTransactionId(), getPluginConfigurationId(), eventMessage.getMessageType().name()));
                    if (getFlowType().equals(FlowType.OUT)) {
                        getPluginRunner().handleOutProvisioningMessage(eventMessage);
                    } else {
                        getPluginRunner().handleInProvisioningMessage(eventMessage);
                    }
                    log.info(String.format("[SUCCESS] Transaction %s for event message processing actor for plugin %d with message type %s",
                            eventMessage.getTransactionId(), getPluginConfigurationId(), eventMessage.getMessageType().name()));
                } catch (PluginException e) {
                    /*
                     * If the message was not a RESYNC message, then attempt to
                     * recover (by sending a RESYNC command) Otherwise, throw an
                     * exception to log the issue.
                     */
                    if (!eventMessage.getMessageType().equals(EventMessage.MessageType.RESYNC)
                            && !eventMessage.getMessageType().equals(EventMessage.MessageType.CUSTOM)) {
                        // Notify the parent (= the message router associated
                        // with the plugin flow) with a resync message
                        getContext().parent().tell(eventMessage.getResyncProvisioningMessage(), getSelf());
                        log.warn(String.format("[FAILURE] Transaction %s for event message processing actor for plugin %d, attempt to recover with a resync",
                                eventMessage.getTransactionId(), getPluginConfigurationId()), e);

                    } else {
                        String errorMessage = String.format("[FAILURE] Transaction %s for event message processing actor for plugin %d failed",
                                eventMessage.getTransactionId(), getPluginConfigurationId());
                        PluginLog.saveOnEventHandlingPluginLog(eventMessage.getTransactionId(), getPluginConfigurationId(), true, eventMessage.getMessageType(),
                                errorMessage + "\nMessage was : " + eventMessage.toString(), eventMessage.getDataType(), eventMessage.getInternalId(),
                                eventMessage.getExternalId());
                        throw new PluginException(errorMessage, e);
                    }
                }
            } else {
                unhandled(message);
            }
        }

        private IPluginRunner getPluginRunner() {
            return pluginRunner;
        }

        private FlowType getFlowType() {
            return flowType;
        }

        private Long getPluginConfigurationId() {
            return pluginConfigurationId;
        }
    }
}
