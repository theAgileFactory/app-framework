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

import java.math.BigDecimal;

/**
 * Interface for an internal KPI.
 * 
 * @author Johann Kohler
 * 
 */
public interface IKpiRunner {

    /**
     * Compute the KPI main value and return it (only for internal KPI).
     * 
     * @param kpi
     *            the KPI
     * @param objectId
     *            the object id
     */
    public BigDecimal computeMain(Kpi kpi, Long objectId);

    /**
     * Compute the KPI additional1 value and return it (only for internal KPI).
     * 
     * @param kpi
     *            the KPI
     * @param objectId
     *            the object id
     */
    public BigDecimal computeAdditional1(Kpi kpi, Long objectId);

    /**
     * Compute the KPI additional2 value and return it (only for internal KPI).
     * 
     * @param kpi
     *            the KPI
     * @param objectId
     *            the object id
     */
    public BigDecimal computeAdditional2(Kpi kpi, Long objectId);

    /**
     * It's possible to add a link to the cell/box display, simply return it if
     * exists, else return null.
     * 
     * @param objectId
     *            the object id
     */
    public String link(Long objectId);

}
