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
package framework.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import framework.services.storage.IAttachmentManagerPlugin;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

/**
 * A class which handle the AJAX calls from a picker (either multivalue or
 * single value picker). Here is the JSON structure to be used:
 * 
 * <pre>
 * {
 *      requestType: RequestType.CONFIG;
 *      request: {
 *      }
 * }
 * </pre>
 * 
 * @param <T>
 *            the type of the object which is handled by the Picker
 * @author Pierre-Yves Cloux
 */
public class PickerHandler<T> {
    private Handle<T> handle;
    private Class<T> clazz;
    private Map<PickerHandler.Parameters, String> parameters = Collections.synchronizedMap(new HashMap<PickerHandler.Parameters, String>());

    /*
     * Suffix of the parameters which are I18n keys.<br/> The value must be
     * retrieved when the parameter is used.
     */
    private static final String I18N_PARAMETER_SUFFIX = "_I18N";

    /**
     * The type of the request sent from the picker to the back-end controller
     * with a POST request.
     * 
     * @author Pierre-Yves Cloux
     */
    public enum RequestType {
        CONFIG, INIT, SEARCH;
    }

    /**
     * The parameters which can be provided to the picker.
     * 
     * @author Pierre-Yves Cloux
     */
    public enum Parameters {
        SEARCH_ENABLED, AJAX_WAIT_IMAGE_URL, MODAL_TITLE_I18N, MODAL_OK_BUTTON_LABEL_I18N, MODAL_CANCEL_BUTTON_LABEL_I18N, AJAX_WAIT_MESSAGE_I18N, AJAX_COMMUNICATION_FAILED_I18N, NO_VALUE_FOUND_I18N, ALREADY_SELECTED_VALUE_I18N;
    }

    public PickerHandler(IAttachmentManagerPlugin attachmentManagerPlugin, Class<T> clazz, PickerHandler.Handle<T> handle) {
        this.handle = handle;
        this.clazz = clazz;
        if (handle.isPickerMulti()) {
            getParameters().put(Parameters.MODAL_TITLE_I18N, "picker.title.multi");
        } else {
            getParameters().put(Parameters.MODAL_TITLE_I18N, "picker.title.single");
        }
        getParameters().put(Parameters.AJAX_WAIT_IMAGE_URL, attachmentManagerPlugin.getAjaxWaitImageUrl());
        getParameters().put(Parameters.MODAL_OK_BUTTON_LABEL_I18N, "button.save");
        getParameters().put(Parameters.MODAL_CANCEL_BUTTON_LABEL_I18N, "button.close");
        getParameters().put(Parameters.AJAX_COMMUNICATION_FAILED_I18N, "picker.ajax.failure.message");
        getParameters().put(Parameters.AJAX_WAIT_MESSAGE_I18N, "picker.ajax.wait.message");
        getParameters().put(Parameters.NO_VALUE_FOUND_I18N, "picker.novalue.message");
        getParameters().put(Parameters.ALREADY_SELECTED_VALUE_I18N, "picker.already.selected.message");
        getParameters().put(Parameters.SEARCH_ENABLED, "false");
    }

    public Result handle(Request request) {
        JsonNode json = request.body().asJson();
        String requestType = json.get("requestType").asText();
        switch (RequestType.valueOf(requestType)) {
        case CONFIG:
            return getConfigResponse();
        case INIT:
            return getInitialValueResponse(json);
        case SEARCH:
            return getSearchResponse(json);
        }
        return Controller.badRequest();
    }

    /**
     * Return the response to the initial configuration
     * 
     * @return a JSON response
     */
    public Result getConfigResponse() {
        ObjectNode result = Json.newObject();
        Map<PickerHandler.Parameters, String> configParameters = getHandle().config(getParameters());
        if (configParameters != null) {
            for (Parameters configParameterName : configParameters.keySet()) {
                if (configParameterName.name().endsWith(I18N_PARAMETER_SUFFIX)) {
                    result.put(configParameterName.name(), Msg.get(configParameters.get(configParameterName)));
                } else {
                    result.put(configParameterName.name(), configParameters.get(configParameterName));
                }
            }
        }
        return Controller.ok(result);
    }

