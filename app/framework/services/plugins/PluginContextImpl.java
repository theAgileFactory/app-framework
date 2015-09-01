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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.tuple.Pair;

import com.avaje.ebean.Ebean;

import framework.commons.DataType;
import framework.commons.message.EventMessage;
import framework.services.ext.api.IExtensionDescriptor.IPluginConfigurationBlockDescriptor;
import framework.services.ext.api.IExtensionDescriptor.IPluginConfigurationBlockDescriptor.ConfigurationBlockEditionType;
import framework.services.ext.api.IExtensionDescriptor.IPluginDescriptor;
import framework.services.plugins.api.IPluginContext;
import framework.services.plugins.api.PluginException;
import framework.services.storage.ISharedStorageService;
import framework.utils.Utilities;
import models.framework_models.plugin.PluginConfiguration;
import models.framework_models.plugin.PluginConfigurationBlock;
import models.framework_models.plugin.PluginIdentificationLink;
import models.framework_models.plugin.PluginLog;
import models.framework_models.plugin.PluginRegistration;
import play.Logger;

/**
 * The object which provides the logging & monitoring features to the plugins
 * 
 * @author Pierre-Yves Cloux
 */
public class PluginContextImpl implements IPluginContext {
    private static Logger.ALogger log = Logger.of(PluginContextImpl.class);
    private static final String LOG_PREFIX_TEMPLATE = "[PLUGIN %s-%d] ";
    private Long pluginConfigurationId;
    private String pluginConfigurationName;
    private IPluginDescriptor pluginDescriptor;
    private String pluginPrefix;
    private IPluginManagerService pluginManagerService;
    private IEventBroadcastingService eventBroadcastingService;
    private ISharedStorageService sharedStorageService;

    public PluginContextImpl(PluginConfiguration pluginConfiguration, IPluginDescriptor pluginDescriptor, IPluginManagerService pluginManagerService,
            IEventBroadcastingService eventBroadcastingService, ISharedStorageService sharedStorageService) {
        this.pluginManagerService = pluginManagerService;
        this.eventBroadcastingService = eventBroadcastingService;
        this.sharedStorageService = sharedStorageService;
        this.pluginConfigurationId = pluginConfiguration.id;
        this.pluginConfigurationName = pluginConfiguration.name;
        this.pluginDescriptor = pluginDescriptor;
        this.pluginPrefix = String.format(LOG_PREFIX_TEMPLATE, pluginConfiguration.pluginDefinition.identifier, pluginConfigurationId);
    }

    @Override
    public IPluginDescriptor getPluginDescriptor() {
        return pluginDescriptor;
    }

    @Override
    public String getPluginConfigurationName() {
        return pluginConfigurationName;
    }

    @Override
    public void reportOnStartup(boolean isError, String logMessage) {
        PluginLog.saveStartPluginLog(getPluginConfigurationId(), logMessage, isError);
    }

    @Override
    public void reportOnStartup(boolean isError, String logMessage, Exception exception) {
        String exceptionStack = logException(exception);
        PluginLog.saveStartPluginLog(getPluginConfigurationId(), logMessage + "\nError trace:\n" + exceptionStack, isError);
    }

    @Override
    public void reportOnEventHandling(String transactionId, boolean isError, EventMessage eventMessage, String logMessage) {
        PluginLog.saveOnEventHandlingPluginLog(transactionId, getPluginConfigurationId(), isError, eventMessage.getMessageType(), logMessage,
                eventMessage.getDataType(), eventMessage.getInternalId(), eventMessage.getExternalId());
    }

    @Override
    public void reportOnEventHandling(String transactionId, boolean isError, EventMessage eventMessage, String logMessage, Exception exception) {
        String exceptionStack = logException(exception);
        PluginLog.saveOnEventHandlingPluginLog(transactionId, getPluginConfigurationId(), isError, eventMessage.getMessageType(),
                logMessage + "\nError trace:\n" + exceptionStack, eventMessage.getDataType(), eventMessage.getInternalId(), eventMessage.getExternalId());
    }

