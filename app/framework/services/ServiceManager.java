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
package framework.services;

import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import play.GlobalSettings;
import play.Logger;
import play.Play;

import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

/**
 * The class that holds the Spring application context and give access to
 * various kind of plugins for the MAF suite.<br/>
 * This context is loaded with the properties taken from the play configuration
 * file (application.conf). <u>NB</u>: Only the properties which starts with
 * MAF_PROPERTIES_PREFIX are loaded in the Spring application context.<br/>
 * <u>WARNING</u>: use only properties of this format
 * MAF_PROPERTIES_PREFIX.property_name_without_any_dot<br/>
 * Such kind of property maf.something.something else will not work.
 * 
 * @author Pierre-Yves Cloux
 */
public class ServiceManager {
    private static Logger.ALogger log = Logger.of(ServiceManager.class);

    private static ServiceManager instance;

    private ApplicationContext ctx;

    /**
     * Default constructor.
     * 
     * @param profiles
     *            a profile name used in the component.xml file
     */
    private ServiceManager(String... profiles) {
        // Initialize the Spring application context
        log.info("[START] Loading the beans definitions");
        Thread.currentThread().setContextClassLoader(Play.application().classloader());
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setProperties(getMafPropertiesFromConfile());
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
        context.getEnvironment().setActiveProfiles(profiles);
        context.addBeanFactoryPostProcessor(configurer);
        context.setConfigLocation("components.xml");
        context.refresh();
        ctx = context;
        log.info("[END] Loading the beans definitions");
    }

    /**
     * Return an instance of the service manager (which is itself a singleton).
     */
    public static ServiceManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("Service manager is not initialized, cannot use it yet");
        }
        return instance;
    }

    /**
     * Method to be called in the onStart method of the {@link GlobalSettings}
     * implementation in the application.
     * 
     * @param profiles
     *            a profile name used in the component.xml file
     */
    public static void init(String... profiles) {
        if (instance == null) {
            instance = new ServiceManager(profiles);
        }
    }

    /**
     * Method to be called in the onStop method of the {@link GlobalSettings}
     * implementation of the application.
     */
    public static void shutdown() {
        try {
            ((ConfigurableApplicationContext) getInstance().getCtx()).close();
            instance = null;
        } catch (Exception e) {
            log.debug("Exception while shutting down the service manager", e);
        }
    }

    /**
     * Return true if the service associated with the specified Id is registered
     * into the ServiceManager.
     * 
     * @param service
     *            a service Id
     * @return a boolean
     */
    public static boolean isServiceRegistered(String service) {
        return getInstance().getCtx().containsBean(service);
    }

    /**
     * Return the service associated with the specified Id.
     * 
     * @param service
     *            a service Id
     * @param clazz
     *            the class for the service
     * @return a service instance
     * @param <T>
     *            the service class
     */
    public static <T> T getService(String service, Class<T> clazz) {
        return getInstance().getCtx().getBean(service, clazz);
    }

    /**
     * Load the properties from the play configuration file.<br/>
     * 
     * @return a Properties object
     */
    private Properties getMafPropertiesFromConfile() {
        Properties properties = new Properties();
        for (Entry<String, ConfigValue> entry : Play.application().configuration().entrySet()) {
            if (entry.getValue().valueType().equals(ConfigValueType.STRING) || entry.getValue().valueType().equals(ConfigValueType.BOOLEAN)
                    || entry.getValue().valueType().equals(ConfigValueType.NUMBER)) {
                properties.put(entry.getKey(), entry.getValue().unwrapped().toString());
            }
        }
        return properties;
    }

    /**
     * Returns all the services with a certain type registered in the service
     * registry.
     * 
     * @param beanType
     *            a class
     * @return a list of service names
     */
    public static String[] getServicesForType(Class<?> beanType) {
        return ServiceManager.getInstance().getCtx().getBeanNamesForType(beanType);
    }

    /**
     * The internal Spring application context.
     */
    private ApplicationContext getCtx() {
        return ctx;
    }
}