    /**
     * Return the response to a request for the initial values to be displayed
     * 
     * @return a JSON response
     */
    public Result getInitialValueResponse(JsonNode json) {
        ObjectNode result = Json.newObject();

        // Get the values passed as parameters
        ArrayList<T> values = new ArrayList<T>();
        JsonNode valuesNode = json.get("values");
        if (valuesNode != null) {
            for (JsonNode node : valuesNode) {
                values.add(convertNodeToT(node));
            }
        }

        // Get context parameters
        HashMap<String, String> context = extractContextFromJsonRequest(json);

        // Create the return structure
        ISelectableValueHolderCollection<T> valueHolders = getHandle().getInitialValueHolders(values, context);
        valueHolderCollectionToJson(result, valueHolders);

        return Controller.ok(result);
    }

    private T convertNodeToT(JsonNode node) {
        if (getClazz().equals(Long.class)) {
            return getClazz().cast(node.asLong());
        }
        if (getClazz().equals(String.class)) {
            return getClazz().cast(node.asText());
        }
        throw new IllegalArgumentException("PickerHandler can only support Strings and Longs");
    }

    /**
     * Return the response to a request for the initial values to be displayed
     * 
     * @return a JSON response
     */
    public Result getSearchResponse(JsonNode json) {
        ObjectNode result = Json.newObject();

        // Get context parameters
        HashMap<String, String> context = extractContextFromJsonRequest(json);

        ISelectableValueHolderCollection<T> valueHolders = getHandle().getFoundValueHolders(json.get("searchString").asText(), context);
        valueHolderCollectionToJson(result, valueHolders);
        return Controller.ok(result);
    }

    /**
     * The abstract class to be implemented by the developer to specify what
     * should do the PickerHandler
     * 
     * @param <T>
     *            the type of the value handled by the picker
     * @author Pierre-Yves Cloux
     */
    public static abstract class Handle<T> {
        /**
         * Returns the configuration to be used by the picker JavaScript
         * component.<br/>
         * This method can be overridden with alternate values than the default
         * ones
         * 
         * @return a Map
         */
        public Map<PickerHandler.Parameters, String> config(Map<PickerHandler.Parameters, String> defaultParameters) {
            return defaultParameters;
        }

        /**
         * Default a single value picker
         */
        public boolean isPickerMulti() {
            return false;
        }

        /**
         * Return the initial value holder(s) for the specified values.<br/>
         * <b>WARNING</b>: the initial value holders collection MUST contain at
         * least all the currently selected values
         */
        public abstract ISelectableValueHolderCollection<T> getInitialValueHolders(List<T> values, Map<String, String> context);

        /**
         * Return a collection) of {@link ISelectableValueHolder} using the
         * specified searchString.<br/>
         * This method should be overridden if the search feature is available
         * (see SEARCH_ENABLED parameter)
         */
        public ISelectableValueHolderCollection<T> getFoundValueHolders(String searchString, Map<String, String> context) {
            throw new IllegalArgumentException("Method not implemented, should not be called");
        }
    }

    private Handle<T> getHandle() {
        return handle;
    }

    private Class<T> getClazz() {
        return clazz;
    }

    private Map<PickerHandler.Parameters, String> getParameters() {
        return parameters;
    }

    /**
     * The JSON requests can contain a "context" attribute which is an object.
     * <br/>
     * 
     * @param json
     *            a JSON request
     * @return a map of attributes
     */
    private HashMap<String, String> extractContextFromJsonRequest(JsonNode json) {
        HashMap<String, String> context = new HashMap<String, String>();
        JsonNode contextNode = json.get("context");
        if (contextNode != null) {
            Iterator<Map.Entry<String, JsonNode>> contextIterator = contextNode.fields();
            while (contextIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = contextIterator.next();
                context.put(entry.getKey(), entry.getValue().asText());
            }
        }
        return context;
    }

    /**
     * Creates a JSON structure from the specified collection of value holders
     * 
     * @param result
     *            a JSON object (two attributes will be added to this object :
     *            "valuesHolders" and "message")
     * @param valueHolders
     *            a collection of value holders
     */
    private void valueHolderCollectionToJson(ObjectNode result, ISelectableValueHolderCollection<T> valueHolders) {
        Collection<ISelectableValueHolder<T>> valueHoldersList = valueHolders.getValues();
        ObjectNode valueHoldersAsJson = Utilities.marshallAsJson(valueHoldersList);
        result.set("valueHolders", valueHoldersAsJson);
        if (valueHoldersList.size() == 0) {
            result.put("message", Msg.get(getParameters().get(Parameters.NO_VALUE_FOUND_I18N)));
        }
    }
}