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

import framework.commons.IFrameworkConstants;
import framework.services.kpi.Kpi.DataType;
import framework.utils.IColumnFormatter;

/**
 * A column formatter which displays the KPI.
 * 
 * @param <T>
 *            the type of the value for this kpi
 * @author Johann Kohler
 */
public class KpiColumnFormatter<T> implements IColumnFormatter<T> {

    private String kpiUid;
    private DataType dataType;

    public KpiColumnFormatter(String kpiUid, DataType dataType) {
        this.kpiUid = kpiUid;
        this.dataType = dataType;
    }

    @Override
    public String apply(T value, Object cellValue) {
        if (cellValue == null || !(cellValue instanceof Long)) {
            return IFrameworkConstants.DEFAULT_VALUE_EMPTY_DATA;
        }
        Long id = (Long) cellValue;
        return views.html.framework_views.parts.kpi.display_kpi_cell.render(kpiUid, id, dataType).body();
    }

}
