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
package models.framework_models.common;

import com.avaje.ebean.Ebean;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.custom_attribute.ICustomAttributeManagerService;
import framework.services.script.IScriptService;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import framework.utils.Msg;
import framework.utils.PropertiesLoader;
import play.Logger;
import play.Play;
import play.api.data.Field;
import play.cache.CacheApi;
import play.inject.Injector;
import play.twirl.api.Html;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Custom attribute to be used to compute a value from a nashorn script. It can be added to any object
 * in the system. Type of the value : {@link ScriptEngine}<br/>
 * Here are the properties supported by this attribute:
 * <ul>
 * <li>script : the script must implement the "getValue(objectType, objectId)" method and can make
 * use of the {@link ScriptUtils} object.</li>
 * </ul>
 *
 * TODO: Voir si on peut mettre des couleurs
 *
 * @author Guillaume Petit
 */
public class ScriptCustomAttributeValue implements ICustomAttributeValue {

    private static final String SCRIPT_ENGINE_CACHE_PREFIX = "ScriptCustomAttributeValue.scriptEngine.";
    private static Logger.ALogger log = Logger.of(ScriptCustomAttributeValue.class);

    private Object value;

    private CustomAttributeDefinition customAttributeDefinition;

    /**
     * Default constructor.
     */
    public ScriptCustomAttributeValue() {
    }

    /**
     * Configure the script engine with the custom attribute definition and execute the getValue method to create
     * the corresponding ScriptCustomAttributeValue.
     *
     * @param objectType the object type
     * @param filter a filter (not used here)
     * @param objectId the object id
     * @param customAttributeDefinition the custom attribute definition
     *
     * @return the custom attribute value
     */
    public static ScriptCustomAttributeValue getOrCreateCustomAttributeValueFromObjectReference(
            Class<?> objectType, String filter, Long objectId,CustomAttributeDefinition customAttributeDefinition) {

        String configuration = customAttributeDefinition.getProperties().getProperty("script");
        Object value = null;
        try {
            // Get and execute script from custom attribute definition
            final Invocable invocable = getInvocableScriptEngine(customAttributeDefinition.uuid, configuration);

            // Invoke script method
            value = invocable.invokeFunction("getValue", objectType.getName(), objectId);
        } catch (ScriptException e) {
            log.error("Error while executing script for custom attribute " + Msg.get(customAttributeDefinition.name), e);
        } catch (NoSuchMethodException e) {
            log.error("Unable to find method \"getValue(objectType, objectId)\" in configured script for custom attribute " + Msg.get(customAttributeDefinition.name), e);
        }

        ScriptCustomAttributeValue scriptCustomAttributeValue = new ScriptCustomAttributeValue();
        scriptCustomAttributeValue.value = value;
        scriptCustomAttributeValue.customAttributeDefinition = customAttributeDefinition;

        return scriptCustomAttributeValue;
    }

    /**
     * Validates the script configuration by executing it
     *
     * @param configuration the custom attribute configuration to validate
     *
     * @throws ScriptException if the script contains an error
     */
    public static void validateScriptConfiguration(String configuration) throws ScriptException, IOException {
        Properties properties = PropertiesLoader.loadProperties(new ByteArrayInputStream(configuration.getBytes()), "UTF-8");
        String script = properties.getProperty("script");
        if (script == null) {
            throw new IllegalArgumentException("There is no \"script\" property in custom attribute configuration");
        }
        getInvocableScriptEngine(null, script);
    }

    /**
     * Get script engine from cache or create a new one
     *
     * @param uuid the script engine uuid
     * @param script the script configuration
     *
     * @return an invocable instance of ScriptEngine
     * @throws ScriptException when script configuration is invalid
     */
    public static Invocable getInvocableScriptEngine(String uuid, String script) throws ScriptException {

        Injector injector = Play.application().injector();
        CacheApi cacheApi = injector.instanceOf(CacheApi.class);

        // Try to get script engine from cache
        ScriptEngine scriptEngine = uuid == null ? null : cacheApi.get(SCRIPT_ENGINE_CACHE_PREFIX + uuid);

        if (scriptEngine == null) {
            scriptEngine = injector.instanceOf(IScriptService.class).getEngine(uuid);
            scriptEngine.getContext().setAttribute("scriptUtils", new ScriptUtils(injector.instanceOf(ICustomAttributeManagerService.class)), ScriptContext.ENGINE_SCOPE);
            scriptEngine.eval(script);
            cacheApi.set(SCRIPT_ENGINE_CACHE_PREFIX + uuid, scriptEngine);
        }
        return (Invocable) scriptEngine;
    }

