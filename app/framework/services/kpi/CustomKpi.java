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
import java.util.List;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.apache.commons.lang3.tuple.Pair;

import framework.services.account.IPreferenceManagerPlugin;
import framework.services.kpi.Kpi.DataType;
import framework.services.script.IScriptService;
import models.framework_models.kpi.KpiData;
import play.Logger;

/**
 * The custom KPI computation class (for an internal and non-standard KPI).
 * 
 * @author Johann Kohler
 */
public class CustomKpi implements IKpiRunner {

    @Override
    public BigDecimal computeMain(IPreferenceManagerPlugin preferenceManagerPlugin, IScriptService scriptService, Kpi kpi, Long objectId) {
        return computeValue(scriptService, kpi, objectId, DataType.MAIN);
    }

    @Override
    public BigDecimal computeAdditional1(IPreferenceManagerPlugin preferenceManagerPlugin, IScriptService scriptService, Kpi kpi, Long objectId) {
        return computeValue(scriptService, kpi, objectId, DataType.ADDITIONAL1);
    }

    @Override
    public BigDecimal computeAdditional2(IPreferenceManagerPlugin preferenceManagerPlugin, IScriptService scriptService, Kpi kpi, Long objectId) {
        return computeValue(scriptService, kpi, objectId, DataType.ADDITIONAL2);
    }

    @Override
    public String link(Long objectId) {
        return null;
    }

    /**
     * Compute a value.
     * 
     * @param scriptService
     * @param kpi
     *            the KPI
     * @param objectId
     *            the object id
     * @param dataType
     *            the value type
     * @return
     */
    private BigDecimal computeValue(IScriptService scriptService, Kpi kpi, Long objectId, DataType dataType) {

        Object object = kpi.getKpiObjectsContainer().getObjectByIdForKpi(objectId);

        BigDecimal value = null;

        try {
            SimpleScriptContext simpleScriptContext = new SimpleScriptContext();
            simpleScriptContext.setAttribute("object", object, ScriptContext.ENGINE_SCOPE);
            Object result = scriptService.evaluateScript("colorScript", kpi.getComputationJsCode(dataType), simpleScriptContext);

            // convert the return value to a BigDecimal
            try {
                value = new BigDecimal((Integer) result);
            } catch (Exception e1) {
                try {
                    value = new BigDecimal((Double) result);
                } catch (Exception e2) {
                    try {
                        value = new BigDecimal((Long) result);
                    } catch (Exception e3) {
                        try {
                            value = (BigDecimal) result;
                        } catch (Exception e4) {
                        }
                    }
                }
            }

            if (value == null) {
                String message = "The " + dataType.name().toLowerCase() + " value for the KPI " + kpi.getUid() + " is not a number";
                Logger.error(message);
            }

        } catch (Exception e) {

            String message = "Error while computing the " + dataType.name().toLowerCase() + " value for the KPI " + kpi.getUid();
            Logger.error(message, e);

        }

        return value;
    }

    @Override
    public Pair<Date, Date> getTrendPeriod(IPreferenceManagerPlugin preferenceManagerPlugin, IScriptService scriptService, Kpi kpi, Long objectId) {
        return null;
    }

    @Override
    public Pair<String, List<KpiData>> getStaticTrendLine(IPreferenceManagerPlugin preferenceManagerPlugin, IScriptService scriptService, Kpi kpi,
            Long objectId) {
        return null;
    }

}
