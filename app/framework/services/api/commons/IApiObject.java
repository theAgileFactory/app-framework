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
package framework.services.api.commons;

public interface IApiObject {

    /**
     * Label which is used to describe the object within an API response. It is
     * a kind of "toString" method but dedicated to object description within
     * the context of the API framework
     * 
     * Label used to get the name of the object in case of an JsonPropertyLink
     * 
     */
    public String getApiName();

    /**
     * Label used for JsonPropertyLink in Api - see bug #1397 If deleted = true
     * Object will be null
     * 
     */
    public boolean getApiDeleted();

}
