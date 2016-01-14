package framework.services.ext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import framework.commons.DataType;
import framework.services.ext.XmlExtensionDescriptor.PluginConfigurationBlockDescriptor;
import framework.services.ext.XmlExtensionDescriptor.PluginDescriptor;
import framework.services.ext.XmlExtensionDescriptor.PluginRegistrationConfiguratorControllerDescriptor;
import framework.services.ext.XmlExtensionDescriptor.WidgetDescriptor;
import framework.services.ext.api.IExtensionDescriptor;

/**
 * A read only representation of the {@link XmlExtensionDescriptor} to prevent
 * any risk of modifications by another service of user
 * 
 * @author Pierre-Yves Cloux
 */
public class ReadOnlyExtensionDescriptor implements IExtensionDescriptor {
    private XmlExtensionDescriptor xmlExtensionDescriptor;
    private Map<String, IPluginDescriptor> pluginDescriptions;

    public ReadOnlyExtensionDescriptor(XmlExtensionDescriptor xmlExtensionDescriptor) {
        this.xmlExtensionDescriptor = xmlExtensionDescriptor;
    }

    @Override
    public String getName() {
        return getXmlExtensionDescriptor().getName();
    }

    @Override
    public List<String> getDeclaredStandaloneControllers() {
        return getXmlExtensionDescriptor().getStandaloneControllers();
    }

    @Override
    public Map<String, IPluginDescriptor> getDeclaredPlugins() {
        if (pluginDescriptions == null) {
            pluginDescriptions = Collections.synchronizedMap(new HashMap<String, IPluginDescriptor>());
            if (getXmlExtensionDescriptor().getPluginDescriptors() != null) {
                for (PluginDescriptor pluginDescriptor : getXmlExtensionDescriptor().getPluginDescriptors()) {
                    pluginDescriptions.put(pluginDescriptor.getIdentifier(), new ReadOnlyPluginDescriptor(pluginDescriptor));
                }
                pluginDescriptions = Collections.unmodifiableMap(pluginDescriptions);
            }
        }
        return pluginDescriptions;
    }

    @Override
    public boolean isMenuCustomized() {
        return getXmlExtensionDescriptor().isMenuCustomized();
    }

    XmlExtensionDescriptor getXmlExtensionDescriptor() {
        return xmlExtensionDescriptor;
    }

    public static class ReadOnlyPluginDescriptor implements IPluginDescriptor {
        private PluginDescriptor pluginDescriptor;
        private List<DataType> supportedDataTypes;
        private Map<String, IPluginConfigurationBlockDescriptor> configurationBlockDescriptors;
        private Map<DataType, String> registrationControllers;
        private Map<String, IWidgetDescriptor> widgetDesriptors;

        public ReadOnlyPluginDescriptor(PluginDescriptor pluginDescriptor) {
            super();
            this.pluginDescriptor = pluginDescriptor;
        }

        @Override
        public String getIdentifier() {
            return getPluginDescriptor().getIdentifier();
        }

        @Override
        public String getClazz() {
            return getPluginDescriptor().getClazz();
        }

        @Override
        public String getVersion() {
            return getPluginDescriptor().getVersion();
        }

        @Override
        public String getVendorUrl() {
            return getPluginDescriptor().getVendorUrl();
        }

        @Override
        public boolean multiInstanceAllowed() {
            return getPluginDescriptor().isMultiInstance();
        }

        @Override
        public String getName() {
            return getPluginDescriptor().getName();
        }

        @Override
        public String getDescription() {
            return getPluginDescriptor().getDescription();
        }

        @Override
        public boolean hasInMessageInterface() {
            return getPluginDescriptor().getEventInterface().isIn();
        }

        @Override
        public boolean hasOutMessageInterface() {
            return getPluginDescriptor().getEventInterface().isOut();
        }

        @Override
        public Map<String, IPluginConfigurationBlockDescriptor> getConfigurationBlockDescriptors() {
            if (configurationBlockDescriptors == null) {
                configurationBlockDescriptors = Collections.synchronizedMap(new HashMap<String, IPluginConfigurationBlockDescriptor>());
                if (getPluginDescriptor().getPluginConfigurationBlockDescriptors() != null) {
                    for (PluginConfigurationBlockDescriptor desc : getPluginDescriptor().getPluginConfigurationBlockDescriptors()) {
                        configurationBlockDescriptors.put(desc.getIdentifier(), new ReadOnlyPluginConfigurationBlockDescriptor(desc));
                    }
                    configurationBlockDescriptors = Collections.unmodifiableMap(configurationBlockDescriptors);
                }
            }
            return configurationBlockDescriptors;
        }

