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
package framework.utils;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFolder;

import com.avaje.ebean.Expr;
import com.avaje.ebean.Expression;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.OrderBy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import framework.services.kpi.IKpiService;
import framework.services.kpi.Kpi;
import framework.services.kpi.Kpi.DataType;
import models.framework_models.account.Principal;
import models.framework_models.common.CustomAttributeDefinition;
import models.framework_models.common.FilterConfiguration;
import models.framework_models.common.ICustomAttributeValue.AttributeType;
import play.Logger;
import play.libs.Json;
import play.mvc.Http.Request;

/**
 * A data structure which contains the configuration for managing advanced table
 * filtering.<br/>
 * <ul>
 * <li><b>selectableColumns</b> : a collection of selectable columns
 * configuration</li>
 * <li><b>userColumnConfiguration</b> : the dynamic configuration for the
 * columns</li>
 * <li><b>currentPage</b> : the current page</li>
 * </ul>
 * 
 * Here is the JSON configuration structure
 * 
 * <pre>
 * { 
 * "selectableColumns" : { 
 *      "value1" : {
 *          "name" : "Name 1", 
 *          "value" : "value1",
 *             "type" : "CHECKBOX",
 *             "defaultValue" : true
 *             }, 
 *         "value2" : {
 *             "name" : "Name 2",
 *             "value" : "value2",
 *             "type" : "TEXTFIELD",
 *             "defaultValue":"a value"},
 *         "value3" : {
 *             "name" : "Name 3", 
 *             "value" : "value3",
 *             "type" : "DATERANGE",
 *             "fromDefaultValue" : "19/11/1974",
 *             "toDefaultValue" : "19/11/1974",
 *             "format" : "dd/mm/yyyy"},
 *         "value4" : {
 *             "name" : "Name 4", 
 *             "value" : "value4",
 *             "type" : "SELECT",
 *             "defaultValue" : "option2", 
 *             "values" : { 
 *                 "option1" : {"name" :"This is option1", "value" : "option1"}, 
 *                 "option2" : {"name" : "This is option2", "value" : "option2"} 
 *             }} 
 *         "value5" : {
 *             "name" : "Name 4", 
 *             "value" : "value4",
 *             "type" : "AUTOCOMPLETE", 
 *             "url" : "http://an_url/"} 
 * },
 * "userColumnConfiguration" : {
 *             "value1" : {
 *                 "sortType" : "DESC",
 *                 "isDisplayed" : true,
 *                 "isFiltered" : true,
 *                 "filterValue" : true
 *             },
 *             "value2" :{
 *                 "sortType" : "DESC",
 *                 "isDisplayed" : true,
 *                 "isFiltered" : true,
 *                 "filterValue" : "*"
 *             },
 *             "value3" :{
 *                 "sortType" : "UNSORTED",
 *                 "isDisplayed" : true,
 *                 "isFiltered" : true,
 *                 "filterValue" : {
 *                     "from" : "19/11/1974",
 *                     "to" : "19/11/1974"
 *                 }
 *             },
 *             "value4" :{
 *                 "sortType" : "NONE",
 *                 "isDisplayed" : true,
 *                 "isFiltered" : true,
 *                 "filterValue" : "option2"
 *             }   
 * }, 
 * "currentPage" : 0 
 * }
 * </pre>
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <T>
 */
public class FilterConfig<T> {
    private static Logger.ALogger log = Logger.of(FilterConfig.class);

    /*
     * The JSON structure field which contains the static configuration for a
     * column in the filter component
     */
    public static final String JSON_SELECTABLE_COLUMNS_FIELD = "selectableColumns";

    /*
     * The JSON structure field which contains the dynamic configuration (user
     * modifiable) for a column in the filter component
     */
    public static final String JSON_USER_CONFIG_FIELD = "userColumnConfiguration";

    /*
     * The JSON structure field which contains the current page number
     */
    public static final String JSON_CURRENT_PAGE_FIELD = "currentPage";

    /**
     * The JSON field name for the selected rows (checked boxes).
     */
    public static final String JSON_SELECTED_ROWS_FIELD = "selectedRows";

    /*
     * Contains the static meta-data for a column to appear in the filter
     * configuration component
     */
    private Map<String, SelectableColumn> selectableColumns;

    /*
     * Contains the dynamic configuration for the selection component and the
     * table
     */
    private Map<String, UserColumnConfiguration> userColumnConfigurations;

    private int currentPage;

    /**
     * The current selected rows (box checked). This attribute is not "static",
     * meaning it is settled only in a copy of the filter config.
     */
    private List<String> selectedRows;

    /**
     * The selected filter configuration (for the select list).
     */
    private FilterConfiguration selectedFilterConfiguration;

    /**
     * Creates a filter configuration.
     */
    public FilterConfig() {
        this.selectableColumns = new HashMap<String, FilterConfig.SelectableColumn>();
        this.userColumnConfigurations = new HashMap<String, FilterConfig.UserColumnConfiguration>();
        this.selectedRows = null;
    }

    /**
     * Creates a filter configuration by copying the information from the
     * specified one.
     * 
     * @param template
     *            the filter config template
     * @param selectedFilterConfiguration
     *            the selected filter configuration
     * @param deepCopy
     *            true for a deep copy
     */
    private FilterConfig(FilterConfig<T> template, FilterConfiguration selectedFilterConfiguration, boolean deepCopy) {
        if (deepCopy) {
            this.selectableColumns = new HashMap<String, FilterConfig.SelectableColumn>(template.getSelectableColumns());
        } else {
            this.selectableColumns = template.getSelectableColumns();
        }
        this.userColumnConfigurations = new HashMap<String, FilterConfig.UserColumnConfiguration>();
        for (UserColumnConfiguration userColumnConfig : template.getUserColumnConfigurations().values()) {
            this.userColumnConfigurations.put(userColumnConfig.getColumnId(), userColumnConfig.copy());
        }
        this.selectedRows = null;
        this.selectedFilterConfiguration = selectedFilterConfiguration;
    }

    /**
     * Create a new FilterConfig instance from the current one and load it with
     * the JSON structure received from the javascript client.
     * 
     * @param json
     *            a JSON structure
     * @param selectedFilterConfiguration
     *            the selected filter configuration
     */
    private synchronized FilterConfig<T> parseResponse(JsonNode json, FilterConfiguration selectedFilterConfiguration) throws FilterConfigException {
        FilterConfig<T> temp = new FilterConfig<T>(this, selectedFilterConfiguration, false);
        temp.unmarshall(json);
        return temp;
    }

    /**
     * Get the current filter configuration. This action is called when the page
     * (with the concerned table) is displayed.
     * 
     * @param principalUid
     *            the principal uid
     * @param request
     *            the original request
     */
    public synchronized FilterConfig<T> getCurrent(String principalUid, Request request) {

        // get the generic class (data type)
        final Pattern pattern = Pattern.compile("<(.+?)>");
        final Matcher matcher = pattern.matcher(getClass().getGenericSuperclass().getTypeName());
        matcher.find();
        String dataType = matcher.group(1);

        /*
         * Get the default filter (it corresponds to the last seen by the user).
         * It is created with the initial configuration if it doesn't exist.
         */
        FilterConfiguration defaultFilter = FilterConfiguration.getDefaultFilterConfiguration(principalUid, dataType);
        if (defaultFilter == null) {
            defaultFilter = new FilterConfiguration();
            defaultFilter.configuration = this.marshall();
            defaultFilter.initialConfiguration = defaultFilter.configuration;
            defaultFilter.dataType = dataType;
            defaultFilter.isDefault = true;
            defaultFilter.isSelected = false;
            defaultFilter.name = "object.filter_configuration.name.default.label";
            defaultFilter.principal = Principal.getPrincipalFromUid(principalUid);
            defaultFilter.save();
        }

        /*
         * If the request includes a "filterSharedKey" query param, then we try
         * to get the corresponding filter for the datatype. If no filter is
         * existing (even deleted) then we ignore the parameter and use standard
         * mechanism (no error message is displayed). If the filter is existing
         * (deleted or not) then we consider a "share" filter (an error message
         * is displayed if the filter has been deleted).
         */
        boolean isSharedFilter = false;
        FilterConfiguration sharedFilter = null;
        if (request.getQueryString("filterSharedKey") != null && !request.getQueryString("filterSharedKey").equals("")) {
            sharedFilter = FilterConfiguration.getFilterConfigurationBySharedKey(request.getQueryString("filterSharedKey"), dataType);
            if (sharedFilter != null) {
                isSharedFilter = true;
            }
        }

        if (isSharedFilter) {

            // if the filter has been deleted then we use the default filter and
            // display an error message
            if (sharedFilter.deleted) {
                defaultFilter.sharedNotExisting = true;
            } else {
                defaultFilter.configuration = sharedFilter.configuration;
                defaultFilter.save();
                FilterConfiguration selectedFilter = FilterConfiguration.getSelectedFilterConfiguration(principalUid, dataType);
                if (selectedFilter != null) {
                    selectedFilter.isSelected = false;
                    selectedFilter.save();
                }
            }

            // convert the JSON string to a JSON node.
            JsonNode json = null;
            try {

                ObjectMapper mapper = new ObjectMapper();
                json = mapper.readTree(defaultFilter.configuration);

            } catch (Exception e) {

                // should not occurred
                Logger.error("impossible to get the filter configuration", e);
                return null;

            }

            // parse the configuration
            try {

                return parseResponse(json, defaultFilter);
            } catch (Exception e) {

                /*
                 * the filter is no more compatible with the current table
                 * configuration, then we display an warning message and get the
                 * initial configuration.
                 */

                Logger.warn("the filter is no more compatible", e);

                defaultFilter.isNotCompatible = true;

                try {

                    ObjectMapper mapper = new ObjectMapper();
                    return parseResponse(mapper.readTree(this.marshall()), defaultFilter);

                } catch (Exception e2) {

                    // should not occurred
                    Logger.error("impossible to get the filter configuration", e);
                    return null;

                }
            }

        } else {

            // Get the selected filter configuration (it could be the default
            // filter).
            FilterConfiguration selectedFilter = FilterConfiguration.getSelectedFilterConfiguration(principalUid, dataType);

            // convert the JSON string to a JSON node.
            JsonNode json = null;
            try {

                ObjectMapper mapper = new ObjectMapper();
                json = mapper.readTree(defaultFilter.configuration);

            } catch (Exception e) {

                // should not occurred
                Logger.error("impossible to get the filter configuration", e);
                return null;

            }

            // parse the configuration
            try {

                return parseResponse(json, selectedFilter);

            } catch (Exception e) {

                /*
                 * the filter is no more compatible with the current table
                 * configuration, then we store the initial configuration in the
                 * default filter and return it (a warning message is
                 * displayed).
                 */

                Logger.warn("the filter is no more compatible", e);

                defaultFilter.isNotCompatible = true;
                defaultFilter.configuration = this.marshall();
                defaultFilter.save();

                try {

                    ObjectMapper mapper = new ObjectMapper();
                    return parseResponse(mapper.readTree(defaultFilter.configuration), defaultFilter);

                } catch (Exception e2) {

                    // should not occurred
                    Logger.error("impossible to get the filter configuration", e);
                    return null;

                }

            }

        }

    }

