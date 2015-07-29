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
package framework.services.plugins.api;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Properties;

/**
 * Properties with preserving add order. This ensures, for instance, that the
 * default configuration appears in the same order as the one defined in the
 * default configuration.
 * 
 * @author Johann Kohler
 */
public class LinkedProperties extends Properties {

    private static final long serialVersionUID = 1583387121047L;

    private final LinkedHashSet<Object> keys = new LinkedHashSet<Object>();

    /**
     * Get an Enumeration of the key.
     */
    public Enumeration<Object> keys() {
        return Collections.<Object> enumeration(keys);
    }

    /**
     * Put a property.
     * 
     * @param key
     *            the key
     * @param value
     *            the property value
     */
    public Object put(Object key, Object value) {
        keys.add(key);
        return super.put(key, value);
    }
}