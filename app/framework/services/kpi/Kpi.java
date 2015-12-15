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

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import akka.actor.Cancellable;
import framework.commons.IFrameworkConstants;
import framework.utils.DefaultSelectableValueHolder;
import framework.utils.DefaultSelectableValueHolderCollection;
import framework.utils.ISelectableValueHolderCollection;
import framework.utils.Msg;
import models.framework_models.kpi.KpiColorRule;
import models.framework_models.kpi.KpiData;
import models.framework_models.kpi.KpiDefinition;
import models.framework_models.kpi.KpiValueDefinition;
import models.framework_models.kpi.KpiValueDefinition.RenderType;
import play.Logger;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * A KPI is a correctly defined KPI definition that is ready to use.
 * 
 * @author Johann Kohler.
 * 
 */
public class Kpi {
    private static Logger.ALogger log = Logger.of(Kpi.class);

    private IKpiService kpiService;
    private KpiDefinition kpiDefinition;
    private IKpiRunner kpiRunner;
    private IKpiObjectsContainer kpiObjectsContainer;
    private Cancellable scheduler = null;
    private Map<String, Object> parameters = new HashMap<String, Object>();
    private boolean cancelled;

    /**
     * Construct a KPI with a KPI definition.
     * 
     * @param kpiDefinition
     *            the KPI definition
     * @param kpiService
     *            the KPI service
     */
    public Kpi(KpiDefinition kpiDefinition, IKpiService kpiService) {
        this.kpiDefinition = kpiDefinition;
        this.kpiService = kpiService;
        this.cancelled = false;
    }

    /*
     * Initialization and cancel actions.
     */

    /**
     * Initialize a KPI.
     * 
     * Return true if the initialization has been correctly done.
     */
    public boolean init() {

        // check the cssGlyphicon
        if (hasBoxDisplay() && (kpiDefinition.cssGlyphicon == null || kpiDefinition.cssGlyphicon.equals(""))) {
            Logger.error("The cssGlyphicon shoud be defined because the KPI has additional values");
            return false;
        }

        // check the computationJsCode
        if (!kpiDefinition.isExternal && !kpiDefinition.isStandard) {
            if (kpiDefinition.mainKpiValueDefinition.computationJsCode == null || kpiDefinition.mainKpiValueDefinition.computationJsCode.equals("")) {
                Logger.error("The computationJsCode for the main value should be given because the KPI in internal and custom");
                return false;
            }
            if (kpiDefinition.additional1KpiValueDefinition != null && (kpiDefinition.additional1KpiValueDefinition.computationJsCode == null
                    || kpiDefinition.additional1KpiValueDefinition.computationJsCode.equals(""))) {
                Logger.error("The computationJsCode for the additional1 value should be given because the KPI in internal and custom");
                return false;
            }
            if (kpiDefinition.additional2KpiValueDefinition != null && (kpiDefinition.additional2KpiValueDefinition.computationJsCode == null
                    || kpiDefinition.additional2KpiValueDefinition.computationJsCode.equals(""))) {
                Logger.error("The computationJsCode for the additional2 value should be given because the KPI in internal and custom");
                return false;
            }
        }

        // check and load the parameters
        if (kpiDefinition.parameters != null && !kpiDefinition.parameters.equals("")) {

            PropertiesConfiguration properties = new PropertiesConfiguration();
            try {
                properties.load(new ByteArrayInputStream(kpiDefinition.parameters.getBytes()));
                for (Iterator<String> iter = properties.getKeys(); iter.hasNext();) {
                    String key = iter.next();
                    parameters.put(key, properties.getProperty(key));
                }
            } catch (Exception e) {
                Logger.error("Unable to read correctly the parameters", e);
                return false;
            }

        }

        // load the lpiObjectsContainer
        try {

            Class<?> clazz = getKpiService().getEnvironment().classLoader().loadClass(kpiDefinition.objectType);
            kpiObjectsContainer = (IKpiObjectsContainer) clazz.newInstance();

        } catch (Exception e) {
            Logger.error("Unable to instanciate the objects container", e);
            return false;
        }

        // load the kpiRunner
        try {

            Class<?> clazz = null;
            if (!kpiDefinition.isExternal && kpiDefinition.isStandard) {
                clazz = getKpiService().getEnvironment().classLoader().loadClass(kpiDefinition.clazz);
            } else {
                clazz = CustomKpi.class;
            }
            kpiRunner = (IKpiRunner) clazz.newInstance();

        } catch (Exception e) {
            Logger.error("Unable to instanciate the runner", e);
            return false;

        }

        // initialize the scheduler if exists
        if (this.hasScheduler()) {

            if (!kpiDefinition.schedulerStartTime.matches("^([01]?[0-9]|2[0-3])h[0-5][0-9]$")) {
                Logger.error("The scheduler start time is not correctly formatted");
                return false;
            }

            try {
                this.initScheduler();
            } catch (Exception e) {
                Logger.error("Unable to initialize the scheduler", e);
                return false;
            }
        }

        return true;

    }

