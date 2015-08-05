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

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Class that encapsulate AJAX communication protocol for the AJAX from
 * framework. See SampleAjax This JSON utility generate messages that are
 * "understood" by the main.js javascript code.
 * 
 * @author Pierre-Yves Cloux
 */
public class FormJsonServerMessage {
    /**
     * Generate a JSON structure that contains a simple "error" message.<br/>
     * This error message will be displayed at the top of the form.
     * 
     * @param unexpectedErrorMessage
     * @return
     */
    public static ObjectNode getUnexpectedErrorStructure(String unexpectedErrorMessage) {
        ObjectNode result = Json.newObject();
        result.put("unexpected", unexpectedErrorMessage);
        return result;
    }

    /**
     * Generate a JSON structure that contains the JSON representation of a Play
     * framework form error set.<br/>
     * Here is an example :
     * 
     * <pre>
     * JsonServerMessage.getErrorStructure(boundForm.errorsAsJson())
     * </pre>
     * 
     * where boundForm is a Play form object.
     * 
     * @param errorsAsJson
     * @return
     */
    public static ObjectNode getErrorStructure(JsonNode errorsAsJson) {
        ObjectNode result = Json.newObject();
        result.set("error", errorsAsJson);
        return result;
    }

    /**
     * Generate a JSON structure that represents a "success".
     * 
     * @param action
     *            the name of an action. This will be used in the JavaScript
     *            form callback to differentiate various possible "successful"
     *            return
     * @param message
     *            a success message to be displayed at the top of the form
     * @return
     */
    public static ObjectNode getSuccessStructure(String action, String message) {
        return getSuccessStructure(action, message, null);
    }

    /**
     * Generate a JSON structure that represents a "success".<br/>
     * This method in addition to an action name and a message also takes an url
     * as a parameter. This url can be used to "download" an HTML fragment.
     * 
     * @param action
     * @param message
     * @param url
     * @return
     */
    public static ObjectNode getSuccessStructure(String action, String message, String url) {
        ObjectNode result = Json.newObject();
        ObjectNode successStructure = Json.newObject();
        result.set("success", successStructure);
        successStructure.put("action", action);
        successStructure.put("message", message);
        if (url != null) {
            successStructure.put("url", url);
        }
        return result;
    }
}