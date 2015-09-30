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
package framework.services.api;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.eclipse.jdt.core.dom.Modifier;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.wordnik.swagger.core.util.JsonUtil;

import framework.commons.IFrameworkConstants;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.api.commons.IApiConstants;
import framework.services.api.commons.IApiObject;
import framework.services.api.commons.JsonPropertyLink;
import framework.services.api.server.ApiLog;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http.RawBuffer;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Result;

/**
 * An utility class which gathers some methods used by the API management
 * features
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class ApiControllerUtilsServiceImpl implements IApiControllerUtilsService, IApiConstants {
    private static Logger.ALogger log = Logger.of(ApiControllerUtilsServiceImpl.class);

    /**
     * Defines the URL patterns to be applied for some classes.
     */
    public static final Map<Class<?>, String> urlFormatsExceptions = Collections.synchronizedMap(new HashMap<Class<?>, String>() {
        private static final long serialVersionUID = -8100380399231080266L;

        {
            // this.put(Child.class, "/api/child/%d");
        }
    });

    private static final String authorizedHeaders = "Content-Type," + IApiConstants.APPLICATION_KEY_HEADER + "," + IApiConstants.SIGNATURE_HEADER + ","
            + IApiConstants.TIMESTAMP_HEADER;

    private ObjectMapper mapper;
    private IPreferenceManagerPlugin preferenceManagerPlugin;

    @Inject
    public ApiControllerUtilsServiceImpl(ApplicationLifecycle lifecycle, IPreferenceManagerPlugin preferenceManagerPlugin) {
        log.info("SERVICE>>> ApiControllerUtilsServiceImpl starting...");
        this.preferenceManagerPlugin = preferenceManagerPlugin;
        getMapper();
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> ApiControllerUtilsServiceImpl stopping...");
            log.info("SERVICE>>> ApiControllerUtilsServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> ApiControllerUtilsServiceImpl started");
    }

    @Override
    public JsonNode getRequestBodyAsJsonNode(Request request) throws JsonProcessingException, IOException {
        RawBuffer rawBuffer = request.body().asRaw();
        if (rawBuffer == null) {
            throw new RuntimeException("Body is null in the specified request");
        }
        byte[] bodyAsByteArray = rawBuffer.asBytes();
        return getMapper().readTree(bodyAsByteArray);
    }

    @Override
    public <T> T getObjectFromJson(JsonNode json, Class<T> clazz) throws JsonProcessingException {
        return getMapper().treeToValue(json, clazz);
    }

    @Override
    public Result getJsonSuccessResponse(Object obj, Response response) {
        return getJsonResponse(obj, SUCCESS_API_RESPONSE_CODE, response);
    }

    @Override
    public Result getJsonSuccessCreatedResponse(Object obj, Response response) throws Exception {
        return getJsonResponse(getIdFromLinkObject(obj, JsonPropertyLink.DEFAULT_ID_NAME), SUCCESS_API_CREATED_RESPONSE_CODE, response);
    }

    @Override
    public Result getJsonSuccessCreatedResponse(Object obj, String objId, Response response) throws Exception {
        return getJsonResponse(getIdFromLinkObject(obj, objId), SUCCESS_API_CREATED_RESPONSE_CODE, response);
    }

    @Override
    public Result getJsonErrorResponse(ApiError error, Response response) {
        return getJsonResponse(error, error.getCode(), response);
    }

    @Override
    public Result getJsonResponse(Object obj, int code, Response response) {
        StringWriter w = new StringWriter();
        try {
            getMapper().writeValue(w, obj);
        } catch (Exception e) {
            String message = "Error while marshalling the application response";
            ApiLog.log.error(message, e);
            try {
                getMapper().writeValue(w, new ApiError(message));
            } catch (Exception exp) {
                throw new RuntimeException("Unexpected error while mashalling an ApiError message");
            }
            code = ERROR_API_RESPONSE_CODE;
        }
        response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", authorizedHeaders);
        return Controller.status(code, w.toString());
    }

    @Override
    public String convertAsJsonString(Object obj) {
        StringWriter w = new StringWriter();
        try {
            getMapper().writeValue(w, obj);
        } catch (Exception e) {
            log.error("Cannot serialize the specified object", e);
        }
        return w.toString();
    }

    /**
     * Return the mapper to be used for JSON serialization/deserialization
     *
     * @return an object mapper
     */
    private ObjectMapper getMapper() {
        if (mapper == null) {
            mapper = JsonUtil.mapper();
            mapper.setDateFormat(new SimpleDateFormat(DATE_FORMAT));
            mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            SimpleModule simpleModule = new SimpleModule("SimpleModule", new Version(1, 0, 0, "", "", ""));
            simpleModule.addSerializer(Model.class, new JsonSerializer<Model>() {
                @Override
                public void serialize(Model model, JsonGenerator jsonGen, SerializerProvider serProv) throws IOException, JsonProcessingException {
                    if (model != null) {
                        jsonGen.writeStartObject();
                        Map<String, ApiControllerUtilsServiceImpl.SerializationEntry> entries = getSerializationEntries(model.getClass(), model);
                        for (ApiControllerUtilsServiceImpl.SerializationEntry entry : entries.values()) {
                            if (entry.isLink) {
                                try {
                                    jsonGen.writeObjectField(entry.propertyName, getIdFromLinkObject(entry.propertyValue, entry.linkfield));
                                } catch (Exception e) {
                                    throw new IOException("Unable to serialize the Linked property " + entry.propertyName, e);
                                }
                            } else {
                                jsonGen.writeObjectField(entry.propertyName, entry.propertyValue);
                            }
                        }
                        jsonGen.writeEndObject();
                    }
                }
            });
            mapper.registerModule(simpleModule);
        }
        return mapper;
    }

    /**
     * The specified field has been annoted with {@link JsonPropertyLink}.<br/>
     * The value to be returned is not the value of the field but the id
     * associated with the child object (or a list of id)
     * 
     * @param bean
     *            the bean
     * @param idFieldName
     *            the name of field annoted with {@link JsonPropertyLink}
     * @return an object to be serialized (either a String or a list of String)
     * @throws Exception
     */
    private Object getIdFromLinkObject(Object bean, String idFieldName) throws Exception {
        if (bean != null) {
            if (bean instanceof List) {
                List<ApiLink> apiLinks = new ArrayList<ApiLink>(); // new object
                                                                   // ApiLink
                List<?> beanAsList = (List<?>) bean;

                for (Object o : beanAsList) {
                    IApiObject model = (IApiObject) o;
                    // if object deleted return null
                    if (model.getApiDeleted() == false) {
                        ApiLink apiLink = new ApiLink();
                        // Get the id field from bean
                        try {
                            apiLink.id = FieldUtils.readField(o, idFieldName);
                        } catch (Exception e) {
                            throw new IllegalArgumentException(
                                    "No field " + idFieldName + " on an element of a List of of type " + (o != null ? o.getClass() : o), e);
                        }
                        // Get the API name from bean
                        apiLink.name = model.getApiName();
                        // Generate URL link if id is not null
                        if (apiLink.id != null) {
                            apiLink.link = String.format(getUrlPatternFromObjectClass(o.getClass(), getPreferenceManagerPlugin()), apiLink.id);
                            apiLink.link = apiLink.link.replace('_', '-');
                        }

                        apiLinks.add(apiLink); // add the occurence to the list
                    } else {
                        return null;
                    }
                }
                return apiLinks;
            }

            // if object deleted return null
            IApiObject model = (IApiObject) bean;
            if (model.getApiDeleted() == false) {
                ApiLink apiLink = new ApiLink();
                // Get the id field from bean
                try {
                    apiLink.id = FieldUtils.readField(bean, idFieldName);
                } catch (Exception e) {
                    throw new IllegalArgumentException("No field " + idFieldName + " on object of type " + bean.getClass(), e);
                }

                // Get the API name form bean
                apiLink.name = model.getApiName();

                // Generate URL link if id is not null
                if (apiLink.id != null) {
                    apiLink.link = String.format(getUrlPatternFromObjectClass(bean.getClass(), getPreferenceManagerPlugin()), apiLink.id);
                    apiLink.link = apiLink.link.replace('_', '-');
                }
                return apiLink;
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * Analyze the structure of an object to identify the values which can be
     * serialized
     * 
     * @param beanClass
     *            the class of the object
     * @param bean
     *            the value of the object (if null only the structure of the
     *            classes is collected)
     * @return a map of description objects
     */
    public static Map<String, SerializationEntry> getSerializationEntries(Class<?> beanClass, Object bean) {
        Map<String, SerializationEntry> entries = new HashMap<String, SerializationEntry>();
        try {
            // Look for annotated getters
            for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(beanClass).getPropertyDescriptors()) {
                if (propertyDescriptor.getReadMethod().isAnnotationPresent(JsonProperty.class)
                        || propertyDescriptor.getReadMethod().isAnnotationPresent(JsonPropertyLink.class)) {
                    SerializationEntry entry = new SerializationEntry();
                    entry.propertyName = propertyDescriptor.getReadMethod().getName();
                    if (propertyDescriptor.getReadMethod().isAnnotationPresent(JsonProperty.class)
                            && !StringUtils.isBlank(propertyDescriptor.getReadMethod().getAnnotation(JsonProperty.class).value())) {
                        entry.propertyName = propertyDescriptor.getReadMethod().getAnnotation(JsonProperty.class).value();
                    }
                    if (propertyDescriptor.getReadMethod().isAnnotationPresent(JsonPropertyLink.class)) {
                        if (!StringUtils.isBlank(propertyDescriptor.getReadMethod().getAnnotation(JsonPropertyLink.class).value())) {
                            entry.propertyName = propertyDescriptor.getReadMethod().getAnnotation(JsonPropertyLink.class).value();
                        }
                        entry.isLink = true;
                        entry.linkfield = propertyDescriptor.getReadMethod().getAnnotation(JsonPropertyLink.class).linkField();
                    }
                    entry.propertyType = propertyDescriptor.getPropertyType();
                    if (bean != null) {
                        entry.propertyValue = PropertyUtils.getProperty(bean, propertyDescriptor.getName());
                    }
                    entries.put(entry.propertyName, entry);
                }
            }
            // Look for annotated public properties
            for (final Field field : beanClass.getDeclaredFields()) {
                if (Modifier.isPublic(field.getModifiers())
                        && (field.isAnnotationPresent(JsonProperty.class) || field.isAnnotationPresent(JsonPropertyLink.class))) {
                    SerializationEntry entry = new SerializationEntry();
                    entry.propertyName = field.getName();
                    if (field.isAnnotationPresent(JsonProperty.class) && !StringUtils.isBlank(field.getAnnotation(JsonProperty.class).value())) {
                        entry.propertyName = field.getAnnotation(JsonProperty.class).value();
                    }
                    if (field.isAnnotationPresent(JsonPropertyLink.class)) {
                        if (!StringUtils.isBlank(field.getAnnotation(JsonPropertyLink.class).value())) {
                            entry.propertyName = field.getAnnotation(JsonPropertyLink.class).value();
                        }
                        entry.isLink = true;
                        entry.linkfield = field.getAnnotation(JsonPropertyLink.class).linkField();
                    }
                    entry.propertyType = field.getType();
                    if (bean != null) {
                        entry.propertyValue = field.get(bean);
                    }
                    entries.put(entry.propertyName, entry);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse the object " + beanClass, e);
        }
        return entries;
    }

    /**
     * Return the URL pattern to be applied to the specified class.<br/>
     * By default a standard pattern is applied based on the name of the class.
     * <br/>
     * The method checks if an exception to the standard pattern is not declared
     * before returning the URL pattern.
     * 
     * @return
     */
    private String getUrlPatternFromObjectClass(Class<?> objectClass, IPreferenceManagerPlugin preferenceManagerPlugin) {
        if (!urlFormatsExceptions.containsKey(objectClass)) {
            return preferenceManagerPlugin.getPreferenceElseConfigurationValue(IFrameworkConstants.SWAGGER_API_BASEPATH_PREFERENCE, "swagger.api.basepath")
                    + STANDARD_API_ROOT_URI + "/" + objectClass.getSimpleName().replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase() + "/%s";
        }
        return urlFormatsExceptions.get(objectClass);
    }

    private IPreferenceManagerPlugin getPreferenceManagerPlugin() {
        return preferenceManagerPlugin;
    }

    /**
     * An object which describes an entry to be serialized
     *
     * @author Pierre-Yves Cloux
     *
     */
    public static class SerializationEntry {
        public String propertyName;
        public Class<?> propertyType;
        public Object propertyValue;
        public boolean isLink;
        public String linkfield;

        @Override
        public String toString() {
            return "SerializationEntry [propertyName=" + propertyName + ", propertyType=" + propertyType + "]";
        }
    }
}