    @Override
    public void log(LogLevel level, String message) {
        if (level.equals(IPluginContext.LogLevel.ERROR)) {
            log.error(getPluginPrefix() + message);
        } else {
            if (level.equals(IPluginContext.LogLevel.INFO)) {
                log.info(getPluginPrefix() + message);
            } else {
                if (level.equals(IPluginContext.LogLevel.DEBUG)) {
                    if (log.isDebugEnabled()) {
                        log.debug(getPluginPrefix() + message);
                    }
                }
            }
        }
    }

    @Override
    public void log(LogLevel level, String message, Exception exception) {
        if (level.equals(IPluginContext.LogLevel.ERROR)) {
            log.error(getPluginPrefix() + message, exception);
        } else {
            if (level.equals(IPluginContext.LogLevel.INFO)) {
                log.info(getPluginPrefix() + message, exception);
            } else {
                if (level.equals(IPluginContext.LogLevel.DEBUG)) {
                    log.debug(getPluginPrefix() + message, exception);
                }
            }
        }
    }

    @Override
    public void setState(Object stateObject) throws PluginException {
        Ebean.beginTransaction();
        try {
            PluginConfiguration pluginConfiguration = PluginConfiguration.getAvailablePluginById(getPluginConfigurationId());
            if (pluginConfiguration == null) {
                throw new PluginException(String.format("Plugin configuration for %d not found", getPluginConfigurationId()));
            }
            pluginConfiguration.setState(stateObject);
            pluginConfiguration.save();
            Ebean.commitTransaction();
        } catch (Exception e) {
            log.error(String.format("Exception while attempting to store the state of the plugin %d", getPluginConfigurationId()), e);
            Ebean.rollbackTransaction();
        } finally {
            Ebean.endTransaction();
        }
    }

    @Override
    public Object getState() throws PluginException {
        PluginConfiguration pluginConfiguration = PluginConfiguration.getAvailablePluginById(getPluginConfigurationId());
        if (pluginConfiguration == null) {
            throw new PluginException(String.format("Plugin configuration for %d not found", getPluginConfigurationId()));
        }
        return pluginConfiguration.getState();
    }

    @Override
    public Pair<Boolean, byte[]> getConfiguration(IPluginConfigurationBlockDescriptor configurationBlockDescriptor, boolean defaultIfNull) {
        PluginConfigurationBlock pluginConfigurationBlock = PluginConfigurationBlock.getPluginConfigurationBlockFromIdentifier(getPluginConfigurationId(),
                configurationBlockDescriptor.getIdentifier());
        if (pluginConfigurationBlock == null) {
            if (defaultIfNull) {
                return Pair.of(false, configurationBlockDescriptor.getDefaultValue());
            }
            return null;
        }
        int savedVersion = 0;
        if (pluginConfigurationBlock.version != null) {
            savedVersion = pluginConfigurationBlock.version;
        }
        return Pair.of(savedVersion < configurationBlockDescriptor.getVersion(), pluginConfigurationBlock.configuration);
    }

