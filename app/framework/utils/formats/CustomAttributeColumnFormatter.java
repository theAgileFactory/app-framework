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
package framework.utils.formats;

import models.framework_models.common.CustomAttributeDefinition;
import models.framework_models.common.ICustomAttributeValue;
import framework.commons.IFrameworkConstants;
import framework.utils.IColumnFormatter;

/**
 * A column formatter which displays the extended attributes.<br/>
 * This one requires:
 * <ul>
 * <li>objectType : the type of object to which the extended attributes are
 * attached</li>
 * <li>customAttributeDefinitionId : the unique id for the custom definition</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 * @param <T>
 */
public class CustomAttributeColumnFormatter<T> implements IColumnFormatter<T> {
    private Class<?> objectType;
    private Long customAttributeDefinitionId;

    public CustomAttributeColumnFormatter(Class<?> objectType, Long customAttributeDefinitionId) {
        this.objectType = objectType;
        this.customAttributeDefinitionId = customAttributeDefinitionId;
    }

    @Override
    public String apply(T value, Object cellValue) {
        if (cellValue == null || !(cellValue instanceof Long)) {
            return IFrameworkConstants.DEFAULT_VALUE_EMPTY_DATA;
        }
        Long id = (Long) cellValue;
        ICustomAttributeValue customAttributeValue = getCustomAttributeValue(id);
        return customAttributeValue.renderDisplay().body();
    }

    private Class<?> getObjectType() {
        return objectType;
    }

    private Long getCustomAttributeDefinitionId() {
        return customAttributeDefinitionId;
    }

    public ICustomAttributeValue getCustomAttributeValue(Long id) {
        return CustomAttributeDefinition.getCustomAttributeValue(getCustomAttributeDefinitionId(), getObjectType(), id);
    }

}
