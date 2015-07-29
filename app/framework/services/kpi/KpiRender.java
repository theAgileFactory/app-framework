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
import java.util.Date;

import models.framework_models.kpi.KpiColorRule;
import models.framework_models.kpi.KpiData;
import framework.services.kpi.Kpi.DataType;

/**
 * The KPI render is used to display a cell or a box of a KPI.
 * 
 * @author Johann Kohler
 * 
 */
public class KpiRender {

    private static final String DEFAULT_COLOR = "default";

    private Kpi kpi;
    private Long objectId;

    private Date valueTimestamp = null;
    private BigDecimal mainValue = null;
    private BigDecimal additional1Value = null;
    private BigDecimal additional2Value = null;
    private KpiColorRule colorRule = null;

    /**
     * Construct a KPI render.
     * 
     * @param kpi
     *            the KPI
     * @param objectId
     *            the object id
     */
    public KpiRender(Kpi kpi, Long objectId) {

        this.kpi = kpi;
        this.objectId = objectId;

        if (kpi.isValueFromKpiData()) {

            if (kpi.hasBoxDisplay()) {
                KpiData kpiAdditional1Data = kpi.getLastKpiData(objectId, DataType.ADDITIONAL1);
                if (kpiAdditional1Data != null) {
                    additional1Value = kpiAdditional1Data.value;
                }

                KpiData kpiAdditional2Data = kpi.getLastKpiData(objectId, DataType.ADDITIONAL2);
                if (kpiAdditional2Data != null) {
                    additional2Value = kpiAdditional2Data.value;
                }
            }

            KpiData kpiMainData = kpi.getLastKpiData(objectId, DataType.MAIN);
            if (kpiMainData != null) {

                mainValue = kpiMainData.value;
                valueTimestamp = kpiMainData.timestamp;

                /*
                 * To improve the performance, the color rule to applied is also
                 * stored in the KPI data table (this should be done by the
                 * scheduler or the plugin). If the color is not stored, then we
                 * compute and store it.
                 */

                if (kpiMainData.kpiColorRule != null) {
                    colorRule = kpiMainData.kpiColorRule;
                } else {
                    colorRule = kpi.computeColorRule(mainValue, additional1Value, additional2Value);
                    kpiMainData.kpiColorRule = colorRule;
                    kpiMainData.save();
                }

            }

        } else {
            mainValue = kpi.computeValue(objectId, DataType.MAIN);
            if (kpi.hasBoxDisplay()) {
                additional1Value = kpi.computeValue(objectId, DataType.ADDITIONAL1);
                additional2Value = kpi.computeValue(objectId, DataType.ADDITIONAL2);
            }
            colorRule = kpi.computeColorRule(mainValue, additional1Value, additional2Value);
            if (kpi.hasTrend()) {
                KpiData kpiMainData = kpi.getLastKpiData(objectId, DataType.MAIN);
                if (kpiMainData != null) {
                    valueTimestamp = kpiMainData.timestamp;
                }
            }
        }

    }

    /**
     * Get the KPI.
     */
    public Kpi getKpi() {
        return kpi;
    }

    /**
     * Get the value timestamp.
     */
    public Date getValueTimestamp() {
        return valueTimestamp;
    }

    /**
     * Get the render for the main value.
     * 
     * @return
     */
    public String getMainValueRender() {
        return kpi.getValueRender(colorRule, mainValue, DataType.MAIN);
    }

    /**
     * Get the render for the additional1 value.
     * 
     * @return
     */
    public String getAdditional1ValueRender() {
        return kpi.getValueRender(colorRule, additional1Value, DataType.ADDITIONAL1);
    }

    /**
     * Get the render for the additional2 value.
     * 
     * @return
     */
    public String getAdditional2ValueRender() {
        return kpi.getValueRender(colorRule, additional2Value, DataType.ADDITIONAL2);
    }

    /**
     * Get the CSS color.
     */
    public String getCssColor() {

        if (colorRule != null) {
            return colorRule.cssColor;
        }

        return DEFAULT_COLOR;
    }

    /**
     * Get the link (only for internal and standard KPI).
     */
    public String getLink() {

        if (!kpi.isExternal() && kpi.isStandard()) {
            return kpi.getKpiRunner().link(objectId);
        }

        return null;
    }
}