    @Override
    public synchronized byte[] getConfigurationAndMergeWithDefault(IPluginConfigurationBlockDescriptor configurationBlockDescriptor) throws PluginException {
        if (!configurationBlockDescriptor.getEditionType().equals(ConfigurationBlockEditionType.PROPERTIES)) {
            throw new IllegalArgumentException("This function can only work with properties configuration block");
        }
        PluginConfigurationBlock pluginConfigurationBlock = PluginConfigurationBlock.getPluginConfigurationBlockFromIdentifier(getPluginConfigurationId(),
                configurationBlockDescriptor.getIdentifier());
        if (pluginConfigurationBlock == null) {
            return configurationBlockDescriptor.getDefaultValue();
        }
        int savedVersion = 0;
        if (pluginConfigurationBlock.version != null) {
            savedVersion = pluginConfigurationBlock.version;
        }
        if (savedVersion < configurationBlockDescriptor.getVersion()) {
            try {
                // This is not the right version, merge with the default
                // configuration and save
                Properties defaultConfigurationProperties = new Properties();
                defaultConfigurationProperties.load(new ByteArrayInputStream(configurationBlockDescriptor.getDefaultValue()));
                Properties actualConfiguration = new Properties();
                actualConfiguration.load(new ByteArrayInputStream(pluginConfigurationBlock.configuration));
                for (Object key : defaultConfigurationProperties.keySet()) {
                    if (!actualConfiguration.containsKey(key)) {
                        actualConfiguration.setProperty((String) key, defaultConfigurationProperties.getProperty((String) key));
                    }
                }
                Enumeration<Object> keys = actualConfiguration.keys();
                while (keys.hasMoreElements()) {
                    String key = (String) keys.nextElement();
                    if (!defaultConfigurationProperties.containsKey(key)) {
                        actualConfiguration.remove((String) key);
                    }
                }
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                actualConfiguration.store(buffer, "Merged configuration on startup " + (new Date()));
                pluginConfigurationBlock.version = configurationBlockDescriptor.getVersion();
                pluginConfigurationBlock.configuration = buffer.toByteArray();
                pluginConfigurationBlock.save();
            } catch (IOException e) {
                throw new PluginException("Error while accessing the configuration, inconsistent state, please reset the configuration to default", e);
            }
        }
        return pluginConfigurationBlock.configuration;
    }

    @Override
    public void setConfiguration(IPluginConfigurationBlockDescriptor configurationBlockDescriptor, byte[] configuration) throws PluginException {
        PluginConfigurationBlock pluginConfigurationBlock = PluginConfigurationBlock.getPluginConfigurationBlockFromIdentifier(getPluginConfigurationId(),
                configurationBlockDescriptor.getIdentifier());
        if (pluginConfigurationBlock == null) {
            pluginConfigurationBlock = new PluginConfigurationBlock();
            pluginConfigurationBlock.identifier = configurationBlockDescriptor.getIdentifier();
            pluginConfigurationBlock.pluginConfiguration = PluginConfiguration.getAvailablePluginById(getPluginConfigurationId());
        }
        pluginConfigurationBlock.version = configurationBlockDescriptor.getVersion();
        pluginConfigurationBlock.configurationType = configurationBlockDescriptor.getEditionType().name();
        pluginConfigurationBlock.configuration = configuration;
        pluginConfigurationBlock.save();
    }

    @Override
    public boolean isRegistered(DataType dataType, Long objectId) {
        return PluginRegistration.isRegistered(getPluginConfigurationId(), dataType, objectId);
    }

    @Override
    public void removeRegistration(DataType dataType, Long objectId) {
        PluginRegistration pluginRegistration = PluginRegistration.getPluginRegistration(getPluginConfigurationId(), dataType, objectId);
        if (pluginRegistration != null) {
            pluginRegistration.delete();
        }
    }

    @Override
    public byte[] getRegistrationConfiguration(DataType dataType, Long objectId) {
        PluginRegistration pluginRegistration = PluginRegistration.getPluginRegistration(getPluginConfigurationId(), dataType, objectId);
        if (pluginRegistration == null) {
            return null;
        }
        return pluginRegistration.configurationProperties;
    }

    @Override
    public void setRegistrationConfiguration(DataType dataType, Long objectId, byte[] configuration) {
        PluginRegistration pluginRegistration = PluginRegistration.getPluginRegistration(getPluginConfigurationId(), dataType, objectId);
        if (pluginRegistration == null) {
            pluginRegistration = new PluginRegistration();
            pluginRegistration.dataType = dataType.getDataTypeClassName();
            pluginRegistration.internalId = objectId;
            pluginRegistration.pluginConfiguration = PluginConfiguration.getPluginById(getPluginConfigurationId());
        }
        pluginRegistration.configurationProperties = configuration;
        pluginRegistration.save();
    }

