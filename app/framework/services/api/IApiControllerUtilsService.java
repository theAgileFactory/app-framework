package framework.services.api;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Result;

public interface IApiControllerUtilsService {
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
    public JsonNode getRequestBodyAsJsonNode(Request request) throws JsonProcessingException, IOException;

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
    public <T> T getObjectFromJson(JsonNode json, Class<T> clazz) throws JsonProcessingException;

    /**
     * Marshall the specified object.<br/>
     * The response is 200 (success). For GET
     * 
     * @param obj
     *            a java object which can be converted into JSON
     * @param response
     *            an HTTP response object
     * @return a JSON structure
     */
    public Result getJsonSuccessResponse(Object obj, Response response);

    /**
     * Marshall the specified object.<br/>
     * The response is 201 (success). For POST and PUT
     * 
     * @param obj
     *            a java object which can be converted into JSON
     * @param response
     *            an HTTP response object
     * @return a JSON structure
     * @throws Exception
     */
    public Result getJsonSuccessCreatedResponse(Object obj, Response response) throws Exception;

    /**
     * Marshall the specified object.<br/>
     * The response is 201 (success). For POST and PUT Surcharge method if id
     * field is different cf JsonPorpertyLink class
     * 
     * @param obj
     *            a java object which can be converted into JSON
     * @param objId
     *            the name of id field in the class
     * @param response
     *            an HTTP response object
     * @return a JSON structure
     * @throws Exception
     */
    public Result getJsonSuccessCreatedResponse(Object obj, String objId, Response response) throws Exception;

    /**
     * Marshall the specified object.<br/>
     * 
     * @param error
     *            an error object
     * @param response
     *            an HTTP response object
     * @return a JSON structure
     */
    public Result getJsonErrorResponse(ApiError error, Response response);

    /**
     * Marshall the specified object.
     * 
     * @param obj
     *            a java object which can be converted into JSON
     * @param code
     *            an HTTP code
     * @param response
     *            an HTTP response object
     * @return a JSON structure
     */
    public Result getJsonResponse(Object obj, int code, Response response);

    /**
     * Convert the specified object as a Json String
     * 
     * @param obj
     *            a serializable object
     */
    public String convertAsJsonString(Object obj);
}