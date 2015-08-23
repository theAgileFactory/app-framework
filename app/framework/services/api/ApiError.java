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

import java.util.List;
import java.util.Map;

import play.data.validation.ValidationError;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

import framework.services.configuration.II18nMessagesPlugin;
import framework.utils.Utilities;

/**
 * Generic object returned when an error occurs during an API call.<br/>
 * Here are the attributes:
 * <ul>
 * <li>code : an error code (0 means unexpected)</li>
 * <li>reason : a String describing the issue if any</li>
 * <li>trace : an error trace if the error raised an exception</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiError {
    public static final int UNEXPECTED_ERROR = 0;
    private int code = UNEXPECTED_ERROR;
    private String reason;
    private String trace;

    public ApiError() {
    }

    public ApiError(String reason) {
        this(0, reason);
    }

    public ApiError(int code, String reason) {
        super();
        this.code = code;
        this.reason = reason;
    }

    public ApiError(int code, String reason, Exception e) {
        super();
        this.code = code;
        this.reason = reason;
        this.trace = Utilities.getExceptionAsString(e);
    }

    @JsonProperty(value = "reason")
    @ApiModelProperty(value = "A reason for the error as a String", required = true)
    public String getReason() {

        return reason;
    }

    @JsonProperty(value = "code")
    @ApiModelProperty(value = "A error code specific to this error, please refer to the documentation", required = true)
    public int getCode() {
        return code;
    }

    @JsonProperty(value = "trace")
    @ApiModelProperty(value = "An exception stacktrace", required = false)
    public String getTrace() {
        return trace;
    }

    /**
     * Return the validation data errors as a String
     * @param messagesPlugin a {@link II18nMessagesPlugin} for internationalization management
     * @param errors the errors
     * @return
     */
    public static String getValidationErrorsMessage(II18nMessagesPlugin messagesPlugin ,Map<String, List<ValidationError>> errors) {
        String errorMsg = "[ ";
        for (String field : errors.keySet()) {
            errorMsg += field + " : ";
            for (ValidationError error : errors.get(field)) {
                errorMsg += messagesPlugin.get(error.message()) + ", ";
            }
        }
        // cut the last colon
        errorMsg = errorMsg.substring(0, errorMsg.length() - 2);
        errorMsg += " ]";
        return errorMsg;
    }
}
