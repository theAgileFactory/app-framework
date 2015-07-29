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
package framework.services.kpi;

import java.util.List;

/**
 * All BizDock objects that could have a KPI should implements this interface.
 * 
 * @author Johann Kohler
 * 
 */
public interface IKpiObjectsContainer {

    /**
     * Get the object id.
     */
    public Long getIdForKpi();

    /**
     * Get the object by id.
     * 
     * @param objectId
     *            the object id
     */
    public Object getObjectByIdForKpi(Long objectId);

    /**
     * Get all objects for which the KPI should be computed.
     */
    public List<? extends IKpiObjectsContainer> getAllInstancesForKpi();

}