    @Override
    public List<Pair<Long, String>> getChildrenIdOfLink(Long internalId, String externalId, String linkType, String childLinkType) {
        List<PluginIdentificationLink> childrenLink = PluginIdentificationLink.getChildren(getPluginConfigurationId(), internalId, externalId, linkType,
                childLinkType);
        ArrayList<Pair<Long, String>> ids = new ArrayList<Pair<Long, String>>();
        for (PluginIdentificationLink child : childrenLink) {
            ids.add(Pair.of(child.internalId, child.externalId));
        }
        return ids;
    }

    @Override
    public List<Long> getMultipleInternalId(String externalId, String linkType) {
        List<PluginIdentificationLink> links = PluginIdentificationLink.getLinksForPluginAndExternalId(getPluginConfigurationId(), externalId, linkType);
        ArrayList<Long> ids = new ArrayList<Long>();
        for (PluginIdentificationLink link : links) {
            ids.add(link.internalId);
        }
        return ids;
    }

    @Override
    public List<String> getMultipleExternalId(Long internalId, String linkType) {
        List<PluginIdentificationLink> links = PluginIdentificationLink.getLinksForPluginAndInternalId(getPluginConfigurationId(), internalId, linkType);
        ArrayList<String> ids = new ArrayList<String>();
        for (PluginIdentificationLink link : links) {
            ids.add(link.externalId);
        }
        return ids;
    }

    @Override
    public Long getUniqueInternalId(String externalId, String linkType) {
        List<PluginIdentificationLink> links = PluginIdentificationLink.getLinksForPluginAndExternalId(getPluginConfigurationId(), externalId, linkType);
        if (links.size() == 0) {
            return null;
        }
        if (links.size() != 1) {
            throw new RuntimeException(String.format("More than one link has been found for the external id %s", externalId));
        }
        return links.get(0).internalId;
    }

    @Override
    public String getUniqueExternalId(Long internalId, String linkType) {
        List<PluginIdentificationLink> links = PluginIdentificationLink.getLinksForPluginAndInternalId(getPluginConfigurationId(), internalId, linkType);
        if (links.size() == 0) {
            return null;
        }
        if (links.size() != 1) {
            throw new RuntimeException(String.format("More than one link has been found for the internal id %d", internalId));
        }
        return links.get(0).externalId;
    }

    @Override
    public Long getUniqueInternalIdWithParent(String externalId, String linkType, Long parentInternalId, String parentExternalId, String parentLinkType) {
        PluginIdentificationLink parentLink = PluginIdentificationLink.getUniqueLink(getPluginConfigurationId(), parentInternalId, parentExternalId,
                parentLinkType);
        if (parentLink == null) {
            throw new RuntimeException(String.format("Impossible to find the parent link"));
        }
        List<PluginIdentificationLink> links = PluginIdentificationLink.getLinksForPluginAndExternalIdAndParentId(getPluginConfigurationId(), externalId,
                parentLink.id, linkType);
        if (links.size() == 0) {
            return null;
        }
        if (links.size() != 1) {
            throw new RuntimeException(String.format("More than one link has been found for the external id %s", externalId));
        }
        return links.get(0).internalId;
    }

    @Override
    public void createOneToOneLink(Long internalId, String externalId, String linkType) throws PluginException {
        PluginIdentificationLink existingLink = PluginIdentificationLink.getUniqueOneToOneLink(getPluginConfigurationId(), internalId, externalId, linkType);
        if (existingLink != null) {
            throw new PluginException(String.format("A link already exists between %d and %s for the linkType %s for the plugin %d", internalId, externalId,
                    linkType, getPluginConfigurationId()));
        } else {
            createLink(internalId, externalId, linkType);
        }
    }

    @Override
    public void createLink(Long internalId, String externalId, String linkType) throws PluginException {
        PluginIdentificationLink newLink = new PluginIdentificationLink();
        newLink.pluginConfiguration = PluginConfiguration.getPluginById(getPluginConfigurationId());
        newLink.internalId = internalId;
        newLink.externalId = externalId;
        newLink.linkType = linkType;
        newLink.save();
    }

