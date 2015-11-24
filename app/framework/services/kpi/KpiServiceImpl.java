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
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import framework.highcharts.HighchartsUtils;
import framework.highcharts.data.SeriesContainer;
import framework.highcharts.data.TimeValueItem;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.database.IDatabaseDependencyService;
import framework.services.kpi.Kpi.DataType;
import framework.services.system.ISysAdminUtils;
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
    private II18nMessagesPlugin messagesPlugin;
    private Provider<IPreferenceManagerPlugin> preferenceManagerPlugin;

    /**
     * Create a new KpiServiceImpl.
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
     * @param messagesPlugin
     *            service for i18n
     * @param databaseDependencyService
     *            the database dependency service
     * @param sysAdminUtils
     *            the system admin utils service
     * @param preferenceManagerPlugin
     *            a provider for the preference manager service
     */
    @Inject
    public KpiServiceImpl(ApplicationLifecycle lifecycle, Environment environment, Configuration configuration,
            IImplementationDefinedObjectService implementationDefinedObjectService, II18nMessagesPlugin messagesPlugin,
            IDatabaseDependencyService databaseDependencyService, ISysAdminUtils sysAdminUtils, Provider<IPreferenceManagerPlugin> preferenceManagerPlugin) {
        log.info("SERVICE>>> KpiServiceImpl starting...");
        this.messagesPlugin = messagesPlugin;
        this.environment = environment;
        this.sysAdminUtils = sysAdminUtils;
        this.preferenceManagerPlugin = preferenceManagerPlugin;
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

    /**
     * Initialize the service.
     */
    public void init() {

        kpis = new Hashtable<String, Kpi>();

        log.info("********START init KPI********");

        for (KpiDefinition kpiDefinition : KpiDefinition.getAllActive()) {
            initKpiDefinition(kpiDefinition);
        }

        log.info("********END init KPI********");
    }

    /**
     * Cancel the service.
     */
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

        Date startDate = null;
        Date endDate = null;

        Triple<List<KpiData>, List<KpiData>, List<KpiData>> datas = kpi.getTrendData(objectId);
        Pair<String, List<KpiData>> staticTrendLine = kpi.getKpiRunner().getStaticTrendLine(this.getPreferenceManagerPlugin(), kpi, objectId);

        SeriesContainer<TimeValueItem> seriesContainer = null;

        if (staticTrendLine != null || (datas.getLeft() != null && datas.getLeft().size() > 0) || (datas.getMiddle() != null && datas.getMiddle().size() > 0)
                || (datas.getRight() != null && datas.getRight().size() > 0)) {

            seriesContainer = new SeriesContainer<TimeValueItem>();

            if (datas.getLeft() != null && datas.getLeft().size() > 0) {
                addTrendSerieAndValues(seriesContainer, kpi, DataType.MAIN, datas.getLeft());
            }

            if (datas.getMiddle() != null && datas.getMiddle().size() > 0) {
                addTrendSerieAndValues(seriesContainer, kpi, DataType.ADDITIONAL1, datas.getMiddle());
            }

            if (datas.getRight() != null && datas.getRight().size() > 0) {
                addTrendSerieAndValues(seriesContainer, kpi, DataType.ADDITIONAL2, datas.getRight());
            }

            if (staticTrendLine != null) {
                framework.highcharts.data.Serie<TimeValueItem> timeSerie = new framework.highcharts.data.Serie<TimeValueItem>(
                        getMessagesPlugin().get(staticTrendLine.getLeft()));
                seriesContainer.addSerie(timeSerie);
                for (KpiData kpiData : staticTrendLine.getRight()) {
                    timeSerie.add(new TimeValueItem(HighchartsUtils.convertToUTCAndClean(kpiData.timestamp), kpiData.value.doubleValue()));
                }

            }

            Pair<Date, Date> period = kpi.getKpiRunner().getTrendPeriod(this.getPreferenceManagerPlugin(), kpi, objectId);
            if (period != null) {
                startDate = period.getLeft();
                endDate = period.getRight();
            }

        }

        return Controller.ok(views.html.framework_views.parts.kpi.display_kpi_trend.render(uid, seriesContainer, startDate, endDate));
    }

    /**
     * Add a serie (to a serie container) for a trend.
     * 
     * @param seriesContainer
     *            the serie container
     * @param kpi
     *            the KPI
     * @param dataType
     *            the data type
     * @param datas
     *            the datas (values)
     */
    private void addTrendSerieAndValues(SeriesContainer<TimeValueItem> seriesContainer, Kpi kpi, DataType dataType, List<KpiData> datas) {

        framework.highcharts.data.Serie<TimeValueItem> timeSerie = new framework.highcharts.data.Serie<TimeValueItem>(
                getMessagesPlugin().get(kpi.getValueName(dataType)));
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

    @Override
    public IPreferenceManagerPlugin getPreferenceManagerPlugin() {
        return this.preferenceManagerPlugin.get();
    }

    @Override
    public void addData(String uid, Long objectId, Date timestamp, BigDecimal mainValue, BigDecimal additional1Value, BigDecimal additional2Value)
            throws KpiServiceException {

        uid = uid.trim();
        KpiDefinition kpiDefinition = KpiDefinition.getByUid(uid);
        if (kpiDefinition == null) {
            throw new KpiServiceException("The KPI with the specified uid is not found", 404);
        }

        if (mainValue == null || additional1Value == null || additional2Value == null) {
            throw new KpiServiceException("The mainValue, additional1Value and additional2Value should not be null", 400);
        }

        Kpi kpi = this.getKpi(uid);
        if (kpi == null) {
            throw new KpiServiceException("Impossible to add a data for an inactive KPI", 400);
        }

        if (kpi.isStandard()) {
            throw new KpiServiceException("Impossible to add a data for a standard KPI", 400);
        }

        if (!kpi.isExternal()) {
            throw new KpiServiceException("Impossible to add a data for a KPI with data provided by BizDock", 400);
        }

        if (!kpi.hasBoxDisplay()) {
            throw new KpiServiceException("Impossible to add a data for a KPI without box display.", 400);
        }

        // Check the object id (should exist for the given object type)
        if (kpi.getKpiObjectsContainer().getObjectByIdForKpi(objectId) == null) {
            throw new KpiServiceException("Impossible to find the corresponding object for the given objectId", 400);
        }

        // If the timestamp is not given then get the current date
        if (timestamp == null) {
            timestamp = new Date();
        }

        KpiData mainKpiData = new KpiData();
        mainKpiData.kpiValueDefinition = kpiDefinition.mainKpiValueDefinition;
        mainKpiData.objectId = objectId;
        mainKpiData.timestamp = timestamp;
        mainKpiData.value = mainValue;
        mainKpiData.save();

        KpiData additional1KpiData = new KpiData();
        additional1KpiData.kpiValueDefinition = kpiDefinition.additional1KpiValueDefinition;
        additional1KpiData.objectId = objectId;
        additional1KpiData.timestamp = timestamp;
        additional1KpiData.value = additional1Value;
        additional1KpiData.save();

        KpiData additional2KpiData = new KpiData();
        additional2KpiData.kpiValueDefinition = kpiDefinition.additional2KpiValueDefinition;
        additional2KpiData.objectId = objectId;
        additional2KpiData.timestamp = timestamp;
        additional2KpiData.value = additional2Value;
        additional2KpiData.save();

    }

    /**
     * Get the messages service.
     */
    private II18nMessagesPlugin getMessagesPlugin() {
        return messagesPlugin;
    }

    /**
     * The KPI service exception.
     * 
     * @author Johann Kohler
     */
    public static class KpiServiceException extends Exception {

        private static final long serialVersionUID = 15698232151247954L;

        private int httpCode;

        /**
         * Default constructor.
         * 
         * @param message
         *            the error message
         * @param httpCode
         *            the corresponding HTTP error code
         */
        public KpiServiceException(String message, int httpCode) {
            super(message);
            this.httpCode = httpCode;
        }

        /**
         * Get the HTTP error code.
         */
        public int getHttpCode() {
            return this.httpCode;
        }
    }
}
