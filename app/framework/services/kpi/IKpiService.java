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

import java.util.Hashtable;
import java.util.List;

import play.mvc.Http.Context;
import play.mvc.Result;

/**
 * The interface for the KPI service.
 * 
 * @author Johann Kohler
 * 
 */
public interface IKpiService {

    public static final String NAME = "kpiService";

    /**
     * Get the KPIs.
     */
    public Hashtable<String, Kpi> getKpis();

    /**
     * Initialize the KPIs. This method should be called on starting the
     * application.
     */
    public void init();

    /**
     * Cancel the KPIs. This method should be called on stopping the
     * application.
     */
    public void cancel();

    /**
     * Reload a KPI definition.
     * 
     * @param uid
     *            the KPI definition uid
     */
    public void reloadKpi(String uid);

    /**
     * Set the default currency code.
     * 
     * @param defaultCurrencyCode
     *            the default currency code
     */
    public void setDefaultCurrencyCode(String defaultCurrencyCode);

    /**
     * Return the default currency code.
     */
    public String getDefaultCurrencyCode();

    /**
     * Get a KPI by KPI definition uid.
     * 
     * @param uid
     *            the KPI definition uid.
     */
    public Kpi getKpi(String uid);

    /**
     * Draw a trend.
     * 
     * @param ctx
     *            the request context
     */
    public Result trend(Context ctx);

    /**
     * Get a KPI render by KPI definition uid.
     * 
     * @param uid
     *            the KPI definition uid
     * @param objectId
     *            the object ID
     */
    public KpiRender getKpiRender(String uid, Long objectId);

    /**
     * Get the active KPIs of an object type.
     * 
     * @param objectType
     *            the object type
     */
    public List<Kpi> getActiveKpisOfObjectType(Class<?> objectType);

    /**
     * Get the active and to display KPIs of an object type.
     * 
     * @param objectType
     *            the object type
     */
    public List<Kpi> getActiveAndToDisplayKpisOfObjectType(Class<?> objectType);
}
