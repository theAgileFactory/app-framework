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

import javax.script.ScriptEngine;

import org.apache.commons.lang3.tuple.Pair;

import framework.services.account.IPreferenceManagerPlugin;
import framework.services.script.IScriptService;
import models.framework_models.kpi.KpiData;

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
     * @param preferenceManagerPlugin
     *            the preference manager service
     * @param scriptService
     *            the service which manages the {@link ScriptEngine}
     * @param kpi
     *            the KPI
     * @param objectId
     *            the object id
     */
    public BigDecimal computeMain(IPreferenceManagerPlugin preferenceManagerPlugin, IScriptService scriptService, Kpi kpi, Long objectId);

    /**
     * Compute the KPI additional1 value and return it (only for internal KPI).
     * 
     * @param preferenceManagerPlugin
     *            the preference manager service
     * @param scriptService
     *            the service which manages the {@link ScriptEngine}
     * @param kpi
     *            the KPI
     * @param objectId
     *            the object id
     */
    public BigDecimal computeAdditional1(IPreferenceManagerPlugin preferenceManagerPlugin, IScriptService scriptService, Kpi kpi, Long objectId);

    /**
     * Compute the KPI additional2 value and return it (only for internal KPI).
     * 
     * @param preferenceManagerPlugin
     *            the preference manager service
     * @param scriptService
     *            the service which manages the {@link ScriptEngine}
     * @param kpi
     *            the KPI
     * @param objectId
     *            the object id
     */
    public BigDecimal computeAdditional2(IPreferenceManagerPlugin preferenceManagerPlugin, IScriptService scriptService, Kpi kpi, Long objectId);

    /**
     * It's possible to add a link to the cell/box display, simply return it if
     * exists, else return null.
     * 
     * @param objectId
     *            the object id
     */
    public String link(Long objectId);

    /**
     * Get the the trend period.
     * 
     * If null, then corresponds to [today - 3 months, today].
     * 
     * The return value is composed by a start date and an end date.
     * 
     * @param preferenceManagerPlugin
     *            the preference manager service
     * @param scriptService
     *            the service which manages the {@link ScriptEngine}
     * @param kpi
     *            the KPI
     * @param objectId
     *            the object id
     */
    public Pair<Date, Date> getTrendPeriod(IPreferenceManagerPlugin preferenceManagerPlugin, IScriptService scriptService, Kpi kpi, Long objectId);

    /**
     * Draw an additionally static line in the trend.
     * 
     * If null, then no additionally line is drawn.
     * 
     * The return value is composed by a label and list of values.
     * 
     * @param preferenceManagerPlugin
     *            the preference manager service
     * @param scriptService
     *            the service which manages the {@link ScriptEngine}
     * @param kpi
     *            the KPI
     * @param objectId
     *            the object id
     */
    public Pair<String, List<KpiData>> getStaticTrendLine(IPreferenceManagerPlugin preferenceManagerPlugin, IScriptService scriptService, Kpi kpi,
            Long objectId);

}
