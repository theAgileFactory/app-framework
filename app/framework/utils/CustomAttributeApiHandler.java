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
import java.util.List;

import models.framework_models.common.CustomAttributeDefinition;
import models.framework_models.common.ICustomAttributeValue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

public abstract class CustomAttributeApiHandler {

    // class use for modelising the attributes
    @JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE,
            isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(Include.ALWAYS)
    public static class CustomAttributeApiValue {
        @JsonProperty
        public String uuid;

        @JsonProperty
        public String name;

        @JsonProperty
        public String type;

        @JsonProperty
        @ApiModelProperty(required = true)
        public Object value;

        public CustomAttributeApiValue() {

        }
    }

    public static List<CustomAttributeApiValue> getSerializableValues(Class<?> clazz, Long objectId) {
        List<CustomAttributeApiValue> customAtttributeApiValues = new ArrayList<>();

        for (ICustomAttributeValue iCustomAttributeValue : CustomAttributeDefinition.getOrderedCustomAttributeValues(clazz, objectId)) {

            CustomAttributeApiValue customAttributeApiValue = new CustomAttributeApiValue();

            customAttributeApiValue.uuid = iCustomAttributeValue.getDefinition().uuid;
            customAttributeApiValue.name = iCustomAttributeValue.getDefinition().getNameLabel();
            customAttributeApiValue.type = iCustomAttributeValue.getDefinition().attributeType;
            customAttributeApiValue.value = iCustomAttributeValue.getAsSerializableValue();

            customAtttributeApiValues.add(customAttributeApiValue);

        }
        return customAtttributeApiValues;
    }

    public static List<CustomAttributeApiValue> getSerializableValues(Class<?> clazz, String filter, Long objectId) {

        List<CustomAttributeApiValue> customAtttributeApiValues = new ArrayList<>();

        for (ICustomAttributeValue iCustomAttributeValue : CustomAttributeDefinition.getOrderedCustomAttributeValues(clazz, filter, objectId)) {

            CustomAttributeApiValue customAttributeApiValue = new CustomAttributeApiValue();

            customAttributeApiValue.uuid = iCustomAttributeValue.getDefinition().uuid;
            customAttributeApiValue.name = iCustomAttributeValue.getDefinition().getNameLabel();
            customAttributeApiValue.type = iCustomAttributeValue.getDefinition().attributeType;
            customAttributeApiValue.value = iCustomAttributeValue.getAsSerializableValue();

            customAtttributeApiValues.add(customAttributeApiValue);

        }
        return customAtttributeApiValues;
    }

}