    /**
     * Persist the current configuration in the default filter. This action is
     * called after each action (edit a filter, change page, add columns...) on
     * a table or when the user change the selected filter.
     * 
     * This method returns null if the selected filter is no more compatible
     * with the table configuration (this case should be treated by the caller).
     * 
     * @param principalUid
     *            the principal uid
     * @param request
     *            the original request
     */
    public synchronized FilterConfig<T> persistCurrentInDefault(String principalUid, Request request) {

        // get the generic class (data type)
        final Pattern pattern = Pattern.compile("<(.+?)>");
        final Matcher matcher = pattern.matcher(getClass().getGenericSuperclass().getTypeName());
        matcher.find();
        String dataType = matcher.group(1);

        // get the json form the request
        JsonNode json = request.body().asJson();

        // get the default filter
        FilterConfiguration defaultFilter = FilterConfiguration.getDefaultFilterConfiguration(principalUid, dataType);

        // store the request configuration in the default filter.
        defaultFilter.configuration = json.toString();
        defaultFilter.save();

        // parse the configuration
        try {

            // the selectedFilterConfiguration is settled to null because it is
            // not
            // used by the answer
            return parseResponse(json, null);

        } catch (Exception e) {

            /*
             * the filter is no more compatible with the current table
             * configuration (this case can occur only when the user has changed
             * the selected filter), then we store the initial configuration in
             * the default filter and return null (the caller must treat this
             * case).
             */

            Logger.warn("the filter is no more compatible", e);

            defaultFilter.configuration = this.marshall();
            defaultFilter.save();

            return null;

        }
    }

    /**
     * Return the selected filter configuration.
     */
    public FilterConfiguration getSelectedFilterConfiguration() {
        return this.selectedFilterConfiguration;
    }

    /**
     * Add a column configuration or modify the one with the same columnId.
     * 
     * @param columnId
     *            the ID of the column
     * @param fieldName
     *            the name of the field associated with this column (should
     *            match the table field name). If the field is an id of an
     *            object you can use "object.id" or any other Ebean syntax
     * @param columnLabel
     *            the label for this column
     * @param filterComponent
     *            the component
     * @param isDisplayed
     *            true if the attribute if displayed by default
     * @param isFiltered
     *            true if the attribute is filtered by default
     * @param sortStatusType
     *            the attribute sort
     */
    public synchronized void addColumnConfiguration(String columnId, String fieldName, String columnLabel, IFilterComponent filterComponent,
            boolean isDisplayed, boolean isFiltered, SortStatusType sortStatusType) {
        SelectableColumn selectableColumn = new SelectableColumn(columnId, fieldName, columnLabel, null, filterComponent);
        getSelectableColumns().put(columnId, selectableColumn);
        UserColumnConfiguration userColumnConfiguration = new UserColumnConfiguration(columnId, sortStatusType, isDisplayed, isFiltered,
                selectableColumn.getFilterComponent().getDefaultFilterValueAsObject());
        getUserColumnConfigurations().put(columnId, userColumnConfiguration);
    }

    public synchronized void addColumnConfiguration(String columnId, String fieldName, String columnLabel, String columnSubLabel,
            IFilterComponent filterComponent, boolean isDisplayed, boolean isFiltered, SortStatusType sortStatusType) {
        SelectableColumn selectableColumn = new SelectableColumn(columnId, fieldName, columnLabel, columnSubLabel, filterComponent);
        getSelectableColumns().put(columnId, selectableColumn);
        UserColumnConfiguration userColumnConfiguration = new UserColumnConfiguration(columnId, sortStatusType, isDisplayed, isFiltered,
                selectableColumn.getFilterComponent().getDefaultFilterValueAsObject());
        getUserColumnConfigurations().put(columnId, userColumnConfiguration);
    }

    /**
     * Add a column configuration for a column not displayed by default.
     * 
     * @param columnId
     *            the ID of the column
     * @param fieldName
     *            the name of the field associated with this column (should
     *            match the table field name). If the field is an id of an
     *            object you can use "object.id" or any other Ebean syntax
     * @param columnLabel
     *            the label for this column
     * @param filterComponent
     *            the component
     */
    public synchronized void addNotDisplayedColumnConfiguration(String columnId, String fieldName, String columnLabel, IFilterComponent filterComponent) {
        addColumnConfiguration(columnId, fieldName, columnLabel, filterComponent, false, false, SortStatusType.NONE);
    }

    /**
     * Add a column configuration for a column not displayed by default.
     * 
     * @param columnId
     *            the ID of the column
     * @param fieldName
     *            the name of the field associated with this column (should
     *            match the table field name). If the field is an id of an
     *            object you can use "object.id" or any other Ebean syntax
     * @param columnLabel
     *            the label for this column
     */
    public synchronized void addNonFilterableNotDisplayedColumnConfiguration(String columnId, String fieldName, String columnLabel) {
        addColumnConfiguration(columnId, fieldName, columnLabel, new NoneFilterComponent(), false, false, SortStatusType.NONE);
    }

    /**
     * Add a column configuration or modify the one with the same columnId.<br/>
     * This column cannot be filtered (yet it is possible to select if it should
     * be displayed or not)
     * 
     * @param columnId
     *            the ID of the column
     * @param fieldName
     *            the name of the field associated with this column (should
     *            match the table field name). If the field is an id of an
     *            object you can use "object.id" or any other Ebean syntax
     * @param columnLabel
     *            the label for this column
     * @param isDisplayed
     *            true if the attribute is displayed by default
     */
    public synchronized void addNonFilterableColumnConfiguration(String columnId, String fieldName, String columnLabel, boolean isDisplayed) {
        this.addColumnConfiguration(columnId, fieldName, columnLabel, new NoneFilterComponent(), isDisplayed, false, SortStatusType.NONE);
    }

    /**
     * Add all the custom attributes to the filters and sort configuration. The
     * columns are added "not displayed".
     * 
     * @param tableIdFieldName
     *            the Id field name used by the table to be filtered
     * @param clazz
     *            the class to which the custom attributes belongs
     */
    public synchronized void addCustomAttributesColumns(String tableIdFieldName, Class<?> clazz) {
        List<CustomAttributeDefinition> customAttributeDefinitions = CustomAttributeDefinition.getOrderedCustomAttributeDefinitions(clazz);
        for (CustomAttributeDefinition customAttributeDefinition : customAttributeDefinitions) {
            switch (AttributeType.valueOf(customAttributeDefinition.attributeType)) {
            case INTEGER:
                addColumnConfiguration(customAttributeDefinition.uuid, tableIdFieldName, customAttributeDefinition.name,
                        new IntegerCustomAttributeFilterComponent("0", "=", customAttributeDefinition), customAttributeDefinition.isDisplayed, false,
                        SortStatusType.UNSORTED);
                break;
            case DECIMAL:
                addColumnConfiguration(customAttributeDefinition.uuid, tableIdFieldName, customAttributeDefinition.name,
                        new DecimalCustomAttributeFilterComponent("0.0", "=", customAttributeDefinition), customAttributeDefinition.isDisplayed, false,
                        SortStatusType.UNSORTED);
                break;
            case BOOLEAN:
                addColumnConfiguration(customAttributeDefinition.uuid, tableIdFieldName, customAttributeDefinition.name,
                        new BooleanCustomAttributeFilterComponent(false, customAttributeDefinition), customAttributeDefinition.isDisplayed, false,
                        SortStatusType.UNSORTED);
                break;
            case DATE:
                addColumnConfiguration(customAttributeDefinition.uuid, tableIdFieldName, customAttributeDefinition.name,
                        new DateCustomAttributeFilterComponent(new Date(), new Date(), Utilities.getDefaultDatePattern(), customAttributeDefinition),
                        customAttributeDefinition.isDisplayed, false, SortStatusType.UNSORTED);
                break;
            case STRING:
                addColumnConfiguration(customAttributeDefinition.uuid, tableIdFieldName, customAttributeDefinition.name,
                        new StringCustomAttributeFilterComponent("*", customAttributeDefinition), customAttributeDefinition.isDisplayed, false,
                        SortStatusType.UNSORTED);
                break;
            case SINGLE_ITEM:
                addColumnConfiguration(customAttributeDefinition.uuid, tableIdFieldName, customAttributeDefinition.name,
                        new SingleItemCustomAttributeFilterComponent(customAttributeDefinition), customAttributeDefinition.isDisplayed, false,
                        SortStatusType.UNSORTED);
                break;
            case MULTI_ITEM:
                addColumnConfiguration(customAttributeDefinition.uuid, tableIdFieldName, customAttributeDefinition.name,
                        new MultiItemCustomAttributeFilterComponent(customAttributeDefinition), customAttributeDefinition.isDisplayed, false,
                        SortStatusType.NONE);
                break;
            case DYNAMIC_SINGLE_ITEM:
            case DYNAMIC_MULTI_ITEM:
                addNonFilterableColumnConfiguration(customAttributeDefinition.uuid, tableIdFieldName, customAttributeDefinition.name,
                        customAttributeDefinition.isDisplayed);
                break;
            case TEXT:
                addColumnConfiguration(customAttributeDefinition.uuid, tableIdFieldName, customAttributeDefinition.name,
                        new TextCustomAttributeFilterComponent("*", customAttributeDefinition), customAttributeDefinition.isDisplayed, false,
                        SortStatusType.UNSORTED);
                break;
            case URL:
                addColumnConfiguration(customAttributeDefinition.uuid, tableIdFieldName, customAttributeDefinition.name,
                        new StringCustomAttributeFilterComponent("*", customAttributeDefinition), customAttributeDefinition.isDisplayed, false,
                        SortStatusType.UNSORTED);
                break;
            case IMAGE:
                break;
            default:
                throw new IllegalArgumentException("Invalid attribute type : " + customAttributeDefinition.attributeType);
            }
        }
    }

    /**
     * Add all KPIs of an object type to a filter config.
     * 
     * @param kpiService
     *            the KPI service
     * @param tableIdFieldName
     *            the Id field name used by the table to be filtered
     * @param objectType
     *            the object type
     */
    public synchronized void addKpis(IKpiService kpiService, String tableIdFieldName, Class<?> objectType) {
        List<Kpi> kpis = kpiService.getActiveKpisOfObjectType(objectType);
        if (kpis != null) {
            for (Kpi kpi : kpis) {
                addKpi(kpiService, tableIdFieldName, kpi.getUid());
            }
        }
    }

    /**
     * Add a KPI for an object type to a filter config.
     * 
     * Note: the KPI is by default not displayed.
     * 
     * @param kpiService
     *            the KPI service
     * @param tableIdFieldName
     *            the Id field name used by the table to be filtered
     * @param kpiUid
     *            the KPI definition uid
     */
    public synchronized void addKpi(IKpiService kpiService, String tableIdFieldName, String kpiUid) {
        Kpi kpi = kpiService.getKpi(kpiUid);
        if (kpi != null) {
            if (kpi.isTrendDisplayed(DataType.MAIN)) {
                addKpiValueDefinition(kpi, tableIdFieldName, DataType.MAIN);
            }
            if (kpi.isTrendDisplayed(DataType.ADDITIONAL1)) {
                addKpiValueDefinition(kpi, tableIdFieldName, DataType.ADDITIONAL1);
            }
            if (kpi.isTrendDisplayed(DataType.ADDITIONAL2)) {
                addKpiValueDefinition(kpi, tableIdFieldName, DataType.ADDITIONAL2);
            }
        }

    }

    private synchronized void addKpiValueDefinition(Kpi kpi, String tableIdFieldName, DataType dataType) {

        String columnId = Table.KPI_COLUMN_NAME_PREFIX + dataType.name() + kpi.getUid();
        String subLabel = null;
        if (!dataType.equals(DataType.MAIN)) {
            subLabel = kpi.getValueName(dataType);
        }

        if (kpi.isValueFromKpiData()) {
            if (kpi.isLabelRenderType(dataType) && kpi.getKpiColorRuleLabels() != null) {
                ISelectableValueHolderCollection<Long> rules = kpi.getKpiColorRuleLabels();
                addColumnConfiguration(columnId, tableIdFieldName, kpi.getValueName(DataType.MAIN), subLabel,
                        new KpiSelectFilterComponent(dataType, rules.getSortedValues().get(0).getValue(), rules, kpi), false, false, SortStatusType.UNSORTED);
            } else {
                addColumnConfiguration(columnId, tableIdFieldName, kpi.getValueName(DataType.MAIN), subLabel,
                        new KpiNumericFilterComponent(dataType, "0", "=", kpi), false, false, SortStatusType.UNSORTED);
            }
        } else {
            addColumnConfiguration(columnId, tableIdFieldName, kpi.getValueName(DataType.MAIN), subLabel, new KpiNoneFilterComponent(), false, false,
                    SortStatusType.NONE);
        }
    }