    @Override
    public void createLink(Long internalId, String externalId, String linkType, Long parentInternalId, String parentExternalId, String parentLinkType)
            throws PluginException {
        PluginIdentificationLink parentLink = PluginIdentificationLink.getUniqueLink(getPluginConfigurationId(), parentInternalId, parentExternalId,
                parentLinkType);
        PluginIdentificationLink newLink = new PluginIdentificationLink();
        newLink.pluginConfiguration = PluginConfiguration.getPluginById(getPluginConfigurationId());
        newLink.internalId = internalId;
        newLink.externalId = externalId;
        newLink.linkType = linkType;
        newLink.parent = parentLink;
        newLink.save();
    }

    @Override
    public boolean deleteOneToOneLink(Long internalId, String externalId, String linkType) {
        PluginIdentificationLink existingLink = PluginIdentificationLink.getUniqueOneToOneLink(getPluginConfigurationId(), internalId, externalId, linkType);
        if (existingLink == null) {
            return false;
        }
        for (PluginIdentificationLink child : existingLink.children) {
            child.delete();
        }
        existingLink.delete();
        return true;
    }

    @Override
    public void deleteLink(Long internalId, String externalId, String linkType) {
        PluginIdentificationLink parentLink = PluginIdentificationLink.getUniqueLink(getPluginConfigurationId(), internalId, externalId, linkType);
        for (PluginIdentificationLink child : parentLink.children) {
            child.delete();
        }
        parentLink.delete();
    }

    @Override
    public void flushAllLinks() {
        PluginIdentificationLink.flushLinksForPlugin(getPluginConfigurationId());
    }

    @Override
    public void killMe() {
        getPluginManagerService().stopPlugin(getPluginConfigurationId());
        reportOnEventHandling("-9", true, null, "Plugin kill was requested");
    }

    public Long getPluginConfigurationId() {
        return pluginConfigurationId;
    }

    @Override
    public void postOutMessage(EventMessage eventMessage) {
        getEventBroadcastingService().postOutMessage(eventMessage);
    }

    @Override
    public PropertiesConfiguration getPropertiesConfigurationFromByteArray(byte[] rawConfigurationBlock) throws PluginException {
        if (rawConfigurationBlock == null || rawConfigurationBlock.length == 0) {
            return new PropertiesConfiguration();
        }
        PropertiesConfiguration properties = new PropertiesConfiguration();
        try {
            properties.load(new ByteArrayInputStream(rawConfigurationBlock));
        } catch (ConfigurationException e) {
            throw new PluginException("Unable to parse the configuration", e);
        }
        return properties;
    }

    @Override
    public InputStream getFileFromSharedStorage(String filePath) throws IOException {
        return getSharedStorageService().getFileAsStream(filePath);
    }

    @Override
    public OutputStream writeFileInSharedStorage(String filePath, boolean overwrite) throws IOException {
        return getSharedStorageService().writeFile(filePath, overwrite);
    }

    @Override
    public void deleteFileInSharedStorage(String filePath) throws IOException {
        getSharedStorageService().deleteFile(filePath);
    }

    @Override
    public String[] getFileListInSharedStorage(String directoryPath) throws IOException {
        return getSharedStorageService().getFileList(directoryPath);
    }

    @Override
    public void renameFileInSharedStorage(String sourceFilePath, String targetFilePath) throws IOException {
        getSharedStorageService().rename(sourceFilePath, targetFilePath);
    }

    @Override
    public void moveFileInSharedStorage(String sourceFilePath, String targetFolderPath) throws IOException {
        getSharedStorageService().move(sourceFilePath, targetFolderPath);
    }

    private String getPluginPrefix() {
        return pluginPrefix;
    }

    /**
     * Create a String from an Exception
     * 
     * @param e
     *            an Exception
     * @return a String
     */
    private static String logException(Exception e) {
        return Utilities.getExceptionAsString(e);
    }

    private ISharedStorageService getSharedStorageService() {
        return sharedStorageService;
    }

    private IPluginManagerService getPluginManagerService() {
        return pluginManagerService;
    }

    private IEventBroadcastingService getEventBroadcastingService() {
        return eventBroadcastingService;
    }
}
