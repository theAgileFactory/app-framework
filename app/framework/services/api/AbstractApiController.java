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

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import framework.services.api.commons.IApiConstants;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

/**
 * The base class for any AbstractApiController
 * 
 * @author Pierre-Yves Cloux
 */
public class AbstractApiController extends Controller implements IApiConstants {
    @Inject
    private IApiControllerUtilsService apiControllerUtilsService;

    public AbstractApiController() {
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
    public JsonNode getRequestBodyAsJsonNode(Request request) throws JsonProcessingException, IOException {
        return getApiControllerUtilsService().getRequestBodyAsJsonNode(request);
    }

    /**
     * Convert the specified Json node into an object of the specified class.
     * <br/>
     * An exception is thrown if the unmarshalling fails
     * 
     * @param json
     * @param clazz
     * @return an object
     * @throws JsonProcessingException
     */
    public <T> T getObjectFromJson(JsonNode json, Class<T> clazz) throws JsonProcessingException {
        return getApiControllerUtilsService().getObjectFromJson(json, clazz);
    }

    /**
     * Marshall the specified object.<br/>
     * The response is 200 (success). For GET
     * 
     * @param obj
     *            a java object which can be converted into JSON
     * @return a JSON structure
     */
    public Result getJsonSuccessResponse(Object obj) {
        return getApiControllerUtilsService().getJsonSuccessResponse(obj, response());
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
    public Result getJsonSuccessCreatedResponse(Object obj) throws Exception {
        return getApiControllerUtilsService().getJsonSuccessCreatedResponse(obj, response());
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
    public Result getJsonSuccessCreatedResponse(Object obj, String objId) throws Exception {
        return getApiControllerUtilsService().getJsonSuccessCreatedResponse(obj, objId, response());
    }

    /**
     * Marshall the specified object.<br/>
     * 
     * @param error
     *            an error object
     * @return a JSON structure
     */
    public Result getJsonErrorResponse(ApiError error) {
        return getApiControllerUtilsService().getJsonErrorResponse(error, response());
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
    public Result getJsonResponse(Object obj, int code) {
        return getApiControllerUtilsService().getJsonResponse(obj, code, response());
    }

    private IApiControllerUtilsService getApiControllerUtilsService() {
        return apiControllerUtilsService;
    }
}