    /**
     * Update the expression with the right SQL instructions.
     * 
     * @param <K>
     *            the corresponding model object
     * @param expression
     *            an SQL expression
     */
    public synchronized <K> ExpressionList<K> updateWithSearchExpression(ExpressionList<K> expression) {
        for (String columnId : getUserColumnConfigurations().keySet()) {
            UserColumnConfiguration userColumnConfiguration = getUserColumnConfigurations().get(columnId);
            SelectableColumn selectableColumn = getSelectableColumns().get(columnId);
            if (userColumnConfiguration.isFiltered()) {
                String fieldName = selectableColumn.getFieldName();
                Expression temp = selectableColumn.getFilterComponent().getEBeanSearchExpression(userColumnConfiguration.getFilterValue(), fieldName);
                if (temp != null) {
                    expression.add(temp);
                }
            }
        }
        return expression;
    }

    /**
     * Create a search expression from the concatenation of the of the various
     * expressions.<br/>
     * WARNING: return null if no expression can be found
     */
    public synchronized Expression getSearchExpression() {
        Expression currentExpression = null;
        for (String columnId : getUserColumnConfigurations().keySet()) {
            UserColumnConfiguration userColumnConfiguration = getUserColumnConfigurations().get(columnId);
            SelectableColumn selectableColumn = getSelectableColumns().get(columnId);
            if (userColumnConfiguration.isFiltered()) {
                String fieldName = selectableColumn.getFieldName();
                Expression temp = selectableColumn.getFilterComponent().getEBeanSearchExpression(userColumnConfiguration.getFilterValue(), fieldName);
                if (currentExpression == null) {
                    currentExpression = temp;
                } else {
                    if (temp != null) {
                        currentExpression = Expr.and(currentExpression, temp);
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format(">>>>>>>>>>>>>>>>>> Search in column %s for value %s", columnId, userColumnConfiguration.getFilterValue()));
                }
            }
        }
        return currentExpression;
    }

    /**
     * Update the expression with the right SQL instructions.
     * 
     * @param <K>
     *            the corresponding model object
     * @param expression
     *            an SQL expression
     */
    public synchronized <K> void updateWithSortExpression(ExpressionList<K> expression) {
        OrderBy<K> orderby = expression.orderBy();
        for (String columnId : getUserColumnConfigurations().keySet()) {
            UserColumnConfiguration userColumnConfiguration = getUserColumnConfigurations().get(columnId);
            SelectableColumn selectableColumn = getSelectableColumns().get(columnId);
            String fieldName = selectableColumn.getFieldName();
            selectableColumn.getFilterComponent().addEBeanSortExpression(orderby, userColumnConfiguration.getSortStatusType(), fieldName);
        }
        if (log.isDebugEnabled()) {
            log.debug("Orderby clause : " + orderby.toString());
        }
    }

    /**
     * Return an order by computed from the various available sort component.
     * 
     * @param <K>
     *            the corresponding model object
     */
    public synchronized <K> OrderBy<K> getSortExpression() {
        OrderBy<K> orderby = new OrderBy<K>();
        for (String columnId : getUserColumnConfigurations().keySet()) {
            UserColumnConfiguration userColumnConfiguration = getUserColumnConfigurations().get(columnId);
            SelectableColumn selectableColumn = getSelectableColumns().get(columnId);
            String fieldName = selectableColumn.getFieldName();
            selectableColumn.getFilterComponent().addEBeanSortExpression(orderby, userColumnConfiguration.getSortStatusType(), fieldName);
        }
        if (log.isDebugEnabled()) {
            log.debug("Orderby clause : " + orderby.toString());
        }
        return orderby;
    }

    /**
     * Return the list of columns to be hidden in the table (the selectable
     * columns which are not selected).
     * 
     * @return a list of column Id
     */
    public synchronized Set<String> getColumnsToHide() {
        Set<String> columnsToHide = new HashSet<String>();
        for (String columnId : getUserColumnConfigurations().keySet()) {
            if (!getUserColumnConfigurations().get(columnId).isDisplayed()) {
                columnsToHide.add(columnId);
            }
        }
        return columnsToHide;
    }

    /**
     * Creates a JSON structure from the specified class data.<br/>
     * This marshall the whole state of the object.<br/>
     * 
     * @return
     */
    public synchronized String marshall() {
        ObjectNode asJson = Json.newObject();

        // Add static configuration
        ObjectNode selectableColumnsAsJson = Json.newObject();
        for (String columnId : getSelectableColumns().keySet()) {
            selectableColumnsAsJson.set(columnId, getSelectableColumns().get(columnId).marshall());
        }
        asJson.set(JSON_SELECTABLE_COLUMNS_FIELD, selectableColumnsAsJson);

        // Add the dynamic configuration
        ObjectNode userColumnConfigurationAsJson = Json.newObject();
        for (String columnId : getUserColumnConfigurations().keySet()) {
            userColumnConfigurationAsJson.set(columnId, getUserColumnConfigurations().get(columnId).marshall(getSelectableColumns().get(columnId)));
        }
        asJson.set(JSON_USER_CONFIG_FIELD, userColumnConfigurationAsJson);

        // Add the current page
        asJson.put("currentPage", getCurrentPage());

        // Add selected rows
        ArrayNode selectableRowsAsJson = asJson.putArray(JSON_SELECTED_ROWS_FIELD);
        if (this.selectedRows != null) {
            for (String id : this.selectedRows) {
                selectableRowsAsJson.add(id);
            }
        }

        return Json.stringify(asJson);
    }

    /**
     * Get the user column configurations.
     */
    public synchronized Map<String, UserColumnConfiguration> getUserColumnConfigurations() {
        return userColumnConfigurations;
    }

    /**
     * Update the content of this object with a JSON structure.<br/>
     * NB: selectableColumns are never updated
     * 
     * @param json
     *            the json node to unmarshall
     */
    private void unmarshall(JsonNode json) throws FilterConfigException {
        JsonNode currentPageNode = json.get(JSON_CURRENT_PAGE_FIELD);
        if (currentPageNode != null && currentPageNode.getNodeType() == JsonNodeType.NUMBER) {
            setCurrentPage(((Long) currentPageNode.asLong()).intValue());
            if (log.isDebugEnabled()) {
                log.debug("Unmarshalled page number : " + getCurrentPage());
            }
        }

        JsonNode userConfigAsJson = json.get(JSON_USER_CONFIG_FIELD);
        if (userConfigAsJson != null && userConfigAsJson.getNodeType() == JsonNodeType.OBJECT) {
            Iterator<String> fieldNames = userConfigAsJson.fieldNames();
            while (fieldNames.hasNext()) {
                String columnId = fieldNames.next();
                if (getUserColumnConfigurations().containsKey(columnId)) {
                    UserColumnConfiguration userColumnConfiguration = getUserColumnConfigurations().get(columnId);
                    JsonNode userColumnConfigurationAsJson = userConfigAsJson.get(columnId);
                    if (log.isDebugEnabled()) {
                        log.debug("Unmarshalled json : " + userColumnConfigurationAsJson);
                    }
                    userColumnConfiguration.unmarshall(getSelectableColumns().get(columnId), userColumnConfigurationAsJson);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Unknown column : " + columnId);
                    }
                }
            }
        }

