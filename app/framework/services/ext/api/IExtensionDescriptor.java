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
package framework.services.ext.api;

import java.util.List;
import java.util.Map;

import framework.commons.DataType;

/**
 * The public interface for the extensions descriptors
 * 
 * @author Pierre-Yves Cloux
 */
public interface IExtensionDescriptor {
    /**
     * The name of the extension
     * 
     * @return
     */
    public String getName();

    /**
     * The list of standalone controllers which are associated with this
     * extension.<br/>
     * Return a list of Java class names.
     * 
     * @return
     */
    public List<String> getDeclaredStandaloneControllers();

    /**
     * The list of plugins which are associated with this extension.<br/>
     * Return a list of plugin description (identifier and java class).
     * 
     * @return
     */
    public Map<String, IPluginDescriptor> getDeclaredPlugins();

    /**
     * True if the extension is associated with a menu customization
     * 
     * @return
     */
    public boolean isMenuCustomized();

    /**
     * An interface to read a plugin descriptor
     * 
     * @author Pierre-Yves Cloux
     */
    public static interface IPluginDescriptor {
        /**
         * Return the unique id of the plugin definition (common to all the
         * plugin instances)
         */
        public String getIdentifier();

        /**
         * The java class name which implements this plugin
         */
        public String getClazz();

        /**
         * The version of the plugin
         */
        public String getVersion();

        /**
         * The URL to the web site of the vendor of the plugin
         */
        public String getVendorUrl();

        /**
         * True if the plugin can be multi-instanciated (multiple configurations
         * for the same plugin)
         * 
         * @return a boolean
         */
        public boolean multiInstanceAllowed();

        /**
         * The i18n key for the name of the plugin
         */
        public String getName();

        /**
         * The i18n key for the description of the plugin
         */
        public String getDescription();

        /**
         * True if the plugin is able to listen for "external" messages sent to
         * BizDock by a third party system
         * 
         * @return a boolean
         */
        public boolean hasInMessageInterface();

        /**
         * True if the plugin is able to listen for "internal" messages sent by
         * BizDock core or other plugins
         * 
         * @return a boolean
         */
        public boolean hasOutMessageInterface();

        /**
         * Return a map of configuration blocks required by this plugin indexed
         * by their unique identifier
         * 
         * @return a map of configuration blocks
         */
        public Map<String, IPluginConfigurationBlockDescriptor> getConfigurationBlockDescriptors();

        /**
         * Return a list of supported data types.<br/>
         * Meaning that this plugin is able to deal with the listed data types
         */
        public List<DataType> getSupportedDataTypes();

        /**
         * Return the class name for an custom configurator controller
         */
        public String getCustomConfiguratorControllerClassName();

        /**
         * Return a map associating a {@link DataType} with a configurator
         * controller class name.<br/>
         * This one is to be used to "register" a named BizDock object (of the
         * specified data type) with the plugin.
         */
        public Map<DataType, String> getRegistrationConfiguratorControllerClassNames();

        /**
         * Return the controllers which are managing widgets.<br/>
         * This method returns a Map[widgetIdentifier, descriptor]
         */
        public Map<String, IWidgetDescriptor> getWidgetControllerClassNames();
    }

    /**
     * A meta-description of a configuration block for a plugin
     * 
     * @author Pierre-Yves Cloux
     */
    public interface IPluginConfigurationBlockDescriptor {
        /**
         * The allowed types of configuration blocks
         * 
         * @author Pierre-Yves Cloux
         */
        public enum ConfigurationBlockEditionType {
            XML, PROPERTIES, JAVASCRIPT, VELOCITY, FILE
        }

        /**
         * The version of this configuration descriptor.<br/>
         * This one will be stored in the database.<br/>
         * Each time you modify the configuration descriptor with a risk of
         * incompatibility, you must increment this version number.<br/>
         * This version number should be checked when reading the configuration.
         */
        public int getVersion();

        /**
         * The unique identifier of the configuration block.<br/>
         * You can use it to extract the configuration block from the database
         * 
         * @return a String
         */
        public String getIdentifier();

        /**
         * The i18n key for name of the configuration block
         */
        public String getName();

        /**
         * The i18n key for the description of the configuration block
         */
        public String getDescription();

        /**
         * Return the type of the configuration block.<br/>
         * This defines the way the block will be edited through the
         * configuration interface
         */
        public ConfigurationBlockEditionType getEditionType();

        /**
         * Return the default value (when the plugin is initialized for the
         * first time or manually reseted)
         */
        public byte[] getDefaultValue();
    }

    /**
     * A meta-description of a widget plugin
     * 
     * @author Pierre-Yves Cloux
     */
    public interface IWidgetDescriptor {
        /**
         * The unique identifier of a widget.<br/>
         * 
         * @return a String
         */
        public String getIdentifier();

        /**
         * The i18n key for name of the widget
         */
        public String getName();

        /**
         * The i18n key for the description of the widget
         */
        public String getDescription();

        /**
         * The name of the controller managing this widget
         */
        public String getControllerClassName();
    }
}