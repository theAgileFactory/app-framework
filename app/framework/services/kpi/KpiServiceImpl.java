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

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import models.framework_models.kpi.KpiData;
import models.framework_models.kpi.KpiDefinition;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Result;
import framework.highcharts.HighchartsUtils;
import framework.highcharts.data.SeriesContainer;
import framework.highcharts.data.TimeValueItem;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.database.IDatabaseDependencyService;
import framework.services.kpi.Kpi.DataType;
import framework.services.system.ISysAdminUtils;
import framework.utils.Msg;

/**
 * The KPI service.
 * 
 * @author Johann Kohler
 * 
 */
@Singleton
public class KpiServiceImpl implements IKpiService {
    private static Logger.ALogger log = Logger.of(KpiServiceImpl.class);
    private Hashtable<String, Kpi> kpis;
    private String defaultCurrencyCode = "CHF";
    private ISysAdminUtils sysAdminUtils;
    private Environment environment;

    /**
     * Create a new KpiServiceImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param environment
     *            the application environment
     * @param configuration
     *            the play application configuration
     * @param implementationDefinedObjectService
     *            the implementation time service (depends on the application in
     *            which the framework is implemented)
     * @param databaseDependencyService
     */
    @Inject
    public KpiServiceImpl(ApplicationLifecycle lifecycle, Environment environment, Configuration configuration,
            IImplementationDefinedObjectService implementationDefinedObjectService, IDatabaseDependencyService databaseDependencyService,
            ISysAdminUtils sysAdminUtils) {
        log.info("SERVICE>>> KpiServiceImpl starting...");
        this.environment = environment;
        this.sysAdminUtils = sysAdminUtils;
        this.defaultCurrencyCode = implementationDefinedObjectService.getDefaultCurrencyCode();
        init();
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> KpiServiceImpl stopping...");
            cancel();
            log.info("SERVICE>>> KpiServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> KpiServiceImpl started");
    }

    public void init() {

        kpis = new Hashtable<String, Kpi>();

        log.info("********START init KPI********");

        for (KpiDefinition kpiDefinition : KpiDefinition.getAllActive()) {
            initKpiDefinition(kpiDefinition);
        }

        log.info("********END init KPI********");
    }

    public void cancel() {

        log.info("********START cancel KPI********");

        for (Kpi kpi : kpis.values()) {

            log.info("--------START cancel " + kpi.getUid() + "--------");

            kpi.cancel();

            log.info("--------END cancel " + kpi.getUid() + "--------");
        }

        log.info("********END cancel KPI********");
    }

    @Override
    public void reload() {
        init();
        cancel();
    }

    @Override
    public void reloadKpi(String uid) {

        Kpi kpi = getKpi(uid);

        if (kpi != null) {
            kpi.cancel();
            kpis.remove(uid);
        }

        KpiDefinition kpiDefinition = KpiDefinition.getByUid(uid);

        if (kpiDefinition.isActive) {
            initKpiDefinition(kpiDefinition);
        }

    }

    /**
     * Init a KPI definition.
     * 
     * @param kpiDefinition
     *            the KPI definition
     */
    private void initKpiDefinition(KpiDefinition kpiDefinition) {

        log.info("--------START init " + kpiDefinition.uid + "--------");

        Kpi kpi = new Kpi(kpiDefinition, this);

        if (kpi.init()) {
            log.info("The KPI " + kpiDefinition.uid + " has been correclty loaded");
            kpis.put(kpiDefinition.uid, kpi);
        } else {
            log.error("Impossible to load the KPI " + kpiDefinition.uid + ", the errors are reported above.");
        }

        log.info("--------END init " + kpiDefinition.uid + "--------");

    }

    @Override
    public Kpi getKpi(String uid) {
        if (kpis.containsKey(uid)) {
            return kpis.get(uid);
        }
        return null;
    }

    @Override
    public Hashtable<String, Kpi> getKpis() {
        return kpis;
    }

    @Override
    public Result trend(Context ctx) {

        String uid = ctx.request().getQueryString("kpiUid");
        Long objectId = Long.valueOf(ctx.request().getQueryString("objectId"));

        Kpi kpi = getKpi(uid);

        List<KpiData> datas = kpi.getTrendData(objectId);

        SeriesContainer<TimeValueItem> seriesContainer = null;

        if (datas != null && datas.size() > 0) {

            seriesContainer = new SeriesContainer<TimeValueItem>();
            framework.highcharts.data.Serie<TimeValueItem> timeSerie = new framework.highcharts.data.Serie<TimeValueItem>(Msg.get(kpi
                    .getValueName(DataType.MAIN)));
            seriesContainer.addSerie(timeSerie);

            // if a day contains many values, we get the last
            Map<Date, Double> cleanValues = new LinkedHashMap<Date, Double>();
            for (KpiData kpiData : datas) {
                cleanValues.put(HighchartsUtils.convertToUTCAndClean(kpiData.timestamp), kpiData.value.doubleValue());
            }

            for (Map.Entry<Date, Double> entry : cleanValues.entrySet()) {
                timeSerie.add(new TimeValueItem(entry.getKey(), entry.getValue()));
            }

        }

        return Controller.ok(views.html.framework_views.parts.kpi.display_kpi_trend.render(uid, seriesContainer));
    }

    @Override
    public void setDefaultCurrencyCode(String defaultCurrencyCode) {
        this.defaultCurrencyCode = defaultCurrencyCode;
    }

    @Override
    public String getDefaultCurrencyCode() {
        return defaultCurrencyCode;
    }

    @Override
    public KpiRender getKpiRender(String uid, Long objectId) {
        if (getKpis().containsKey(uid)) {
            return new KpiRender(getKpis().get(uid), objectId);
        }
        return null;
    }

    @Override
    public List<Kpi> getActiveKpisOfObjectType(Class<?> objectType) {
        List<KpiDefinition> kpiDefinitions = KpiDefinition.getActiveOfObjectType(objectType);

        List<Kpi> kpis = new ArrayList<Kpi>();
        for (KpiDefinition kpiDefinition : kpiDefinitions) {
            if (getKpis().containsKey(kpiDefinition.uid)) {
                kpis.add(getKpi(kpiDefinition.uid));
            }

        }

        return kpis;
    }

    @Override
    public List<Kpi> getActiveAndToDisplayKpisOfObjectType(Class<?> objectType) {
        List<KpiDefinition> kpiDefinitions = KpiDefinition.getActiveAndToDisplayOfObjectType(objectType);

        List<Kpi> kpis = new ArrayList<Kpi>();
        for (KpiDefinition kpiDefinition : kpiDefinitions) {
            if (getKpis().containsKey(kpiDefinition.uid)) {
                kpis.add(getKpi(kpiDefinition.uid));
            }

        }

        return kpis;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public ISysAdminUtils getSysAdminUtils() {
        return sysAdminUtils;
    }
}