        if (json.has(JSON_SELECTED_ROWS_FIELD) && json.get(JSON_SELECTED_ROWS_FIELD).isArray()) {
            this.selectedRows = new ArrayList<>();
            for (JsonNode node : json.get(JSON_SELECTED_ROWS_FIELD)) {
                this.selectedRows.add(node.asText());
            }
        }
    }

    /**
     * Get the selectable columns.
     */
    private Map<String, SelectableColumn> getSelectableColumns() {
        return selectableColumns;
    }

    /**
     * Get the current page.
     */
    public synchronized int getCurrentPage() {
        return currentPage;
    }

    /**
     * Set the current page.
     * 
     * @param currentPage
     *            the current page to set
     */
    public synchronized void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    /**
     * Get the selected rows.
     */
    public synchronized List<String> getSelectedRows() {
        return selectedRows;
    }

    /**
     * Helper to get the selected rows (ids) from the request (depending of the
     * row action type: ajax or link).
     * 
     * @param request
     *            the HTTP request
     */
    public static List<String> getIdsFromRequest(Request request) {

        try {

            List<String> ids = new ArrayList<>();
            JsonNode selectedRows = null;

            // ajax case
            JsonNode json = request.body().asJson();
            if (json != null && json.get(JSON_SELECTED_ROWS_FIELD) != null) {
                selectedRows = json.get(JSON_SELECTED_ROWS_FIELD);
            }

            // link case
            Map<String, String[]> formUrlEncoded = request.body().asFormUrlEncoded();
            if (formUrlEncoded != null && formUrlEncoded.get(JSON_SELECTED_ROWS_FIELD) != null && formUrlEncoded.get(JSON_SELECTED_ROWS_FIELD).length > 0) {
                String[] selectedRowsString = formUrlEncoded.get(JSON_SELECTED_ROWS_FIELD);
                ObjectMapper mapper = new ObjectMapper();
                selectedRows = mapper.readTree(selectedRowsString[0]);
            }

            if (selectedRows != null) {
                for (JsonNode elem : selectedRows) {
                    ids.add(elem.asText());
                }
                return ids;
            }

        } catch (Exception e) {
            Logger.error("impossible to get the ids", e);
        }

        return null;
    }

    /**
     * The data structure which contains the configuration (meta-data) for a
     * column.<br/>
     * Here is a sample structure for a checkbox:
     * 
     * <pre>
     * {
     *         "name" : "Name 1", 
     *         "value" : "value1",
     *         "type" : "CHECKBOX",
     *         "defaultValue" : true
     * }
     * </pre>
     * 
     * @author Pierre-Yves Cloux
     */
    public static class SelectableColumn {
        /*
         * The JSON structure field which contains the label of for this column
         */
        public static final String JSON_NAME_FIELD = "name";

        /*
         * The JSON structure field which contains the columnId for this column.
         */
        public static final String JSON_VALUE_FIELD = "value";

        /*
         * The JSON structure field which contains the filter component type for
         * this column
         */
        public static final String JSON_TYPE_FIELD = "type";

        private String columnId;
        private String fieldName;
        private String columnLabel;
        private String columnSubLabel;
        private IFilterComponent filterComponent;

        /**
         * Creates a new column for the filter configuration.
         * 
         * @param columnId
         *            the name of the table column
         * @param fieldName
         *            the name of the underlying "database" field (see Ebean)
         * @param columnLabel
         *            the label to be used for the column (in the column
         *            selector)
         * @param filterComponent
         *            the filter configuration component
         */
        public SelectableColumn(String columnId, String fieldName, String columnLabel, String columnSubLabel, IFilterComponent filterComponent) {
            super();
            this.columnId = columnId;
            this.fieldName = fieldName;
            this.columnLabel = columnLabel;
            this.columnSubLabel = columnSubLabel;
            this.filterComponent = filterComponent;
        }

        /**
         * Get the column id.
         */
        public String getColumnId() {
            return columnId;
        }

        /**
         * Get the field name.
         */
        public String getFieldName() {
            return fieldName;
        }

        /**
         * Get the column label.
         */
        public String getColumnLabel() {
            return columnLabel;
        }

        public String getColumnSubLabel() {
            return this.columnSubLabel;
        }

        /**
         * Get the filter component.
         */
        public IFilterComponent getFilterComponent() {
            return filterComponent;
        }

        /**
         * Returns a JSON representation of the specified column.
         * 
         * @return a json structure
         */
        public ObjectNode marshall() {
            ObjectNode asJson = Json.newObject();
            String label = Msg.get(getColumnLabel());
            if (getColumnSubLabel() != null) {
                label += " / " + Msg.get(getColumnSubLabel());
            }
            asJson.put(JSON_NAME_FIELD, label);
            asJson.put(JSON_VALUE_FIELD, getColumnId());
            asJson.put("isKpi", getFilterComponent().isKpi());
            getFilterComponent().marshallMetaData(asJson);
            return asJson;
        }
    }

    /**
     * The configuration for a column as selected by the end user.<br/>
     * This one can be marshalled and unmarshalled. Here is an example:
     * 
     * <pre>
     *         "value1" : {
     *             "isSorted" : "DESC",
     *             "isDisplayed" : true,
     *             "isFiltered" : true,
     *             "filterValue" : true
     *         },
     * </pre>
     */
    public static class UserColumnConfiguration {
        /*
         * The JSON structure field which contains true if the column is sorted
         */
        public static final String JSON_SORTTYPE_FIELD = "sortType";

        /*
         * The JSON structure field which contains true if the column is
         * displayed
         */
        public static final String JSON_ISDISPLAYED_FIELD = "isDisplayed";

        /*
         * The JSON structure field which contains true if the column is fitered
         */
        public static final String JSON_ISFILTERED_FIELD = "isFiltered";

        /*
         * The JSON structure field which contains the data structure which
         * contains the value to apply to the filter
         */
        public static final String JSON_FILTERVALUE_FIELD = "filterValue";

        private String columnId;
        private SortStatusType sortStatusType;
        private boolean isDisplayed;
        private boolean isFiltered;
        private Object filterValue;

        /**
         * Creates a new configuration for a column.<br/>
         * In principle this configuration is modified by the end user through
         * the filter graphical user interface
         * 
         * @param columnId
         *            the name of the column for this configuration
         * @param sortStatusType
         *            the {@link SortStatusType}
         * @param isDisplayed
         *            true if the column is displayed
         * @param isFiltered
         *            true if the column is filtered
         * @param filterValue
         *            a value to be used in the filters
         */
        public UserColumnConfiguration(String columnId, SortStatusType sortStatusType, boolean isDisplayed, boolean isFiltered, Object filterValue) {
            super();
            this.columnId = columnId;
            this.sortStatusType = sortStatusType;
            this.isDisplayed = isDisplayed;
            this.isFiltered = isFiltered;
            this.filterValue = filterValue;
        }

        /**
         * Copy the current configuration and store it in a new object.
         */
        public UserColumnConfiguration copy() {
            UserColumnConfiguration newColumnConfig = new UserColumnConfiguration(this.columnId, this.sortStatusType, this.isDisplayed, this.isFiltered,
                    this.filterValue);
            return newColumnConfig;
        }

        /**
         * Get the sort status type.
         */
        public SortStatusType getSortStatusType() {
            return sortStatusType;
        }

        /**
         * Return true if it is filtered.
         */
        public boolean isFiltered() {
            return isFiltered;
        }

        /**
         * Get the column id.
         */
        public String getColumnId() {
            return columnId;
        }

        /**
         * Return true if it is displayed.
         */
        public boolean isDisplayed() {
            return isDisplayed;
        }

        /**
         * Get the filter value.
         */
        public Object getFilterValue() {
            return filterValue;
        }

        /**
         * Generates a JSON representation of the column configuration using the
         * previously stored value for the filter component.
         * 
         * @param selectableColumn
         *            the column meta-data
         */
        public ObjectNode marshall(SelectableColumn selectableColumn) {
            ObjectNode asJson = Json.newObject();
            asJson.put(JSON_SORTTYPE_FIELD, getSortStatusType().name());
            asJson.put(JSON_ISDISPLAYED_FIELD, isDisplayed());
            asJson.put(JSON_ISFILTERED_FIELD, isFiltered());
            asJson.put("isKpi", selectableColumn.getFilterComponent().isKpi());
            selectableColumn.getFilterComponent().marshallFilterValue(asJson, getFilterValue());
            return asJson;
        }

        /**
         * Update the state of the filter config with the data provided from the
         * server.
         * 
         * @param selectableColumn
         *            the {@link SelectableColumn} object associated with this
         *            column
         * @param json
         *            sent from the server
         */
        public void unmarshall(SelectableColumn selectableColumn, JsonNode json) throws FilterConfigException {
            this.isDisplayed = json.get(JSON_ISDISPLAYED_FIELD).asBoolean();
            this.isFiltered = json.get(JSON_ISFILTERED_FIELD).asBoolean();
            this.sortStatusType = SortStatusType.valueOf(json.get(JSON_SORTTYPE_FIELD).asText());
            this.filterValue = selectableColumn.getFilterComponent().getFilterValueFromJson(json.get(JSON_FILTERVALUE_FIELD));
        }
    }

    /**
     * A class which tells about the status of a sort for a named column.
     * <ul>
     * <li>NONE : there is no sort</li>
     * <li>UNSORTED : a sort is defined but no specific order is activated</li>
     * <li>ASC : the sort is set by default to ascending sort</li>
     * <li>DESC : the sort is set by default to descending sort</li>
     * </ul>
     * 
     * @author Pierre-Yves Cloux
     */
    public enum SortStatusType {
        ASC, DESC, UNSORTED, NONE;
    }

    /**
     * The type of filter components.
     * 
     * @author Pierre-Yves Cloux
     */
    public enum FilterComponentType {
        CHECKBOX, TEXTFIELD, NUMERIC, DATERANGE, SELECT, AUTOCOMPLETE, NONE
    }

    /**
     * The generic interface for a filter component.
     * 
     * @author Pierre-Yves Cloux
     */
    public static interface IFilterComponent {

        /**
         * Convert the meta data for this fiter component into JSON.<br/>
         * This one contains the static for the filter components to be used by
         * the JavaScript client.
         * 
         * @param selectableColumnFilterComponentMetaData
         *            the meta data of the selected filter
         */
        public void marshallMetaData(ObjectNode selectableColumnFilterComponentMetaData);

        /**
         * Convert the default value for the filter.
         * 
         * @param userColumnConfiguration
         *            the user column configuration.
         * @param filterValue
         *            the filter value to be used to marshall
         */
        public void marshallFilterValue(ObjectNode userColumnConfiguration, Object filterValue);

        /**
         * Convert the JSON structure into one or more values to be used for the
         * SQL query search section.
         * 
         * @param filterValue
         *            the filter value
         * @param fieldName
         *            the name of the field (should match the SQL field name)
         */
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName);

        /**
         * Convert the JSON structure into one or more values to be used for the
         * SQL query orderBy section.
         * 
         * @param <T>
         *            the corresponding class
         * 
         * @param orderby
         *            an order by expression set
         * @param sortStatusType
         *            the sort status type
         * @param fieldName
         *            the name of the field (should match the SQL field name)
         */
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName);

        /**
         * Convert the JSON structure into one or more java values.
         * 
         * @param json
         *            the JSON value
         */
        public Object getFilterValueFromJson(JsonNode json) throws FilterConfigException;

        /**
         * Get the default value as an Object.<br/>
         * The nature of the object vary depending on the type of component
         * 
         * @return an object
         */
        public Object getDefaultFilterValueAsObject();

        /**
         * The KPI fields are displayed as a group just under the other fields.
         */
        public boolean isKpi();
    }

    /**
     * A filter component implemented as a checkbox.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class CheckboxFilterComponent implements IFilterComponent {
        private boolean defaultValue;

        /**
         * Default constructor.
         * 
         * @param defaultValue
         *            the default value
         */
        public CheckboxFilterComponent(boolean defaultValue) {
            super();
            this.defaultValue = defaultValue;
        }

        /**
         * Get the default value.
         */
        public boolean getDefaultValue() {
            return defaultValue;
        }

        @Override
        public void marshallMetaData(ObjectNode selectableColumnFilterComponentMetaData) {
            selectableColumnFilterComponentMetaData.put(SelectableColumn.JSON_TYPE_FIELD, FilterComponentType.CHECKBOX.name());
            selectableColumnFilterComponentMetaData.put("defaultValue", getDefaultValue());
        }

        @Override
        public void marshallFilterValue(ObjectNode userColumnConfiguration, Object filterValue) {
            if (!(filterValue instanceof Boolean)) {
                userColumnConfiguration.put(UserColumnConfiguration.JSON_FILTERVALUE_FIELD, getDefaultValue());
                log.error("The value " + filterValue + " returned for this filter component " + this + " is invalid, use default one instead");
            } else {
                userColumnConfiguration.put(UserColumnConfiguration.JSON_FILTERVALUE_FIELD, (Boolean) filterValue);
            }
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {
            if (filterValue != null) {
                return Expr.eq(fieldName, filterValue);
            }
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                if (sortStatusType == SortStatusType.DESC) {
                    orderby.desc(fieldName);
                } else {
                    orderby.asc(fieldName);
                }
            }
        }

        @Override
        public Object getFilterValueFromJson(JsonNode json) {
            return json.asBoolean();
        }

        @Override
        public Object getDefaultFilterValueAsObject() {
            return getDefaultValue();
        }

        @Override
        public String toString() {
            return "CheckboxFilterComponent [defaultValue=" + defaultValue + "]";
        }

        @Override
        public boolean isKpi() {
            return false;
        }
    }

    /**
     * A filter component implemented as a text field.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class TextFieldFilterComponent implements IFilterComponent {
        public static final String JOKER = "*";
        private String defaultValue;

        /**
         * Default constructor.
         * 
         * @param defaultValue
         *            the default value
         */
        public TextFieldFilterComponent(String defaultValue) {
            super();
            this.defaultValue = defaultValue;
        }

        /**
         * Get the default value.
         */
        public String getDefaultValue() {
            return defaultValue;
        }

        @Override
        public void marshallMetaData(ObjectNode selectableColumnFilterComponentMetaData) {
            selectableColumnFilterComponentMetaData.put(SelectableColumn.JSON_TYPE_FIELD, FilterComponentType.TEXTFIELD.name());
            selectableColumnFilterComponentMetaData.put("defaultValue", getDefaultValue());
        }

        @Override
        public void marshallFilterValue(ObjectNode userColumnConfiguration, Object filterValue) {
            if (!(filterValue instanceof String)) {
                userColumnConfiguration.put(UserColumnConfiguration.JSON_FILTERVALUE_FIELD, getDefaultValue());
                log.error("The value " + filterValue + " returned for this filter component " + this + " is invalid, use default one instead");
            } else {
                userColumnConfiguration.put(UserColumnConfiguration.JSON_FILTERVALUE_FIELD, (String) filterValue);
            }
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {
            if (filterValue != null) {
                String value = (String) filterValue;
                if (value.contains(JOKER)) {
                    value = value.replaceAll("\\" + JOKER, "%");
                    return Expr.like(fieldName, value);
                } else {
                    Expression expr = Expr.eq(fieldName, value);
                    if (value.isEmpty()) {
                        return Expr.or(expr, Expr.isNull(fieldName));
                    } else {
                        return expr;
                    }
                }
            }
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                if (sortStatusType == SortStatusType.DESC) {
                    orderby.desc(fieldName);
                } else {
                    orderby.asc(fieldName);
                }
            }
        }

        @Override
        public Object getFilterValueFromJson(JsonNode json) {
            return json.asText();
        }

        @Override
        public Object getDefaultFilterValueAsObject() {
            return getDefaultValue();
        }

        @Override
        public String toString() {
            return "TextFieldFilterComponent [defaultValue=" + defaultValue + "]";
        }

        @Override
        public boolean isKpi() {
            return false;
        }
    }

    /**
     * A filter component implemented as a numeric field.
     * 
     * @author Johann Kohler
     */
    public static class NumericFieldFilterComponent implements IFilterComponent {

        private String defaultValue;
        private String defaultComparator;

        /**
         * Default constructor.
         * 
         * @param defaultValue
         *            the default value (for comparison)
         * @param defaultComparator
         *            the default comparator
         */
        public NumericFieldFilterComponent(String defaultValue, String defaultComparator) {
            super();
            this.defaultValue = defaultValue;
            this.defaultComparator = defaultComparator;
        }

        /**
         * Get the default value.
         */
        public String getDefaultValue() {
            return defaultValue;
        }

        /**
         * Get the default comparator.
         */
        public String getDefaultComparator() {
            return defaultComparator;
        }

        @Override
        public void marshallMetaData(ObjectNode selectableColumnFilterComponentMetaData) {
            selectableColumnFilterComponentMetaData.put(SelectableColumn.JSON_TYPE_FIELD, FilterComponentType.NUMERIC.name());
            selectableColumnFilterComponentMetaData.put("defaultValue", getDefaultValue());
            selectableColumnFilterComponentMetaData.put("defaultComparator", getDefaultComparator());
        }

        @Override
        public void marshallFilterValue(ObjectNode userColumnConfiguration, Object filterValue) {
            if (!(filterValue instanceof String[])) {
                ObjectNode numeric = Json.newObject();
                numeric.put("value", getDefaultValue());
                numeric.put("comparator", getDefaultComparator());
                userColumnConfiguration.set(UserColumnConfiguration.JSON_FILTERVALUE_FIELD, numeric);
                log.error("The value " + filterValue + " returned for this filter component " + this + " is invalid, use default one instead");
            } else {
                String[] numericAsObject = (String[]) filterValue;
                ObjectNode numeric = Json.newObject();
                numeric.put("value", numericAsObject[0]);
                numeric.put("comparator", numericAsObject[1]);
                userColumnConfiguration.set(UserColumnConfiguration.JSON_FILTERVALUE_FIELD, numeric);
            }
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {
            if (filterValue != null) {
                String[] numericAsObject = (String[]) filterValue;
                String value = numericAsObject[0];
                String comparator = numericAsObject[1];

                if (comparator.equals("=")) {
                    return Expr.eq(fieldName, value);
                } else if (comparator.equals(">")) {
                    return Expr.gt(fieldName, value);
                } else if (comparator.equals(">=")) {
                    return Expr.ge(fieldName, value);
                } else if (comparator.equals("<")) {
                    return Expr.lt(fieldName, value);
                } else if (comparator.equals("<=")) {
                    return Expr.le(fieldName, value);
                } else if (comparator.equals("<>")) {
                    return Expr.ne(fieldName, value);
                }

            }
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                if (sortStatusType == SortStatusType.DESC) {
                    orderby.desc(fieldName);
                } else {
                    orderby.asc(fieldName);
                }
            }
        }

        @Override
        public Object getFilterValueFromJson(JsonNode json) {

            String value = json.get("value").asText();
            String comparator = json.get("comparator").asText();

            return new String[] { value, comparator };

        }

        @Override
        public Object getDefaultFilterValueAsObject() {
            return new String[] { getDefaultValue(), getDefaultComparator() };
        }

        @Override
        public String toString() {
            return "NumericFieldFilterComponent [defaultValue=" + getDefaultValue() + ", defaultComparator=" + getDefaultComparator() + "]";
        }

        @Override
        public boolean isKpi() {
            return false;
        }
    }

    /**
     * A filter component implemented as a range of date.<br/>
     * The search is to be performed between two date values
     * 
     * @author Pierre-Yves Cloux
     */
    public static class DateRangeFilterComponent implements IFilterComponent {
        private Date from;
        private Date to;
        private String format;

        /**
         * Default constructor.
         * 
         * @param from
         *            the default from date
         * @param to
         *            the default to date
         * @param format
         *            the default format
         */
        public DateRangeFilterComponent(Date from, Date to, String format) {
            super();
            this.from = from;
            this.to = to;
            this.format = format;
        }

        /**
         * Get the from date.
         */
        public Date getFrom() {
            return from;
        }

        /**
         * Get the to date.
         */
        public Date getTo() {
            return to;
        }

        /**
         * Get the format.
         */
        public String getFormat() {
            return format;
        }

        @Override
        public void marshallMetaData(ObjectNode selectableColumnFilterComponentMetaData) {
            selectableColumnFilterComponentMetaData.put(SelectableColumn.JSON_TYPE_FIELD, FilterComponentType.DATERANGE.name());
            DateFormat dateFormat = Utilities.getDateFormat(Utilities.JSON_DATE_FORMAT);
            selectableColumnFilterComponentMetaData.put("from", dateFormat.format(getFrom()));
            selectableColumnFilterComponentMetaData.put("to", dateFormat.format(getTo()));
            if (getFormat() == null) {
                selectableColumnFilterComponentMetaData.put("format", Utilities.getDefaultDatePattern().toLowerCase());
            } else {
                selectableColumnFilterComponentMetaData.put("format", getFormat().toLowerCase());
            }

        }

        @Override
        public void marshallFilterValue(ObjectNode userColumnConfiguration, Object filterValue) {
            if (!(filterValue instanceof Date[])) {
                ObjectNode dateRange = Json.newObject();
                DateFormat dateFormat = Utilities.getDateFormat(Utilities.JSON_DATE_FORMAT);
                dateRange.put("from", dateFormat.format(getFrom()));
                dateRange.put("to", dateFormat.format(getTo()));
                userColumnConfiguration.set(UserColumnConfiguration.JSON_FILTERVALUE_FIELD, dateRange);
                log.error("The value " + filterValue + " returned for this filter component " + this + " is invalid, use default one instead");
            } else {
                Date[] dateRangeAsObject = (Date[]) filterValue;
                ObjectNode dateRange = Json.newObject();
                DateFormat dateFormat = Utilities.getDateFormat(Utilities.JSON_DATE_FORMAT);
                dateRange.put("from", dateFormat.format(dateRangeAsObject[0]));
                dateRange.put("to", dateFormat.format(dateRangeAsObject[1]));
                userColumnConfiguration.set(UserColumnConfiguration.JSON_FILTERVALUE_FIELD, dateRange);
            }
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {
            if (filterValue != null) {
                Date[] fromTo = (Date[]) filterValue;

                Date to = fromTo[1];
                Calendar cal = Calendar.getInstance();
                cal.setTime(to);
                cal.set(Calendar.HOUR, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);

                return Expr.between(fieldName, fromTo[0], cal.getTime());
            }
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                if (sortStatusType == SortStatusType.DESC) {
                    orderby.desc(fieldName);
                } else {
                    orderby.asc(fieldName);
                }
            }
        }

        @Override
        public Object getFilterValueFromJson(JsonNode json) throws FilterConfigException {
            DateFormat dateFormat = Utilities.getDateFormat(Utilities.JSON_DATE_FORMAT);
            String fromDateAsString = json.get("from").asText();
            String toDateAsString = json.get("to").asText();

            Date fromDate = new Date();
            Date toDate = new Date();

            if (fromDateAsString != null && !fromDateAsString.equals("null")) {
                try {
                    fromDate = dateFormat.parse(fromDateAsString);
                } catch (ParseException e) {
                    throw new FilterConfigException("Error while converting a filter value in from date");
                }
            }

            if (toDateAsString != null && !toDateAsString.equals("null")) {
                try {
                    toDate = dateFormat.parse(toDateAsString);
                } catch (ParseException e) {
                    throw new FilterConfigException("Error while converting a filter value in to date");
                }
            }

            return new Date[] { fromDate, toDate };
        }

        @Override
        public Object getDefaultFilterValueAsObject() {
            return new Date[] { getFrom(), getTo() };
        }

        @Override
        public String toString() {
            return "DateRangeFilterComponent [from=" + from + ", to=" + to + ", format=" + format + "]";
        }

        @Override
        public boolean isKpi() {
            return false;
        }
    }

    /**
     * A filter component implemented as dropdown.
     * 
     * @author Johann kohler
     *
     */
    public static class SelectFilterComponent implements IFilterComponent {
        private List<String> defaultValue;
        private ISelectableValueHolderCollection<String> values;
        private String[] fieldsSort;

        /**
         * Default constructor for "Long" case.
         * 
         * @param defaultValue
         *            the default value
         * @param values
         *            the possible values
         */
        public SelectFilterComponent(Long defaultValue, ISelectableValueHolderCollection<Long> values) {
            super();

            this.defaultValue = new ArrayList<>();
            if (defaultValue != null) {
                this.defaultValue.add(String.valueOf(defaultValue));
            }
            this.values = new DefaultSelectableValueHolderCollection<String>();
            int order = 0;
            for (ISelectableValueHolder<Long> value : values.getSortedValues()) {
                DefaultSelectableValueHolder<String> valueHolder = new DefaultSelectableValueHolder<String>(String.valueOf(value.getValue()), value.getName(),
                        value.getDescription(), value.getUrl());
                valueHolder.setOrder(order);
                order++;
                this.values.add(valueHolder);
            }
            this.fieldsSort = null;
        }

        /**
         * Default constructor for "Long" case with sort of the fields.
         * 
         * @param defaultValue
         *            the default value
         * @param values
         *            the possible values
         * @param fieldsSort
         *            the sort of the fields
         */
        public SelectFilterComponent(Long defaultValue, ISelectableValueHolderCollection<Long> values, String[] fieldsSort) {
            this(defaultValue, values);
            this.fieldsSort = fieldsSort;
        }

        /**
         * Default constructor for "String" case.
         * 
         * @param defaultValue
         *            the default value
         * @param values
         *            the possible values
         */
        public SelectFilterComponent(String defaultValue, ISelectableValueHolderCollection<String> values) {
            super();
            this.defaultValue = new ArrayList<>();
            if (defaultValue != null) {
                this.defaultValue.add(defaultValue);
            }
            this.values = values;
            this.fieldsSort = null;
        }

        /**
         * Default constructor for "String" case with sort of the fields.
         * 
         * @param defaultValue
         *            the default value
         * @param values
         *            the possible values
         * @param fieldsSort
         *            the sort of the fields
         */
        public SelectFilterComponent(String defaultValue, ISelectableValueHolderCollection<String> values, String[] fieldsSort) {
            this(defaultValue, values);
            this.fieldsSort = fieldsSort;
        }

        /**
         * Get the possible values.
         */
        public ISelectableValueHolderCollection<String> getValues() {
            return values;
        }

        @Override
        public void marshallMetaData(ObjectNode selectableColumnFilterComponentMetaData) {
            selectableColumnFilterComponentMetaData.put(SelectableColumn.JSON_TYPE_FIELD, FilterComponentType.SELECT.name());

            ObjectMapper mapper = new ObjectMapper();
            ArrayNode array = mapper.valueToTree(this.defaultValue);
            selectableColumnFilterComponentMetaData.putArray("defaultValue").addAll(array);

            selectableColumnFilterComponentMetaData.set("values", Utilities.marshallAsJson(getValues().getSortedValues()));
        }

        @Override
        public void marshallFilterValue(ObjectNode userColumnConfiguration, Object filterValue) {

            ObjectMapper mapper = new ObjectMapper();

            if (!(filterValue instanceof List)) {

                ArrayNode array = mapper.valueToTree(this.defaultValue);
                userColumnConfiguration.putArray(UserColumnConfiguration.JSON_FILTERVALUE_FIELD).addAll(array);

                log.error("The value " + filterValue + " returned for this filter component " + this + " is invalid, use default one instead");
            } else {

                ArrayNode array = mapper.valueToTree(filterValue);
                userColumnConfiguration.putArray(UserColumnConfiguration.JSON_FILTERVALUE_FIELD).addAll(array);

            }
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {

            if (filterValue != null) {

                @SuppressWarnings("unchecked")
                List<String> listValue = (List<String>) filterValue;

                if (listValue.size() > 0) {
                    return Expr.in(fieldName, listValue);
                } else {
                    return Expr.isNull(fieldName);
                }

            }
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                if (this.fieldsSort == null) {
                    if (sortStatusType == SortStatusType.DESC) {
                        orderby.desc(fieldName);
                    } else {
                        orderby.asc(fieldName);
                    }
                } else {
                    for (String fieldSort : this.fieldsSort) {
                        if (sortStatusType == SortStatusType.DESC) {
                            orderby = orderby.desc(fieldSort).order();
                        } else {
                            orderby = orderby.asc(fieldSort).order();
                        }

                    }
                }
            }
        }

        @Override
        public Object getFilterValueFromJson(JsonNode json) {

            List<String> r = new ArrayList<>();

            for (JsonNode elem : json) {
                r.add(elem.asText());
            }

            return r;
        }

        @Override
        public Object getDefaultFilterValueAsObject() {
            return this.defaultValue;
        }

        @Override
        public String toString() {
            return "SelectFilterComponent [defaultValue=" + defaultValue + ", values=" + values + "]";
        }

        @Override
        public boolean isKpi() {
            return false;
        }
    }
    
    /**
     * A filter component which "unactivates" the database filtering and sorting capability while the rest of the
     * filter remains functional.
     * @author Pierre-Yves Cloux
     */
    public static class NoDbFilterComponentWrapper implements IFilterComponent{
    	private IFilterComponent wrappedFilterComponent;
    	
    	public NoDbFilterComponentWrapper(IFilterComponent wrappedFilterComponent) {
			this.wrappedFilterComponent=wrappedFilterComponent;
		}

		@Override
		public void marshallMetaData(ObjectNode selectableColumnFilterComponentMetaData) {
			getWrappedFilterComponent().marshallMetaData(selectableColumnFilterComponentMetaData);
		}

		@Override
		public void marshallFilterValue(ObjectNode userColumnConfiguration, Object filterValue) {
			getWrappedFilterComponent().marshallFilterValue(userColumnConfiguration, filterValue);
		}

		@Override
		public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {
			return null;
		}

		@Override
		public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
		}

		@Override
		public Object getFilterValueFromJson(JsonNode json) throws FilterConfigException {
			return getWrappedFilterComponent().getFilterValueFromJson(json);
		}

		@Override
		public Object getDefaultFilterValueAsObject() {
			return getWrappedFilterComponent().getDefaultFilterValueAsObject();
		}

		@Override
		public boolean isKpi() {
			return getWrappedFilterComponent().isKpi();
		}

		private IFilterComponent getWrappedFilterComponent() {
			return wrappedFilterComponent;
		}
    }

    /**
     * A filter component implemented as an autocomplete field.
     * 
     * @author Johann kohler
     *
     */
    public static class AutocompleteFilterComponent implements IFilterComponent {
        private String url;
        private String[] fieldsSort;

        /**
         * Default constructor.
         * 
         * @param url
         *            the URL of the data provider (for autocomplete search)
         */
        public AutocompleteFilterComponent(String url) {
            super();
            this.url = url;
            this.fieldsSort = null;
        }

        /**
         * Default constructor.
         * 
         * @param url
         *            the URL of the data provider (for autocomplete search)
         * @param fieldsSort
         *            the sort of the fields
         */
        public AutocompleteFilterComponent(String url, String[] fieldsSort) {
            this(url);
            this.fieldsSort = fieldsSort;
        }

        @Override
        public void marshallMetaData(ObjectNode selectableColumnFilterComponentMetaData) {
            selectableColumnFilterComponentMetaData.put(SelectableColumn.JSON_TYPE_FIELD, FilterComponentType.AUTOCOMPLETE.name());
            selectableColumnFilterComponentMetaData.put("url", getUrl());
        }

        @Override
        public void marshallFilterValue(ObjectNode userColumnConfiguration, Object filterValue) {
            if (!(filterValue instanceof Object[])) {
                Object[] defalutValues = (Object[]) getDefaultFilterValueAsObject();
                ObjectNode jsonValue = Json.newObject();
                jsonValue.put("value", (Long) defalutValues[0]);
                jsonValue.put("content", (String) defalutValues[1]);
                userColumnConfiguration.set(UserColumnConfiguration.JSON_FILTERVALUE_FIELD, jsonValue);
                log.error("The value " + filterValue + " returned for this filter component " + this + " is invalid, use default one instead");
            } else {
                Object[] value = (Object[]) filterValue;
                ObjectNode jsonValue = Json.newObject();
                jsonValue.put("value", (Long) value[0]);
                jsonValue.put("content", (String) value[1]);
                userColumnConfiguration.set(UserColumnConfiguration.JSON_FILTERVALUE_FIELD, jsonValue);
            }
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {
            if (filterValue != null) {
                Object[] value = (Object[]) filterValue;
                Long aLongValue = (Long) value[0];
                if (aLongValue != null && !aLongValue.equals(0L)) {
                    return Expr.eq(fieldName, aLongValue);
                } else {
                    return Expr.isNull(fieldName);
                }
            }
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {

            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                if (this.fieldsSort == null) {
                    if (sortStatusType == SortStatusType.DESC) {
                        orderby.desc(fieldName);
                    } else {
                        orderby.asc(fieldName);
                    }
                } else {
                    for (String fieldSort : this.fieldsSort) {
                        if (sortStatusType == SortStatusType.DESC) {
                            orderby = orderby.desc(fieldSort).order();
                        } else {
                            orderby = orderby.asc(fieldSort).order();
                        }

                    }
                }
            }
        }

        @Override
        public Object getFilterValueFromJson(JsonNode json) {
            return new Object[] { json.get("value").asLong(), json.get("content").asText() };
        }

        @Override
        public Object getDefaultFilterValueAsObject() {
            return new Object[] { 0L, "" };
        }

        /**
         * Get the URL.
         */
        private String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return "AutocompleteFilterComponent [url=" + url + "]";
        }

        @Override
        public boolean isKpi() {
            return false;
        }
    }

    /**
     * The none filter componenent.
     * 
     * @author Johann Kohler
     *
     */
    public static class NoneFilterComponent implements IFilterComponent {
        @Override
        public void marshallMetaData(ObjectNode selectableColumnFilterComponentMetaData) {
            selectableColumnFilterComponentMetaData.put(SelectableColumn.JSON_TYPE_FIELD, FilterComponentType.NONE.name());
        }

        @Override
        public void marshallFilterValue(ObjectNode userColumnConfiguration, Object filterValue) {
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
        }

        @Override
        public Object getFilterValueFromJson(JsonNode json) {
            return null;
        }

        @Override
        public Object getDefaultFilterValueAsObject() {
            return null;
        }

        @Override
        public boolean isKpi() {
            return false;
        }
    }

    /**
     * A filter component for a custom attribute value which is a string.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class StringCustomAttributeFilterComponent extends TextFieldFilterComponent {
        private static final String SEARCH_EXPRESSION_TEMPLATE = "(select count(*) from string_custom_attribute_value as cust%2$s "
                + "where cust%2$s.deleted=0 and cust%2$s.object_type='%1$s' "
                + "and cust%2$s.object_id=t0.id and cust%2$s.custom_attribute_definition_id=%2$s and cust%2$s.value%3$s)<>0";

        private static final String SORT_EXPRESSION_TEMPLATE = "(select sortcust%2$s.value from string_custom_attribute_value as sortcust%2$s "
                + "where sortcust%2$s.deleted=0 "
                + "and sortcust%2$s.object_type='%1$s' and sortcust%2$s.object_id=t0.id and sortcust%2$s.custom_attribute_definition_id=%2$s)";

        private CustomAttributeDefinition customAttributeDefinition;

        /**
         * Default constructor.
         * 
         * @param defaultValue
         *            the default value
         * @param customAttributeDefinition
         *            the custom attribute definition
         */
        public StringCustomAttributeFilterComponent(String defaultValue, CustomAttributeDefinition customAttributeDefinition) {
            super(defaultValue);
            this.customAttributeDefinition = customAttributeDefinition;
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {
            if (filterValue != null) {
                String value = (String) filterValue;
                if (value.contains(JOKER)) {
                    value = value.replaceAll("\\" + JOKER, "%");
                    String sql = String.format(SEARCH_EXPRESSION_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id,
                            " like '" + value + "' or '" + value + "' = ''");
                    return Expr.raw(sql);
                } else {
                    String sql = String.format(SEARCH_EXPRESSION_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id,
                            "='" + value + "'");
                    return Expr.raw(sql);
                }
            }
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                String template = String.format(SORT_EXPRESSION_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id);
                if (sortStatusType == SortStatusType.DESC) {
                    orderby.desc(template);
                } else {
                    orderby.asc(template);
                }
            }
        }

        /**
         * Get the custom attribute definition.
         */
        private CustomAttributeDefinition getCustomAttributeDefinition() {
            return customAttributeDefinition;
        }
    }

    /**
     * A custom attribute filter component for a custom attribute whichz is a
     * text (long string).
     * 
     * @author Pierre-Yves Cloux
     */
    public static class TextCustomAttributeFilterComponent extends TextFieldFilterComponent {
        private static final String SEARCH_EXPRESSION_TEMPLATE = "(select count(*) from text_custom_attribute_value as cust%2$s "
                + "where cust%2$s.deleted=0 and cust%2$s.object_type='%1$s' and cust%2$s.object_id=t0.id and cust%2$s.custom_attribute_definition_id=%2$s "
                + "and CAST(cust%2$s.value AS CHAR(10000) CHARACTER SET utf8)%3$s)<>0";

        private static final String SORT_EXPRESSION_TEMPLATE = "(select sortcust%2$s.value from text_custom_attribute_value as sortcust%2$s "
                + "where sortcust%2$s.deleted=0 and sortcust%2$s.object_type='%1$s' and sortcust%2$s.object_id=t0.id "
                + "and sortcust%2$s.custom_attribute_definition_id=%2$s)";

        private CustomAttributeDefinition customAttributeDefinition;

        /**
         * Default constructor.
         * 
         * @param defaultValue
         *            the default value
         * @param customAttributeDefinition
         *            the custom attribute definition
         */
        public TextCustomAttributeFilterComponent(String defaultValue, CustomAttributeDefinition customAttributeDefinition) {
            super(defaultValue);
            this.customAttributeDefinition = customAttributeDefinition;
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {
            if (filterValue != null) {
                String value = (String) filterValue;
                if (value.contains(JOKER)) {
                    value = value.replaceAll("\\" + JOKER, "%");
                    String sql = String.format(SEARCH_EXPRESSION_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id,
                            " like '" + value + "'");
                    return Expr.raw(sql);
                } else {
                    String sql = String.format(SEARCH_EXPRESSION_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id,
                            "='" + value + "'");
                    return Expr.raw(sql);
                }
            }
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                String template = String.format(SORT_EXPRESSION_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id);
                if (sortStatusType == SortStatusType.DESC) {
                    orderby.desc(template);
                } else {
                    orderby.asc(template);
                }
            }
        }

        /**
         * Get the custom attribute definition.
         */
        private CustomAttributeDefinition getCustomAttributeDefinition() {
            return customAttributeDefinition;
        }
    }

    /**
     * A filter component for a boolean custom attribute.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class BooleanCustomAttributeFilterComponent extends CheckboxFilterComponent {
        private static final String SEARCH_EXPRESSION_TEMPLATE = "(select count(*) from boolean_custom_attribute_value as cust%2$s "
                + "where cust%2$s.deleted=0 and cust%2$s.object_type='%1$s' and cust%2$s.object_id=t0.id and cust%2$s.custom_attribute_definition_id=%2$s "
                + "and cust%2$s.value=%3$s)<>0 or %3$s = 0";

        private static final String SORT_EXPRESSION_TEMPLATE = "(select #value# from boolean_custom_attribute_value as sortcust%2$s "
                + "where sortcust%2$s.deleted=0 and sortcust%2$s.object_type='%1$s' and sortcust%2$s.object_id=t0.id "
                + "and sortcust%2$s.custom_attribute_definition_id=%2$s)";
        private static final String SORT_EXPRESSION_TEMPLATE_ASC = "sortcust%2$s.value";
        private static final String SORT_EXPRESSION_TEMPLATE_DESC = "(1 - sortcust%2$s.value)";

        private CustomAttributeDefinition customAttributeDefinition;

        /**
         * Default constructor.
         * 
         * @param defaultValue
         *            the default value
         * @param customAttributeDefinition
         *            the custom attribute definition
         */
        public BooleanCustomAttributeFilterComponent(boolean defaultValue, CustomAttributeDefinition customAttributeDefinition) {
            super(defaultValue);
            this.customAttributeDefinition = customAttributeDefinition;
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {
            if (filterValue != null) {
                int value = 0;
                if ((Boolean) filterValue) {
                    value = 1;
                }
                String sql = String.format(SEARCH_EXPRESSION_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id,
                        String.valueOf(value));
                return Expr.raw(sql);
            }
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                String template = null;
                if (sortStatusType == SortStatusType.DESC) {
                    template = SORT_EXPRESSION_TEMPLATE.replace("#value#", SORT_EXPRESSION_TEMPLATE_DESC);
                } else {
                    template = SORT_EXPRESSION_TEMPLATE.replace("#value#", SORT_EXPRESSION_TEMPLATE_ASC);
                }
                orderby.asc(String.format(template, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id));
            }
        }

        /**
         * Get the custom attribute definition.
         */
        private CustomAttributeDefinition getCustomAttributeDefinition() {
            return customAttributeDefinition;
        }
    }

    /**
     * A filter component for a data custom attribute.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class DateCustomAttributeFilterComponent extends DateRangeFilterComponent {

        private static final String MYSQL_DATETIME_FROM = "yyyy-MM-dd 00:00:00";
        private static final String MYSQL_DATETIME_TO = "yyyy-MM-dd 23:59:59";
        private static final String SEARCH_EXPRESSION_TEMPLATE = "(select count(*) from date_custom_attribute_value as cust%2$s "
                + "where cust%2$s.deleted=0 and cust%2$s.object_type='%1$s' and cust%2$s.object_id=t0.id and cust%2$s.custom_attribute_definition_id=%2$s "
                + "and (cust%2$s.value between '%3$s' and '%4$s'))<>0";

        private static final String SORT_EXPRESSION_TEMPLATE = "(select #value# from date_custom_attribute_value as sortcust%2$s "
                + "where sortcust%2$s.deleted=0 and sortcust%2$s.object_type='%1$s' and sortcust%2$s.object_id=t0.id "
                + "and sortcust%2$s.custom_attribute_definition_id=%2$s)";
        private static final String SORT_EXPRESSION_TEMPLATE_ASC = "sortcust%2$s.value";
        private static final String SORT_EXPRESSION_TEMPLATE_DESC = "DATEDIFF(NOW(), sortcust%2$s.value)";

        private CustomAttributeDefinition customAttributeDefinition;

        /**
         * Default constructor.
         * 
         * @param from
         *            the from date
         * @param to
         *            the to date
         * @param format
         *            the date format
         * @param customAttributeDefinition
         *            the custom attribute definition
         */
        public DateCustomAttributeFilterComponent(Date from, Date to, String format, CustomAttributeDefinition customAttributeDefinition) {
            super(from, to, format);
            this.customAttributeDefinition = customAttributeDefinition;
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {
            if (filterValue != null) {
                Date from = ((Date[]) filterValue)[0];
                Date to = ((Date[]) filterValue)[1];
                String sql = String.format(SEARCH_EXPRESSION_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id,
                        Utilities.getDateFormat(MYSQL_DATETIME_FROM).format(from), Utilities.getDateFormat(MYSQL_DATETIME_TO).format(to));
                return Expr.raw(sql);
            }
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                String template = null;
                if (sortStatusType == SortStatusType.DESC) {
                    template = SORT_EXPRESSION_TEMPLATE.replace("#value#", SORT_EXPRESSION_TEMPLATE_DESC);
                } else {
                    template = SORT_EXPRESSION_TEMPLATE.replace("#value#", SORT_EXPRESSION_TEMPLATE_ASC);
                }
                orderby.asc(String.format(template, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id));
            }
        }

        /**
         * Get the custom attribute definition.
         */
        private CustomAttributeDefinition getCustomAttributeDefinition() {
            return customAttributeDefinition;
        }
    }

    /**
     * A filter component for an integer custom attribute.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class IntegerCustomAttributeFilterComponent extends NumericFieldFilterComponent {
        private static final String SEARCH_EXPRESSION_TEMPLATE = "(select count(*) from integer_custom_attribute_value as cust%2$s "
                + "where cust%2$s.deleted=0 and cust%2$s.object_type='%1$s' and cust%2$s.object_id=t0.id and cust%2$s.custom_attribute_definition_id=%2$s "
                + "and cust%2$s.value %4$s %3$s)<>0 or %3$s = 0";

        private static final String SORT_EXPRESSION_TEMPLATE = "(select #value# from integer_custom_attribute_value as sortcust%2$s "
                + "where sortcust%2$s.deleted=0 and sortcust%2$s.object_type='%1$s' and sortcust%2$s.object_id=t0.id "
                + "and sortcust%2$s.custom_attribute_definition_id=%2$s)";
        private static final String SORT_EXPRESSION_TEMPLATE_ASC = "sortcust%2$s.value";
        private static final String SORT_EXPRESSION_TEMPLATE_DESC = "(1 - sortcust%2$s.value)";

        private CustomAttributeDefinition customAttributeDefinition;

        /**
         * Default constructor.
         * 
         * @param defaultValue
         *            the default value
         * @param defaultComparator
         *            the default comparator
         * @param customAttributeDefinition
         *            the custom attribute definition
         */
        public IntegerCustomAttributeFilterComponent(String defaultValue, String defaultComparator, CustomAttributeDefinition customAttributeDefinition) {
            super(defaultValue, defaultComparator);
            this.customAttributeDefinition = customAttributeDefinition;
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {
            if (filterValue != null) {

                BigDecimal value = new BigDecimal(0);
                String comparator = ((String[]) filterValue)[1];
                try {
                    value = new BigDecimal(((String[]) filterValue)[0]);
                } catch (NumberFormatException e) {
                    Logger.warn("impossible to convert '" + filterValue + "' to a BigDecimal");
                }
                String sql = String.format(SEARCH_EXPRESSION_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id,
                        value.toPlainString(), comparator);
                return Expr.raw(sql);
            }
            return null;

        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                String template = null;
                if (sortStatusType == SortStatusType.DESC) {
                    template = SORT_EXPRESSION_TEMPLATE.replace("#value#", SORT_EXPRESSION_TEMPLATE_DESC);
                } else {
                    template = SORT_EXPRESSION_TEMPLATE.replace("#value#", SORT_EXPRESSION_TEMPLATE_ASC);
                }
                orderby.asc(String.format(template, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id));
            }
        }

        /**
         * Get the custom attribute definition.
         */
        private CustomAttributeDefinition getCustomAttributeDefinition() {
            return customAttributeDefinition;
        }
    }

    /**
     * A filter component for a decimal custom attribute.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class DecimalCustomAttributeFilterComponent extends NumericFieldFilterComponent {
        private static final String SEARCH_EXPRESSION_TEMPLATE = "(select count(*) from decimal_custom_attribute_value as cust%2$s "
                + "where cust%2$s.deleted=0 and cust%2$s.object_type='%1$s' and cust%2$s.object_id=t0.id and cust%2$s.custom_attribute_definition_id=%2$s "
                + "and cust%2$s.value %4$s %3$s)<>0 or %3$s = 0";

        private static final String SORT_EXPRESSION_TEMPLATE = "(select #value# from decimal_custom_attribute_value as sortcust%2$s "
                + "where sortcust%2$s.deleted=0 and sortcust%2$s.object_type='%1$s' and sortcust%2$s.object_id=t0.id "
                + "and sortcust%2$s.custom_attribute_definition_id=%2$s)";
        private static final String SORT_EXPRESSION_TEMPLATE_ASC = "sortcust%2$s.value";
        private static final String SORT_EXPRESSION_TEMPLATE_DESC = "(1 - sortcust%2$s.value)";

        private CustomAttributeDefinition customAttributeDefinition;

        /**
         * Default constructor.
         * 
         * @param defaultValue
         *            the default value
         * @param defaultComparator
         *            the default comparator
         * @param customAttributeDefinition
         *            the custom attribute definition
         */
        public DecimalCustomAttributeFilterComponent(String defaultValue, String defaultComparator, CustomAttributeDefinition customAttributeDefinition) {
            super(defaultValue, defaultComparator);
            this.customAttributeDefinition = customAttributeDefinition;
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {

            if (filterValue != null) {

                BigDecimal value = new BigDecimal(0);
                String comparator = ((String[]) filterValue)[1];
                try {
                    value = new BigDecimal(((String[]) filterValue)[0]);
                } catch (NumberFormatException e) {
                    Logger.warn("impossible to convert '" + filterValue + "' to a BigDecimal");
                }
                String sql = String.format(SEARCH_EXPRESSION_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id,
                        value.toPlainString(), comparator);
                return Expr.raw(sql);
            }
            return null;

        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                String template = null;
                if (sortStatusType == SortStatusType.DESC) {
                    template = SORT_EXPRESSION_TEMPLATE.replace("#value#", SORT_EXPRESSION_TEMPLATE_DESC);
                } else {
                    template = SORT_EXPRESSION_TEMPLATE.replace("#value#", SORT_EXPRESSION_TEMPLATE_ASC);
                }
                orderby.asc(String.format(template, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id));
            }
        }

        /**
         * Get the custom attribute definition.
         */
        private CustomAttributeDefinition getCustomAttributeDefinition() {
            return customAttributeDefinition;
        }
    }

    /**
     * A filter component for for a single item custom attribute.
     * 
     * @author Pierre-Yves Cloux
     */
    public static class SingleItemCustomAttributeFilterComponent extends SelectFilterComponent {
        private static final String SEARCH_EXPRESSION_TEMPLATE = "(select count(*) from single_item_custom_attribute_value as cust%2$s "
                + "where cust%2$s.deleted=0 and cust%2$s.object_type='%1$s' and cust%2$s.object_id=t0.id and cust%2$s.custom_attribute_definition_id=%2$s "
                + "and cust%2$s.value_id IN %3$s)<>0";

        private static final String SEARCH_EXPRESSION_NULL_CASE_TEMPLATE = "(select count(*) from single_item_custom_attribute_value as cust%2$s "
                + "where cust%2$s.deleted=0 and cust%2$s.object_type='%1$s' and cust%2$s.object_id=t0.id and cust%2$s.custom_attribute_definition_id=%2$s "
                + "and cust%2$s.value_id IS NULL)<>0";

        private static final String SORT_EXPRESSION_TEMPLATE = "(select #value# from single_item_custom_attribute_value as sortcust%2$s"
                + " join custom_attribute_item_option as sortcust%2$s_option on sortcust%2$s_option.id=sortcust%2$s.value_id"
                + " where sortcust%2$s.deleted=0 and sortcust%2$s.object_type='%1$s' and sortcust%2$s.object_id=t0.id "
                + "and sortcust%2$s.custom_attribute_definition_id=%2$s)";

        private CustomAttributeDefinition customAttributeDefinition;

        /**
         * Default constructor.
         * 
         * @param customAttributeDefinition
         *            the custom attribute definition
         */
        public SingleItemCustomAttributeFilterComponent(CustomAttributeDefinition customAttributeDefinition) {
            super(null, customAttributeDefinition.getValueHoldersCollectionForSingleItemCustomAttribute());
            this.customAttributeDefinition = customAttributeDefinition;
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {

            if (filterValue != null) {

                @SuppressWarnings("unchecked")
                List<String> listValue = (List<String>) filterValue;

                String sql = null;

                if (listValue.size() > 0) {
                    String value = "(" + String.join(",", listValue) + ")";
                    sql = String.format(SEARCH_EXPRESSION_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id, value);
                } else {
                    sql = String.format(SEARCH_EXPRESSION_NULL_CASE_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id);
                }

                return Expr.raw(sql);
            }
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                String template = String.format(SORT_EXPRESSION_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id);
                if (sortStatusType == SortStatusType.DESC) {
                    orderby.desc(template);
                } else {
                    orderby.asc(template);
                }
            }
        }

        /**
         * Get the custom attribute definition.
         */
        private CustomAttributeDefinition getCustomAttributeDefinition() {
            return customAttributeDefinition;
        }
    }

    /**
     * A filter component for for a multi item custom attribute.
     * 
     * @author Johann Kohler
     */
    public static class MultiItemCustomAttributeFilterComponent extends SelectFilterComponent {

        private static final String SEARCH_EXPRESSION_TEMPLATE = "(SELECT COUNT(*) FROM multi_item_ca_value_has_ca_multi_item_option ca_value_item%2$s"
                + " JOIN multi_item_custom_attribute_value ca_value%2$s ON ca_value_item%2$s.multi_item_custom_attribute_value_id=ca_value%2$s.id"
                + " WHERE ca_value%2$s.deleted=0 AND ca_value%2$s.object_type='%1$s'"
                + " AND ca_value%2$s.object_id=t0.id AND ca_value%2$s.custom_attribute_definition_id=%2$s"
                + " AND ca_value_item%2$s.custom_attribute_multi_item_option_id IN %3$s)<>0";

        private CustomAttributeDefinition customAttributeDefinition;

        /**
         * Default constructor.
         * 
         * @param customAttributeDefinition
         *            the custom attribute definition
         */
        public MultiItemCustomAttributeFilterComponent(CustomAttributeDefinition customAttributeDefinition) {
            super(null, customAttributeDefinition.getValueHoldersCollectionForMultiItemCustomAttribute());
            this.customAttributeDefinition = customAttributeDefinition;
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {

            if (filterValue != null) {

                @SuppressWarnings("unchecked")
                List<String> listValue = (List<String>) filterValue;

                String sql = null;

                if (listValue.size() > 0) {
                    String value = "(" + String.join(",", listValue) + ")";
                    sql = String.format(SEARCH_EXPRESSION_TEMPLATE, getCustomAttributeDefinition().objectType, getCustomAttributeDefinition().id, value);
                } else {
                    sql = "0=1";
                }

                return Expr.raw(sql);
            }

            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
        }

        /**
         * Get the custom attribute definition.
         */
        private CustomAttributeDefinition getCustomAttributeDefinition() {
            return customAttributeDefinition;
        }
    }

    /**
     * A filter component for KPI (when displayed as a value).
     * 
     * @author Johann Kohler
     */
    public static class KpiNumericFilterComponent extends NumericFieldFilterComponent {

        private static final String SEARCH_EXPRESSION_TEMPLATE = "(SELECT count(*) from kpi_data kdata%1$s"
                + " JOIN kpi_value_definition kvd%1$s ON kdata%1$s.kpi_value_definition_id = kvd%1$s.id"
                + " JOIN kpi_definition kd%1$s ON kvd%1$s.id = kd%1$s.%4$s"
                + " WHERE kdata%1$s.deleted = 0 AND kvd%1$s.deleted = 0 AND kd%1$s.deleted = 0 AND"
                + " kdata%1$s.timestamp = (SELECT MAX(kdata_i%1$s.timestamp) FROM kpi_data kdata_i%1$s"
                + " WHERE kdata_i%1$s.kpi_value_definition_id = kvd%1$s.id AND kdata_i%1$s.object_id = kdata%1$s.object_id)"
                + " AND kd%1$s.uid = '%1$s' AND kdata%1$s.object_id = t0.id AND kdata%1$s.value %3$s %2$s)<>0";

        private static final String SORT_EXPRESSION_TEMPLATE = "(SELECT #value# from kpi_data kdatasort%1$s"
                + " JOIN kpi_value_definition kvdsort%1$s ON kdatasort%1$s.kpi_value_definition_id = kvdsort%1$s.id"
                + " JOIN kpi_definition kdsort%1$s ON kvdsort%1$s.id = kdsort%1$s.%2$s"
                + " WHERE kdatasort%1$s.deleted = 0 AND kvdsort%1$s.deleted = 0 AND kdsort%1$s.deleted = 0 AND"
                + " kdatasort%1$s.timestamp = (SELECT MAX(kdata_isort%1$s.timestamp) FROM kpi_data kdata_isort%1$s"
                + " WHERE kdata_isort%1$s.kpi_value_definition_id = kvdsort%1$s.id AND kdata_isort%1$s.object_id = kdatasort%1$s.object_id)"
                + " AND kdsort%1$s.uid = '%1$s' AND kdatasort%1$s.object_id = t0.id)";

        private static final String SORT_EXPRESSION_TEMPLATE_ASC = "kdatasort%1$s.value";
        private static final String SORT_EXPRESSION_TEMPLATE_DESC = "(1 - kdatasort%1$s.value)";

        private DataType dataType;
        private Kpi kpi;

        /**
         * Default constructor.
         * 
         * @param dataType
         *            the data type
         * @param defaultValue
         *            the default value
         * @param defaultComparator
         *            the default comparator
         * @param kpi
         *            the KPI
         */
        public KpiNumericFilterComponent(DataType dataType, String defaultValue, String defaultComparator, Kpi kpi) {
            super(defaultValue, defaultComparator);
            this.dataType = dataType;
            this.kpi = kpi;
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {

            if (filterValue != null) {
                BigDecimal value = new BigDecimal(0);
                String comparator = ((String[]) filterValue)[1];
                try {
                    value = new BigDecimal(((String[]) filterValue)[0]);
                } catch (NumberFormatException e) {
                    Logger.warn("impossible to convert '" + filterValue + "' to a BigDecimal");
                }
                String sql = String.format(SEARCH_EXPRESSION_TEMPLATE, getKpi().getUid(), value.toPlainString(), comparator, this.dataType.getIdFieldName());
                return Expr.raw(sql);
            }
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                String template = null;
                if (sortStatusType == SortStatusType.DESC) {
                    template = SORT_EXPRESSION_TEMPLATE.replace("#value#", SORT_EXPRESSION_TEMPLATE_DESC);
                } else {
                    template = SORT_EXPRESSION_TEMPLATE.replace("#value#", SORT_EXPRESSION_TEMPLATE_ASC);
                }
                orderby.asc(String.format(template, getKpi().getUid(), this.dataType.getIdFieldName()));
            }

        }

        /**
         * Get the KPI.
         */
        private Kpi getKpi() {
            return kpi;
        }

        @Override
        public boolean isKpi() {
            return true;
        }
    }

    /**
     * A filter component for KPI (when displayed as a label).
     * 
     * @author Johann Kohler
     */
    public static class KpiSelectFilterComponent extends SelectFilterComponent {

        private static final String SEARCH_EXPRESSION_TEMPLATE = "(SELECT count(*) from kpi_data kdata%1$s"
                + " JOIN kpi_value_definition kvd%1$s ON kdata%1$s.kpi_value_definition_id = kvd%1$s.id"
                + " JOIN kpi_definition kd%1$s ON kvd%1$s.id = kd%1$s.%3$s"
                + " WHERE kdata%1$s.deleted = 0 AND kvd%1$s.deleted = 0 AND kd%1$s.deleted = 0 AND"
                + " kdata%1$s.timestamp = (SELECT MAX(kdata_i%1$s.timestamp) FROM kpi_data kdata_i%1$s"
                + " WHERE kdata_i%1$s.kpi_value_definition_id = kvd%1$s.id AND kdata_i%1$s.object_id = kdata%1$s.object_id)"
                + " AND kd%1$s.uid = '%1$s' AND kdata%1$s.object_id = t0.id AND kdata%1$s.kpi_color_rule_id IN %2$s)<>0";

        private static final String SORT_EXPRESSION_TEMPLATE = "(SELECT #value# from kpi_data kdatasort%1$s"
                + " JOIN kpi_value_definition kvdsort%1$s ON kdatasort%1$s.kpi_value_definition_id = kvdsort%1$s.id"
                + " JOIN kpi_definition kdsort%1$s ON kvdsort%1$s.id = kdsort%1$s.%2$s"
                + " WHERE kdatasort%1$s.deleted = 0 AND kvdsort%1$s.deleted = 0 AND kdsort%1$s.deleted = 0 AND"
                + " kdatasort%1$s.timestamp = (SELECT MAX(kdata_isort%1$s.timestamp) FROM kpi_data kdata_isort%1$s"
                + " WHERE kdata_isort%1$s.kpi_value_definition_id = kvdsort%1$s.id AND kdata_isort%1$s.object_id = kdatasort%1$s.object_id)"
                + " AND kdsort%1$s.uid = '%1$s' AND kdatasort%1$s.object_id = t0.id)";

        private static final String SORT_EXPRESSION_TEMPLATE_ASC = "kdatasort%1$s.kpi_color_rule_id";
        private static final String SORT_EXPRESSION_TEMPLATE_DESC = "(1 - kdatasort%1$s.kpi_color_rule_id)";

        private DataType dataType;
        private Kpi kpi;

        /**
         * Default constructor.
         * 
         * @param dataType
         *            the data type
         * @param defaultValue
         *            the default value
         * @param rules
         *            the possible rules
         * @param kpi
         *            the KPI
         */
        public KpiSelectFilterComponent(DataType dataType, Long defaultValue, ISelectableValueHolderCollection<Long> rules, Kpi kpi) {
            super(defaultValue, rules);
            this.dataType = dataType;
            this.kpi = kpi;
        }

        @Override
        public Expression getEBeanSearchExpression(Object filterValue, String fieldName) {

            if (filterValue != null) {

                @SuppressWarnings("unchecked")
                List<String> listValue = (List<String>) filterValue;

                // remove the not number elems (occurs when the render type has
                // changed)
                List<String> finalListValue = new ArrayList<>();
                for (String lv : listValue) {
                    try {
                        Double.parseDouble(lv);
                        finalListValue.add(lv);
                    } catch (NumberFormatException nfe) {
                    }
                }

                String sql = null;

                if (finalListValue.size() > 0) {
                    String value = "(" + String.join(",", finalListValue) + ")";
                    sql = String.format(SEARCH_EXPRESSION_TEMPLATE, getKpi().getUid(), value, this.dataType.getIdFieldName());
                } else {
                    sql = "1=0";
                }

                return Expr.raw(sql);
            }
            return null;
        }

        @Override
        public <T> void addEBeanSortExpression(OrderBy<T> orderby, SortStatusType sortStatusType, String fieldName) {
            if (sortStatusType != SortStatusType.NONE && sortStatusType != SortStatusType.UNSORTED) {
                String template = null;
                if (sortStatusType == SortStatusType.DESC) {
                    template = SORT_EXPRESSION_TEMPLATE.replace("#value#", SORT_EXPRESSION_TEMPLATE_DESC);
                } else {
                    template = SORT_EXPRESSION_TEMPLATE.replace("#value#", SORT_EXPRESSION_TEMPLATE_ASC);
                }
                orderby.asc(String.format(template, getKpi().getUid(), this.dataType.getIdFieldName()));
            }
        }

        /**
         * Get the KPI.
         */
        private Kpi getKpi() {
            return kpi;
        }

        @Override
        public boolean isKpi() {
            return true;
        }
    }

    /**
     * A filter component for KPI when no filter is available.
     * 
     * @author Johann Kohler
     */
    public static class KpiNoneFilterComponent extends NoneFilterComponent {

        @Override
        public boolean isKpi() {
            return true;
        }

    }

    /**
     * Exception for filter config.
     * 
     * @author Johann Kohler
     *
     */
    public static class FilterConfigException extends Exception {

        private static final long serialVersionUID = 1L;

        /**
         * Default constructor.
         * 
         * @param message
         *            the exception message
         */
        public FilterConfigException(String message) {
            super(message);
        }
    }

}