    public static void flushCache(String uuid) {
        Play.application().injector().instanceOf(CacheApi.class).remove(SCRIPT_ENGINE_CACHE_PREFIX + uuid);
    }

    public static void cloneInDB(Class<?> objectType, Long oldObjectId, Long newObjectId, CustomAttributeDefinition customAttributeDefinition) {}

    @Override
    public String getLinkedObjectClassName() {
        return null;
    }

    @Override
    public Long getLinkedObjectId() {
        return null;
    }

    @Override
    public boolean isNotReadFromDb() {
        return true;
    }

    @Override
    public void defaults() {
    }

    @Override
    public void performSave(IUserSessionManagerPlugin userSessionManagerPlugin, IAttachmentManagerPlugin attachmentManagerPlugin, String fieldName) {
    }

    @Override
    public CustomAttributeDefinition getDefinition() {
        return this.customAttributeDefinition;
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.SCRIPT;
    }

    @Override
    public Object getValueAsObject() {
        return this.value;
    }

    @Override
    public void setValueAsObject(Object newValue) {
    }

    @Override
    public String print() {
        return null;
    }

    @Override
    public boolean parse(II18nMessagesPlugin i18nMessagesPlugin, String text) {
        return false;
    }

    @Override
    public boolean parseFile(ICustomAttributeManagerService customAttributeManagerService) {
        return false;
    }

    @Override
    public boolean hasError() {
        return false;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public void resetError() {
    }

    @Override
    public Html renderFormField(II18nMessagesPlugin i18nMessagesPlugin, IUserSessionManagerPlugin userSessionManagerPlugin, IImplementationDefinedObjectService implementationDefinedObjectService, Field field, boolean displayDescription) {
        String description = "";
        if (displayDescription) {
            description = customAttributeDefinition.description;
        }
        return views.html.framework_views.parts.read_only_field.render(field, customAttributeDefinition.name, description);
    }

    @Override
    public Html renderDisplay(II18nMessagesPlugin i18nMessagesPlugin) {
        return views.html.framework_views.parts.formats.display_object.render(getValueAsObject(), true);
    }

    @Override
    public Html renderDisplayNoDescription(II18nMessagesPlugin i18nMessagesPlugin) {
        return renderDisplay(i18nMessagesPlugin);
    }

    @Override
    public Object getAsSerializableValue() {
        return this.value;
    }

    static public class ScriptUtils {

        private ICustomAttributeManagerService customAttributeManagerService;

        public ScriptUtils(ICustomAttributeManagerService customAttributeManagerService) {
            this.customAttributeManagerService = customAttributeManagerService;
        }

        public Object getObjectFromId(String objectType, long objectId) throws ScriptUtilsException {
            try {
                Class<?> dataTypeClass = Class.forName(objectType);
                return Ebean.getReference(dataTypeClass, objectId);
            } catch (ClassNotFoundException e) {
                throw new ScriptUtilsException("Unable to find object " + objectType + " with id " + objectId);
            }
        }

        public Object getCustomAttributeValue(String objectType, long objectId, String uuid) throws ScriptUtilsException {
            try {
                Class<?> dataTypeClass = Class.forName(objectType);
                ICustomAttributeValue customAttributeValue = CustomAttributeDefinition.getCustomAttributeValue(uuid, dataTypeClass, objectId);
                if (customAttributeValue != null) {
                    return customAttributeValue.getValueAsObject();
                } else {
                    throw new ScriptUtilsException("Unable to find custom attribute " + uuid + " for object " + objectType);
                }
            } catch (ClassNotFoundException e) {
                throw new ScriptUtilsException("Unable to find object " + objectType + " with id " + objectId);
            }
        }

        public String getMessage(String key) {
            return Msg.get(key);
        }
    }

    static private class ScriptUtilsException extends Exception {

        public ScriptUtilsException(String message) {
            super(message);
        }
    }
}