    /**
     * Initialize the scheduler of a KPI definition.
     */
    private void initScheduler() {

        // run at start the computation of values
        getKpiService().getSysAdminUtils().scheduleOnce(false, "INITIAL_" + getUid(), Duration.create(2, TimeUnit.MINUTES), new Runnable() {
            @Override
            public void run() {
                if (!isCancelled()) {
                    storeValues();
                }
            }
        });

        String time = kpiDefinition.schedulerStartTime;
        Date today = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time.substring(0, 2)));
        calendar.set(Calendar.MINUTE, Integer.valueOf(time.substring(3, 5)));
        if (calendar.getTime().before(today)) {
            calendar.add(Calendar.DATE, 1);
        }
        long diff = calendar.getTime().getTime() - today.getTime();
        Long howMuchMinutesUntilStartTime = diff / (60 * 1000);

        Logger.info("next scheduler execution: " + howMuchMinutesUntilStartTime + " minutes");

        FiniteDuration frequency = FiniteDuration.create(kpiDefinition.schedulerFrequency, TimeUnit.MINUTES);

        scheduler = getKpiService().getSysAdminUtils().scheduleRecurring(false, getUid(), Duration.create(howMuchMinutesUntilStartTime, TimeUnit.MINUTES),
                frequency, new Runnable() {
                    @Override
                    public void run() {
                        storeValues();
                    }
                });
    }

    /**
     * Cancel a KPI.
     */

    public synchronized void cancel() {
        this.cancelled = true;
        log.info("Request cancel KPI " + getUid());
        if (scheduler != null) {
            scheduler.cancel();
            log.info("Scheduler not null and cancelled for " + getUid());
        }
    }

    /**
     * Store the values in the KPI data table.
     */
    public void storeValues() {

        for (IKpiObjectsContainer kpiObject : kpiObjectsContainer.getAllInstancesForKpi()) {

            Pair<Date, Date> period = this.kpiRunner.getTrendPeriod(this.getKpiService().getPreferenceManagerPlugin(),
                    this.getKpiService().getScriptService(), this, kpiObject.getIdForKpi());
            Date today = new Date();

            if (period == null || (period.getLeft().before(today) && period.getRight().after(today))) {

                BigDecimal main = computeValue(kpiObject.getIdForKpi(), DataType.MAIN);
                BigDecimal additional1 = computeValue(kpiObject.getIdForKpi(), DataType.ADDITIONAL1);
                BigDecimal additional2 = computeValue(kpiObject.getIdForKpi(), DataType.ADDITIONAL2);

                KpiColorRule colorRule = computeColorRule(main, additional1, additional2);

                KpiData mainData = new KpiData();
                mainData.kpiColorRule = colorRule;
                mainData.kpiValueDefinition = kpiDefinition.mainKpiValueDefinition;
                mainData.objectId = kpiObject.getIdForKpi();
                mainData.timestamp = new Date();
                mainData.value = main;
                mainData.save();

                KpiData additional1Data = new KpiData();
                additional1Data.kpiValueDefinition = kpiDefinition.additional1KpiValueDefinition;
                additional1Data.objectId = kpiObject.getIdForKpi();
                additional1Data.timestamp = new Date();
                additional1Data.value = additional1;
                additional1Data.save();

                KpiData additional2Data = new KpiData();
                additional2Data.kpiValueDefinition = kpiDefinition.additional2KpiValueDefinition;
                additional2Data.objectId = kpiObject.getIdForKpi();
                additional2Data.timestamp = new Date();
                additional2Data.value = additional2;
                additional2Data.save();

            }

        }

    }

    /*
     * Getters.
     */

    /**
     * Get the parameters.
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Get the KPI runner.
     */
    public IKpiRunner getKpiRunner() {
        return kpiRunner;
    }

    /**
     * Get the KPI objects container.
     */
    public IKpiObjectsContainer getKpiObjectsContainer() {
        return kpiObjectsContainer;
    }

    /**
     * Get the KPI definition uid.
     */
    public String getUid() {
        return kpiDefinition.uid;
    }

    /**
     * Get the object type.
     */
    public String getObjectType() {
        return kpiDefinition.objectType;
    }

    /**
     * Return the CSS glyphicon.
     */
    public String getCssGlyphicon() {
        return kpiDefinition.cssGlyphicon;
    }

    /**
     * Get the computation JS code of a value definition.
     * 
     * @param dataType
     *            the data type
     */
    public String getComputationJsCode(DataType dataType) {
        switch (dataType) {
        case ADDITIONAL1:
            return kpiDefinition.additional1KpiValueDefinition.computationJsCode;
        case ADDITIONAL2:
            return kpiDefinition.additional2KpiValueDefinition.computationJsCode;
        case MAIN:
            return kpiDefinition.mainKpiValueDefinition.computationJsCode;
        }
        return null;
    }

    /**
     * Get the name of a value definition.
     * 
     * @param dataType
     *            the data type
     */
    public String getValueName(DataType dataType) {
        switch (dataType) {
        case ADDITIONAL1:
            return kpiDefinition.additional1KpiValueDefinition.name;
        case ADDITIONAL2:
            return kpiDefinition.additional2KpiValueDefinition.name;
        case MAIN:
            return kpiDefinition.mainKpiValueDefinition.name;
        }
        return null;
    }

    /**
     * Return true if the KPI has a box display (the additional values are
     * defined).
     */
    public boolean hasBoxDisplay() {
        return kpiDefinition.additional1KpiValueDefinition != null && kpiDefinition.additional2KpiValueDefinition != null;
    }

    /**
     * For an internal KPI, return true if it is scheduled.
     */
    public boolean hasScheduler() {
        boolean r = !kpiDefinition.isExternal && kpiDefinition.schedulerStartTime != null && kpiDefinition.schedulerFrequency != null
                && kpiDefinition.schedulerRealTime != null;
        return r;
    }

    /**
     * Return true if the value of a cell/box display should be taken from the
     * kpiData table (else the value should be computed on the fly).
     * 
     * This is false when:<br/>
     * -The KPI is internal AND hasn't a scheduler<br/>
     * -The KPI is internal AND has a scheduler AND the scheduler real time is
     * true
     */
    public boolean isValueFromKpiData() {

        if (!kpiDefinition.isExternal && (!this.hasScheduler() || kpiDefinition.schedulerRealTime == true)) {
            return false;
        }

        return true;
    }

    /**
     * Return true if the KPI has a trend.
     * 
     * This is true when:<br/>
     * -The KPI is external -The KPI is internal AND has a scheduler
     */
    public boolean hasTrend() {

        if (kpiDefinition.isExternal || this.hasScheduler()) {
            return true;
        }

        return false;
    }

    /**
     * Return true if the KPI definition is external.
     */
    public boolean isExternal() {
        return kpiDefinition.isExternal;
    }

    /**
     * Return true if the KPI definition is standard.
     */
    public boolean isStandard() {
        return kpiDefinition.isStandard;
    }

    /**
     * Return true if the KPI definition should be displayed by default.
     */
    public boolean isDisplayed() {
        return kpiDefinition.isDisplayed;
    }

    /**
     * Return true if the KPI has a link.
     */
    public boolean hasLink() {
        if (!isExternal() && isStandard() && getKpiRunner().link(0L) != null) {
            return true;
        }
        return false;
    }

    /**
     * Get the labels of the KPI color rules as a value holder collection.
     * 
     * Return null if the KPI definition has no color rule.
     */
    public ISelectableValueHolderCollection<Long> getKpiColorRuleLabels() {
        if (this.kpiDefinition.kpiColorRules != null && this.kpiDefinition.kpiColorRules.size() > 0) {
            ISelectableValueHolderCollection<Long> rules = new DefaultSelectableValueHolderCollection<Long>();
            for (KpiColorRule kpiColorRule : this.kpiDefinition.kpiColorRules) {
                String label = kpiColorRule.renderLabel != null ? kpiColorRule.renderLabel : "";
                rules.add(new DefaultSelectableValueHolder<Long>(kpiColorRule.id, label));
            }
            return rules;
        }
        return null;
    }

    /**
     * Return true if the render type of the main value is LABEL.
     */
    public boolean isLabelRenderType() {
        return kpiDefinition.mainKpiValueDefinition.renderType.equals(RenderType.LABEL);
    }

    /*
     * Data.
     */

    /**
     * Get the last KPI data for a value definition.
     * 
     * @param objectId
     *            the object id
     * @param dataType
     *            the data type
     */
    public KpiData getLastKpiData(Long objectId, DataType dataType) {

        switch (dataType) {
        case ADDITIONAL1:
            return KpiData.getLastOfKpiValueDefinitionForObjectId(kpiDefinition.additional1KpiValueDefinition.id, objectId);
        case ADDITIONAL2:
            return KpiData.getLastOfKpiValueDefinitionForObjectId(kpiDefinition.additional2KpiValueDefinition.id, objectId);
        case MAIN:
            return KpiData.getLastOfKpiValueDefinitionForObjectId(kpiDefinition.mainKpiValueDefinition.id, objectId);
        }

        return null;
    }

    /**
     * Get the KPI data of the last 3 months for a trend.
     * 
     * @param objectId
     *            the object id
     */
    public Triple<List<KpiData>, List<KpiData>, List<KpiData>> getTrendData(Long objectId) {
        return Triple.of(getKpiData(objectId, this.kpiDefinition.mainKpiValueDefinition),
                getKpiData(objectId, this.kpiDefinition.additional1KpiValueDefinition),
                getKpiData(objectId, this.kpiDefinition.additional2KpiValueDefinition));
    }

    /**
     * Get the KPI data of a value definition.
     * 
     * @param objectId
     *            the object id
     * @param kpiValueDefinition
     *            the KPI value definition
     */
    private List<KpiData> getKpiData(Long objectId, KpiValueDefinition kpiValueDefinition) {
        if (kpiValueDefinition.isTrendDisplayed) {
            Pair<Date, Date> period = this.kpiRunner.getTrendPeriod(this.getKpiService().getPreferenceManagerPlugin(),
                    this.getKpiService().getScriptService(), this, objectId);
            if (period != null) {
                return KpiData.getKpiDataAsListByPeriod(kpiValueDefinition.id, objectId, period.getLeft(), period.getRight());
            } else {
                return KpiData.getOfLast3MonthsForKpiValueDefinitionAndObjectId(kpiValueDefinition.id, objectId);
            }
        }
        return null;
    }

    /*
     * Computation.
     */

    /**
     * Compute and return a KPI value for a data type (main, additional1,
     * additional2) and an object id.
     * 
     * Note: this method is called only for internal KPI.
     * 
     * @param objectId
     *            the object id
     * @param dataType
     *            the data type
     */
    public BigDecimal computeValue(Long objectId, DataType dataType) {

        if (!kpiDefinition.isExternal) {
            switch (dataType) {
            case ADDITIONAL1:
                return kpiRunner.computeAdditional1(this.getKpiService().getPreferenceManagerPlugin(), this.getKpiService().getScriptService(), this,
                        objectId);
            case ADDITIONAL2:
                return kpiRunner.computeAdditional2(this.getKpiService().getPreferenceManagerPlugin(), this.getKpiService().getScriptService(), this,
                        objectId);
            case MAIN:
                return kpiRunner.computeMain(this.getKpiService().getPreferenceManagerPlugin(), this.getKpiService().getScriptService(), this, objectId);
            }
        }

        return null;

    }

    /**
     * Compute and return the color rule to applied for a KPI definition.
     * 
     * @param main
     *            the main value
     * @param additional1
     *            the additional1 value
     * @param additional2
     *            the additional2 value
     */
    public KpiColorRule computeColorRule(BigDecimal main, BigDecimal additional1, BigDecimal additional2) {

        KpiColorRule computedColorRule = null;

        if (kpiDefinition.kpiColorRules != null) {

            for (KpiColorRule kpiColorRule : KpiColorRule.getKpiColorRuleAsListByDefinition(kpiDefinition.id)) {

                try {
                    SimpleScriptContext scriptContext = new SimpleScriptContext();
                    scriptContext.setAttribute(DataType.MAIN.name().toLowerCase(), main, ScriptContext.ENGINE_SCOPE);

                    if (additional1 != null) {
                        scriptContext.setAttribute(DataType.ADDITIONAL1.name().toLowerCase(), additional1, ScriptContext.ENGINE_SCOPE);
                    }
                    if (additional2 != null) {
                        scriptContext.setAttribute(DataType.ADDITIONAL2.name().toLowerCase(), additional2, ScriptContext.ENGINE_SCOPE);
                    }

                    Boolean ruleResult = false;
                    try {
                        ruleResult = (Boolean) getKpiService().getScriptService().evaluateScript("colorScript", kpiColorRule.rule, scriptContext);
                    } catch (ClassCastException eCast) {
                        Logger.warn("Warning while computing the color rule " + kpiColorRule.id + " for the KPI " + kpiDefinition.uid
                                + ": the last statement should be a boolean");
                    }

                    if (ruleResult) {
                        computedColorRule = kpiColorRule;
                    }

                } catch (Exception e) {

                    String message = "Error while computing the color rule " + kpiColorRule.id + " for the KPI " + kpiDefinition.uid;
                    Logger.error(message, e);

                }

                if (computedColorRule != null) {
                    break;
                }

            }

        }

        return computedColorRule;
    }

    /*
     * Render.
     */

    /**
     * Get the render for a value.
     * 
     * @param colorRule
     *            the computed color rule
     * @param value
     *            the value
     * @param dataType
     *            the data type
     */
    public String getValueRender(KpiColorRule colorRule, BigDecimal value, DataType dataType) {

        KpiValueDefinition kpiValueDefinition = null;
        switch (dataType) {
        case ADDITIONAL1:
            kpiValueDefinition = kpiDefinition.additional1KpiValueDefinition;
            break;
        case ADDITIONAL2:
            kpiValueDefinition = kpiDefinition.additional2KpiValueDefinition;
            break;
        case MAIN:
            kpiValueDefinition = kpiDefinition.mainKpiValueDefinition;
            break;
        }

        if (value != null) {
            switch (kpiValueDefinition.renderType) {
            case LABEL:
                if (colorRule != null && colorRule.renderLabel != null && !colorRule.renderLabel.equals("")) {
                    return Msg.get(colorRule.renderLabel);
                } else {
                    return IFrameworkConstants.DEFAULT_VALUE_EMPTY_DATA;
                }
            case PATTERN:
                String pattern = kpiValueDefinition.renderPattern;
                if (value != null && pattern != null) {
                    pattern = pattern.replaceAll(":default_currency_code", getKpiService().getDefaultCurrencyCode());
                    pattern = pattern.replaceAll(":i", views.html.framework_views.parts.formats.display_number.render(value.intValue(), null, false).body());
                    pattern = pattern.replaceAll(":si", views.html.framework_views.parts.formats.display_number.render(value.intValue(), null, true).body());
                    pattern = pattern.replaceAll(":d", views.html.framework_views.parts.formats.display_number.render(value, null, false).body());
                    pattern = pattern.replaceAll(":sd", views.html.framework_views.parts.formats.display_number.render(value, null, true).body());
                    return pattern;
                }
                break;
            case VALUE:
                return views.html.framework_views.parts.formats.display_number.render(value, null, false).body();
            }
        }

        return views.html.framework_views.parts.formats.display_number.render(value, null, false).body();
    }

    /**
     * List of data types.
     * 
     * @author Johann Kohler
     */
    public static enum DataType {
        MAIN, ADDITIONAL1, ADDITIONAL2;
    }

    /**
     * Get the KPI service.
     */
    private IKpiService getKpiService() {
        return kpiService;
    }

    private synchronized boolean isCancelled() {
        return cancelled;
    }

}
