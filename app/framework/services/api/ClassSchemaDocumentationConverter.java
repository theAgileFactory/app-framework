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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.converter.SwaggerSchemaConverter;
import com.wordnik.swagger.model.Model;

import play.Logger;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.immutable.Map;

/**
 * A converter which is hiding irrelevant attributes from the Swagger
 * documentation.<br/>
 * Only the methods and public properties annotated with {@link JsonProperty}
 * will be taken into account.<br/>
 * All other properties are ignored.
 * 
 * @author Pierre-Yves Cloux
 */
public class ClassSchemaDocumentationConverter extends SwaggerSchemaConverter {
    private static Logger.ALogger log = Logger.of(ClassSchemaDocumentationConverter.class);

    public ClassSchemaDocumentationConverter() {
    }

    @Override
    public Option<Model> read(Class<?> modelClass, Map<String, String> typeMap) {
        final Option<Model> modelOption = super.read(modelClass, typeMap);

        // Look for public properties
        java.util.Map<String, ApiControllerUtilsServiceImpl.SerializationEntry> entries = ApiControllerUtilsServiceImpl.getSerializationEntries(modelClass,
                null);

        // Remove all properties which are not public
        if (!modelOption.isEmpty()) {
            Iterator<String> keysIterator = modelOption.get().properties().keysIterator();
            while (keysIterator.hasNext()) {
                String propertyName = keysIterator.next();
                if (!entries.containsKey(propertyName)) {
                    modelOption.get().properties().remove(propertyName);
                    if (log.isDebugEnabled()) {
                        log.debug("Successfully hidden API model property '" + propertyName + "'");
                    }
                }
            }
        }

        return modelOption;
    }
}
