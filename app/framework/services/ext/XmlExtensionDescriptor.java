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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import framework.services.plugins.api.IPluginRunner;

/**
 * The class which is to be used to parse an extension descriptor.<br/>
 * Here are the descriptor attributes:
 * <ul>
 * <li>name : the name of the extension</li>
 * <li>controllers : the class name of the controllers for this extension</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@XmlRootElement(name = "extension-descriptor")
class XmlExtensionDescriptor {
    private String name;
    private List<String> controllers;
    private List<PluginDescriptor> pluginDescriptors;
    private List<I18nMessage> i18nMessages;
    private MenuCustomizationDescriptor menuCustomizationDescriptor;

    public XmlExtensionDescriptor() {
    }

    @XmlElement(name = "name", required = true, nillable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "controller", nillable = false)
    public List<String> getControllers() {
        return controllers;
    }

    public void setControllers(List<String> controllers) {
        this.controllers = controllers;
    }

    @XmlElement(name = "plugin", nillable = false)
    public List<PluginDescriptor> getPluginDescriptors() {
        return pluginDescriptors;
    }

    public void setPluginDescriptors(List<PluginDescriptor> pluginDescriptors) {
        this.pluginDescriptors = pluginDescriptors;
    }

    @XmlElement(name = "i18n-messages", nillable = false)
    public List<I18nMessage> getI18nMessages() {
        return i18nMessages;
    }

    public void setI18nMessages(List<I18nMessage> i18nMessages) {
        this.i18nMessages = i18nMessages;
    }

    @XmlElement(name = "menu-customization", nillable = false)
    public MenuCustomizationDescriptor getMenuCustomizationDescriptor() {
        return menuCustomizationDescriptor;
    }

    public void setMenuCustomizationDescriptor(MenuCustomizationDescriptor menuCustomizationDescriptor) {
        this.menuCustomizationDescriptor = menuCustomizationDescriptor;
    }

    public List<String> getStandaloneControllers() {
        return Collections.unmodifiableList(getControllers() != null ? getControllers() : new ArrayList<String>());
    }

    public boolean isMenuCustomized() {
        return getMenuCustomizationDescriptor() != null;
    }

    /**
     * A sub-structure which represents some changes to be performed on the
     * application menu
     * 
     * @author Pierre-Yves Cloux
     */
    public static class MenuCustomizationDescriptor {
        private List<String> menusToRemove;
        private List<MenuItemDescriptor> menusToAdd;

        public MenuCustomizationDescriptor() {
            super();
        }

        @XmlElement(name = "remove-menu-uuid", nillable = false)
        public List<String> getMenusToRemove() {
            return menusToRemove;
        }

        public void setMenusToRemove(List<String> menusToRemove) {
            this.menusToRemove = menusToRemove;
        }

        @XmlElement(name = "add-menu", nillable = false)
        public List<MenuItemDescriptor> getMenusToAdd() {
            return menusToAdd;
        }

        public void setMenusToAdd(List<MenuItemDescriptor> menusToAdd) {
            this.menusToAdd = menusToAdd;
        }
    }

    /**
     * A menu descriptor to be added to the main menu.<br/>
     * <ul>
     * <li>addAfterUuid : the uuid of the menu item after which this menu item
     * muste be added</li>
     * <li>addBeforeUuid : the uuid of the menu item before which this menu item
     * muste be added</li>
     * <li>label : a label displayed to the end user</li>
     * <li>url : the URL for this menu</li>
     * <li>permissions : a list of permissions</li>
     * </ul>
     * <b>WARNING : if no reference uuid (after/before) is provided, the new
     * menu is assumed to be a top level menu</b>
     * 
     * @author Pierre-Yves Cloux
     */
    public static class MenuItemDescriptor {
        private String uuid;
        private String toUuid;
        private String addAfterUuid;
        private String addBeforeUuid;
        private String label;
        private String url;
        private List<String> permissions;

        public MenuItemDescriptor() {
            super();
        }

        @XmlAttribute(name = "uuid", required = true)
        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        @XmlElement(name = "label", required = true, nillable = false)
        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        @XmlElement(name = "after-uuid", nillable = false)
        public String getAddAfterUuid() {
            return addAfterUuid;
        }

        public void setAddAfterUuid(String addAfterUuid) {
            this.addAfterUuid = addAfterUuid;
        }

        @XmlElement(name = "before-uuid", nillable = false)
        public String getAddBeforeUuid() {
            return addBeforeUuid;
        }

        public void setAddBeforeUuid(String addBeforeUuid) {
            this.addBeforeUuid = addBeforeUuid;
        }

        @XmlElement(name = "to-uuid", nillable = false)
        public String getToUuid() {
            return toUuid;
        }

        public void setToUuid(String toUuid) {
            this.toUuid = toUuid;
        }

        @XmlElement(name = "url", required = true, nillable = false)
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @XmlElement(name = "permission", nillable = false)
        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }
    }

    /**
     * A descriptor structure which identifies a plugin.<br/>
     * <ul>
     * <li>identifier : the unique ID for a plugin</li>
     * <li>clazz : the class which is implementing the {@link IPluginRunner}
     * interface</li>
     * <li>isAvailable : true if the plugin will be "visible" to the users</li>
     * </ul>
     * 
     * @author Pierre-Yves Cloux
     */
    public static class PluginDescriptor {
        private String identifier;
        private String clazz;
        private String name;
        private String description;
        private String version;
        private String vendorUrl;
        private boolean multiInstance;
        private PluginEventInterfaceDescriptor eventInterface;
        private List<PluginConfigurationBlockDescriptor> pluginConfigurationBlockDescriptors;
        private List<String> supportedDataTypes;
        private String customConfigurationController;
        private List<PluginRegistrationConfiguratorControllerDescriptor> registrationConfigurationControllerDescriptors;

        public PluginDescriptor() {
            super();
        }

        @XmlElement(name = "identifier", required = true, nillable = false)
        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        @XmlElement(name = "clazz", required = true, nillable = false)
        public String getClazz() {
            return clazz;
        }

        public void setClazz(String clazz) {
            this.clazz = clazz;
        }

        @XmlElement(name = "name", required = true, nillable = false)
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlElement(name = "description", required = true, nillable = false)
        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @XmlElement(name = "version", required = true, nillable = false)
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @XmlElement(name = "vendor-url", required = true, nillable = false)
        public String getVendorUrl() {
            return vendorUrl;
        }

        public void setVendorUrl(String vendorUrl) {
            this.vendorUrl = vendorUrl;
        }

        @XmlElement(name = "multi-instance", required = true, nillable = false)
        public boolean isMultiInstance() {
            return multiInstance;
        }

        public void setMultiInstance(boolean multiInstance) {
            this.multiInstance = multiInstance;
        }

        @XmlElement(name = "event-interface", required = true, nillable = false)
        public PluginEventInterfaceDescriptor getEventInterface() {
            return eventInterface;
        }

        public void setEventInterface(PluginEventInterfaceDescriptor eventInterface) {
            this.eventInterface = eventInterface;
        }

        @XmlElement(name = "configuration-block", required = false, nillable = false)
        public List<PluginConfigurationBlockDescriptor> getPluginConfigurationBlockDescriptors() {
            return pluginConfigurationBlockDescriptors;
        }

        public void setPluginConfigurationBlockDescriptors(List<PluginConfigurationBlockDescriptor> pluginConfigurationBlockDescriptors) {
            this.pluginConfigurationBlockDescriptors = pluginConfigurationBlockDescriptors;
        }

        @XmlElement(name = "supported-data-type", required = false, nillable = false)
        public List<String> getSupportedDataTypes() {
            return supportedDataTypes;
        }

        public void setSupportedDataTypes(List<String> supportedDataTypes) {
            this.supportedDataTypes = supportedDataTypes;
        }

        @XmlElement(name = "custom-configurator", required = false, nillable = false)
        public String getCustomConfigurationController() {
            return customConfigurationController;
        }

        public void setCustomConfigurationController(String customConfigurationController) {
            this.customConfigurationController = customConfigurationController;
        }

        @XmlElement(name = "registration-configurator", required = false, nillable = false)
        public List<PluginRegistrationConfiguratorControllerDescriptor> getRegistrationConfigurationControllerDescriptors() {
            return registrationConfigurationControllerDescriptors;
        }

        public void setRegistrationConfigurationControllerDescriptors(
                List<PluginRegistrationConfiguratorControllerDescriptor> registrationConfiguratorControllerDescriptors) {
            this.registrationConfigurationControllerDescriptors = registrationConfiguratorControllerDescriptors;
        }
    }

    /**
     * The data structure which describes the event interface supported by a
     * plugin:
     * <ul>
     * <li>IN : message coming from outside BizDock (into BizDock)</li>
     * <li>OUT : message sent by BizDock event management to the plugin (and
     * usually to the outside world)</li>
     * </ul>
     * 
     * @author Pierre-Yves Cloux
     */
    public static class PluginEventInterfaceDescriptor {
        private boolean in;
        private boolean out;

        public PluginEventInterfaceDescriptor() {
            super();
        }

        @XmlAttribute(name = "in", required = true)
        public boolean isIn() {
            return in;
        }

        public void setIn(boolean in) {
            this.in = in;
        }

        @XmlAttribute(name = "out", required = true)
        public boolean isOut() {
            return out;
        }

        public void setOut(boolean out) {
            this.out = out;
        }
    }

    /**
     * The data structure for describing a plugin "registration" configuration
     * controller
     * 
     * @author Pierre-Yves Cloux
     */
    public static class PluginRegistrationConfiguratorControllerDescriptor {
        private String dataType;
        private String controllerClass;

        public PluginRegistrationConfiguratorControllerDescriptor() {
            super();
        }

        @XmlAttribute(name = "data-type", required = true)
        public String getDataType() {
            return dataType;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        @XmlValue
        public String getControllerClass() {
            return controllerClass;
        }

        public void setControllerClass(String controllerClass) {
            this.controllerClass = controllerClass;
        }
    }

    /**
     * The descriptor of a standard configuration item. Various data types can
     * be supported : PROPERTIES, XML, JAVASCRIPT, etc.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class PluginConfigurationBlockDescriptor {
        private String identifier;
        private String type;
        private String name;
        private String description;
        private int version;
        private String defaultValue;

        public PluginConfigurationBlockDescriptor() {
            super();
        }

        @XmlAttribute(name = "identifier", required = true)
        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        @XmlAttribute(name = "type", required = true)
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @XmlElement(name = "name", required = true, nillable = false)
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlElement(name = "description", required = true, nillable = false)
        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @XmlElement(name = "version", required = true, nillable = false)
        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        @XmlElement(name = "default", required = true, nillable = false)
        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    /**
     * A class which handle i18n messages for the extension.<br/>
     * The messages are to be added to the Bizdock i18n library.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class I18nMessage {
        private String language;
        private String messages;

        public I18nMessage() {
            super();
        }

        @XmlAttribute(name = "language", required = true)
        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        @XmlValue
        public String getMessages() {
            return messages;
        }

        public void setMessages(String messages) {
            this.messages = messages;
        }
    }
}
