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
package framework.services.audit;

import java.io.Serializable;

/**
 * A class that specifies which objects are editable or not.<br/>
 * By default if an object is not found, it is considered as not auditable.
 * 
 * @author Pierre-Yves Cloux
 */
public class Auditable implements Serializable {
    private static final long serialVersionUID = -8358089258530142345L;
    public String objectClass;
    public boolean isAuditable;

    public Auditable(String objectClass, boolean isAuditable) {
        super();
        this.objectClass = objectClass;
        this.isAuditable = isAuditable;
    }

    public Auditable() {
    }
}