        @Override
        public List<DataType> getSupportedDataTypes() {
            if (supportedDataTypes == null) {
                supportedDataTypes = Collections.synchronizedList(new ArrayList<DataType>());
                if (getPluginDescriptor().getSupportedDataTypes() != null) {
                    for (String supportedDataType : getPluginDescriptor().getSupportedDataTypes()) {
                        supportedDataTypes.add(DataType.getDataType(supportedDataType));
                    }
                }
                supportedDataTypes = Collections.unmodifiableList(supportedDataTypes);
            }
            return supportedDataTypes;
        }

        @Override
        public String getCustomConfiguratorControllerClassName() {
            return getPluginDescriptor().getCustomConfigurationController();
        }

        @Override
        public Map<DataType, String> getRegistrationConfiguratorControllerClassNames() {
            if (registrationControllers == null) {
                registrationControllers = Collections.synchronizedMap(new HashMap<>());
                if (getPluginDescriptor().getRegistrationConfigurationControllerDescriptors() != null) {
                    for (PluginRegistrationConfiguratorControllerDescriptor desc : getPluginDescriptor().getRegistrationConfigurationControllerDescriptors()) {
                        DataType dataType = DataType.getDataType(desc.getDataType());
                        registrationControllers.put(dataType, desc.getControllerClass());
                    }
                    registrationControllers = Collections.unmodifiableMap(registrationControllers);
                }
            }
            return registrationControllers;
        }

        @Override
        public Map<String, IWidgetDescriptor> getWidgetDescriptors() {
            if (widgetDesriptors == null) {
                widgetDesriptors = Collections.synchronizedMap(new HashMap<>());
                if (getPluginDescriptor().getWidgetDescriptors() != null) {
                    for (WidgetDescriptor desc : getPluginDescriptor().getWidgetDescriptors()) {
                        widgetDesriptors.put(desc.getIdentifier(), new ReadOnlyWidgetDescriptor(desc));
                    }
                    widgetDesriptors = Collections.unmodifiableMap(widgetDesriptors);
                }
            }
            return widgetDesriptors;
        }

        private PluginDescriptor getPluginDescriptor() {
            return pluginDescriptor;
        }
    }

    public static class ReadOnlyPluginConfigurationBlockDescriptor implements IPluginConfigurationBlockDescriptor {
        private PluginConfigurationBlockDescriptor pluginConfigurationBlockDescriptor;

        public ReadOnlyPluginConfigurationBlockDescriptor(PluginConfigurationBlockDescriptor pluginConfigurationBlockDescriptor) {
            super();
            this.pluginConfigurationBlockDescriptor = pluginConfigurationBlockDescriptor;
        }

        @Override
        public int getVersion() {
            return getPluginConfigurationBlockDescriptor().getVersion();
        }

        @Override
        public String getIdentifier() {
            return getPluginConfigurationBlockDescriptor().getIdentifier();
        }

        @Override
        public String getName() {
            return getPluginConfigurationBlockDescriptor().getName();
        }

        @Override
        public String getDescription() {
            return getPluginConfigurationBlockDescriptor().getDescription();
        }

        @Override
        public ConfigurationBlockEditionType getEditionType() {
            return ConfigurationBlockEditionType.valueOf(getPluginConfigurationBlockDescriptor().getType());
        }

        @Override
        public byte[] getDefaultValue() {
            return (getPluginConfigurationBlockDescriptor().getDefaultValue()) != null ? getPluginConfigurationBlockDescriptor().getDefaultValue().getBytes()
                    : null;
        }

        private PluginConfigurationBlockDescriptor getPluginConfigurationBlockDescriptor() {
            return pluginConfigurationBlockDescriptor;
        }
    }

    public static class ReadOnlyWidgetDescriptor implements IWidgetDescriptor {
        private WidgetDescriptor widgetDescriptor;

        public ReadOnlyWidgetDescriptor(WidgetDescriptor widgetDescriptor) {
            this.widgetDescriptor = widgetDescriptor;
        }

        @Override
        public String getIdentifier() {
            return getWidgetDescriptor().getIdentifier();
        }

        @Override
        public String getName() {
            return getWidgetDescriptor().getName();
        }

        @Override
        public String getDescription() {
            return getWidgetDescriptor().getDescription();
        }

        @Override
        public String getControllerClassName() {
            return getWidgetDescriptor().getClazz();
        }

        private WidgetDescriptor getWidgetDescriptor() {
            return widgetDescriptor;
        }
    }
}
