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

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;

import play.Play;
import play.mvc.Controller;
import play.mvc.Http.RawBuffer;
import play.mvc.Http.Request;
import play.mvc.Result;

import com.avaje.ebean.Model;
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
import framework.services.api.commons.IApiConstants;
import framework.services.api.commons.IApiObject;
import framework.services.api.commons.JsonPropertyLink;
import framework.services.api.server.ApiLog;
import framework.utils.Utilities;

/**
 * The base class for any AbstractApiController
 * 
 * @author Pierre-Yves Cloux
 */
public class AbstractApiController extends Controller {
    public static final int ERROR_API_RESPONSE_CODE = HttpURLConnection.HTTP_INTERNAL_ERROR;
    public static final String ERROR_API_RESPONSE_NAME = "error";
    public static final int SUCCESS_API_RESPONSE_CODE = HttpURLConnection.HTTP_OK; // HTTP200
    public static final int SUCCESS_API_CREATED_RESPONSE_CODE = HttpURLConnection.HTTP_CREATED; // HTTP201
    public static final String SUCCESS_API_RESPONSE_NAME = "success";
    public static final String STANDARD_API_ROOT_URI = "/api/core";

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * Defines the URL patterns to be applied for some classes.
     */
    public static final Map<Class<?>, String> urlFormatsExceptions = Collections.synchronizedMap(new HashMap<Class<?>, String>() {
        private static final long serialVersionUID = -8100380399231080266L;

        {
            // this.put(Child.class, "/api/child/%d");
        }
    });

    protected static ObjectMapper mapper;
    private static final String authorizedHeaders = "Content-Type," + IApiConstants.APPLICATION_KEY_HEADER + "," + IApiConstants.SIGNATURE_HEADER + ","
            + IApiConstants.TIMESTAMP_HEADER;

    public AbstractApiController() {
    }

    /**
     * Return the mapper to be used for JSON serialization/deserialization
     * 
     * @return
     */
    private static ObjectMapper getMapper() {
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
                        Map<String, ApiUtils.SerializationEntry> entries = ApiUtils.getSerializationEntries(model.getClass(), model);
                        for (ApiUtils.SerializationEntry entry : entries.values()) {
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
     * Parse the request to extracted a Json payload.<br/>
     * <b>WARNING : </b>because of the signature mechanism the body must be
     * parse as Raw.<br/>
     * This means that the method must be annotated with:
     * 
     * <pre>
     * {@code
     * &#64;BodyParser.Of(BodyParser.Raw.class)
     * }
     * </pre>
     * 
     * @param request
     * @return
     * @throws JsonProcessingException
     * @throws IOException
     */
    public static JsonNode getRequestBodyAsJsonNode(Request request) throws JsonProcessingException, IOException {
        RawBuffer rawBuffer = request.body().asRaw();
        if (rawBuffer == null) {
            throw new RuntimeException("Body is null in the specified request");
        }
        byte[] bodyAsByteArray = rawBuffer.asBytes();
        return getMapper().readTree(bodyAsByteArray);
    }

    /**
     * Convert the specified Json node into an object of the specified class. <br/>
     * An exception is thrown if the unmarshalling fails
     * 
     * @param json
     * @param clazz
     * @return an object
     * @throws JsonProcessingException
     */
    public static <T> T getObjectFromJson(JsonNode json, Class<T> clazz) throws JsonProcessingException {
        return getMapper().treeToValue(json, clazz);
    }

    /**
     * Marshall the specified object.<br/>
     * The response is 200 (success). For GET
     * 
     * @param obj
     *            a java object which can be converted into JSON
     * @return a JSON structure
     */
    public static Result getJsonSuccessResponse(Object obj) {
        return getJsonResponse(obj, SUCCESS_API_RESPONSE_CODE);
    }

    /**
     * Marshall the specified object.<br/>
     * The response is 201 (success). For POST and PUT
     * 
     * @param obj
     *            a java object which can be converted into JSON
     * @return a JSON structure
     * @throws Exception
     */
    public static Result getJsonSuccessCreatedResponse(Object obj) throws Exception {
        return getJsonResponse(getIdFromLinkObject(obj, JsonPropertyLink.DEFAULT_ID_NAME), SUCCESS_API_CREATED_RESPONSE_CODE);
    }

    /**
     * Marshall the specified object.<br/>
     * The response is 201 (success). For POST and PUT Surcharge method if id
     * field is different cf JsonPorpertyLink class
     * 
     * @param obj
     *            a java object which can be converted into JSON
     * @param objId
     *            the name of id field in the class
     * @return a JSON structure
     * @throws Exception
     */
    public static Result getJsonSuccessCreatedResponse(Object obj, String objId) throws Exception {
        return getJsonResponse(getIdFromLinkObject(obj, objId), SUCCESS_API_CREATED_RESPONSE_CODE);
    }

    /**
     * Marshall the specified object.<br/>
     * 
     * @param error
     *            an error object
     * @return a JSON structure
     */
    public static Result getJsonErrorResponse(ApiError error) {
        return getJsonResponse(error, error.getCode());
    }

    /**
     * Marshall the specified object.
     * 
     * @param obj
     *            a java object which can be converted into JSON
     * @param code
     *            an HTTP code
     * @return a JSON structure
     */
    public static Result getJsonResponse(Object obj, int code) {
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
        response().setContentType("application/json");
        response().setHeader("Access-Control-Allow-Origin", "*");
        response().setHeader("Access-Control-Allow-Headers", authorizedHeaders);
        return status(code, w.toString());
    }

    /**
     * Return the URL pattern to be applied to the specified class.<br/>
     * By default a standard pattern is applied based on the name of the class. <br/>
     * The method checks if an exception to the standard pattern is not declared
     * before returning the URL pattern.
     * 
     * @return
     */
    private static String getUrlPatternFromObjectClass(Class<?> objectClass) {
        if (!urlFormatsExceptions.containsKey(objectClass)) {
            return Utilities.getPreferenceElseConfigurationValue(Play.application().configuration(), IFrameworkConstants.SWAGGER_API_BASEPATH_PREFERENCE,
                    "swagger.api.basepath")
                    + STANDARD_API_ROOT_URI
                    + "/"
                    + objectClass.getSimpleName().replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase()
                    + "/%s";
        }
        return urlFormatsExceptions.get(objectClass);
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
    private static Object getIdFromLinkObject(Object bean, String idFieldName) throws Exception {
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
                            throw new IllegalArgumentException("No field " + idFieldName + " on an element of a List of of type "
                                    + (o != null ? o.getClass() : o), e);
                        }
                        // Get the API name from bean
                        apiLink.name = model.getApiName();
                        // Generate URL link if id is not null
                        if (apiLink.id != null) {
                            apiLink.link = String.format(getUrlPatternFromObjectClass(o.getClass()), apiLink.id);
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
                    apiLink.link = String.format(getUrlPatternFromObjectClass(bean.getClass()), apiLink.id);
                    apiLink.link = apiLink.link.replace('_', '-');
                }
                return apiLink;
            } else {
                return null;
            }
        }
        return null;
    }
}
