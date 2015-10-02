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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import framework.services.ServiceStaticAccessor;
import framework.services.kpi.IKpiService;
import framework.services.kpi.Kpi;
import framework.services.kpi.Kpi.DataType;
import framework.utils.Table.ColumnDef.SorterType;
import framework.utils.formats.CustomAttributeColumnFormatter;
import framework.utils.formats.KpiColumnFormatter;
import models.framework_models.common.CustomAttributeDefinition;
import models.framework_models.common.ICustomAttributeValue;
import play.Logger;
import scala.Option;

/**
 * This class is to be used with the {@link views.html.parts.tableview} and
 * {@link views.html.parts,tableview_row} scala template. It is used to display
 * table of objects with various options.
 * 
 * @param <T>
 *            the type of object which the table handle (one row = one object
 *            instance)
 * @author Pierre-Yves Cloux
 */
public class Table<T> {
    private static final String ID_COLUMN_NAME = "_id";

    public static final String KPI_COLUMN_NAME_PREFIX = "__KPI__";

    private List<ColumnDef> columnDefs;
    private Map<String, ColumnDef> columnDefsMap;
    private ColumnDef idFieldDef;
    private List<T> values;
    private String id;
    private Set<String> notDisplayedColumns;
    private Set<String> notDisplayedCustomAttributeColumns;
    private String emptyMessageKey = "table.empty";
    private IColumnFormatter<T> lineAction = null;

    /**
     * Attributes to manage the row actions (actions to applied to selected
     * rows). Can be used only in filter tables. There are not "static", meaning
     * they are settled only in a copy of the table (after the table has been
     * filled).
     */
    private List<RowAction> rowActions;
    private String allIdsUrl;

    private static Logger.ALogger log = Logger.of(Table.class);

    public Table() {
        this.id = String.valueOf(hashCode());
        this.columnDefs = new ArrayList<ColumnDef>();
        this.columnDefsMap = new HashMap<String, ColumnDef>();
        this.notDisplayedColumns = new HashSet<String>();
        this.notDisplayedCustomAttributeColumns = new HashSet<String>();
        this.rowActions = new ArrayList<>();
        this.allIdsUrl = null;
    }

    /**
     * Creates a new Table instance by cloning the configuration of the table
     * passed as a parameter
     * 
     * @param sourceTable
     * @param values
     * @param notDisplayedColumns
     *            the columns which must be hidden (not generated)
     */
    private Table(Table<T> sourceTable, List<T> values, Set<String> notDisplayedColumns) {
        this.columnDefs = sourceTable.getColumnDefs();
        this.columnDefsMap = sourceTable.getColumnDefsMap();
        this.idFieldDef = sourceTable.getIdColumnDef();
        this.id = sourceTable.getId();
        this.values = values;
        this.notDisplayedColumns = notDisplayedColumns;
        this.emptyMessageKey = sourceTable.getEmptyMessageKey();
        this.lineAction = sourceTable.getLineAction();
        this.rowActions = new ArrayList<>();
        this.allIdsUrl = null;
    }

    /**
     * Returns a table that inherits the configuration (field definitions) of
     * this table and that is filled with the values provided as parameters.
     * 
     * @param values
     *            a list of objects
     * @return a new Table filled with the values
     */
    public Table<T> fill(List<T> values) {
        return new Table<T>(this, values, this.notDisplayedCustomAttributeColumns);
    }

    /**
     * Returns a table that inherits the configuration (field definitions) of
     * this table and that is filled with the values provided as parameters.
     * 
     * @param values
     *            a list of objects
     * @param notDisplayedColumns
     *            the columns which should be displayed
     * @return a new Table filled with the values
     */
    public Table<T> fill(List<T> values, Set<String> notDisplayedColumns) {
        notDisplayedColumns.addAll(this.notDisplayedCustomAttributeColumns);
        return new Table<T>(this, values, notDisplayedColumns);
    }

    /**
     * Returns a table that inherits the configuration (field definitions) of
     * this table and that is filled with the values provided as parameters.
     * This method should be called when the table to display uses a filter
     * config.
     * 
     * @param values
     *            a list of objects
     * @param notDisplayedColumns
     *            the columns which should be displayed
     * 
     * @return a new Table filled with the values
     */
    public Table<T> fillForFilterConfig(List<T> values, Set<String> notDisplayedColumns) {
        return new Table<T>(this, values, notDisplayedColumns);
    }

    public void setLineAction(IColumnFormatter<T> formatter) {
        this.lineAction = formatter;
    }

    public IColumnFormatter<T> getLineAction() {
        return this.lineAction;
    }

    /**
     * Add a column to the table.<br/>
     * <b>WARNING</b>: The name of the column must not start with "_"
     * 
     * @param name
     *            the unique name of the column
     * @param valueFieldName
     *            the name of a field to be mapped with this column
     * @param label
     *            the label of the field (can be a i18n key)
     * @param sorterType
     *            the type of javascript client side sort mechanism to be
     *            associated with this column
     */
    public void addColumn(String name, String valueFieldName, String label, SorterType sorterType) {
        ColumnDef columnDef = new ColumnDef(name, valueFieldName, label, sorterType);
        this.columnDefs.add(columnDef);
        this.columnDefsMap.put(name, columnDef);
    }

    /**
     * Add a column to the table.<br/>
     * <b>WARNING</b>: The name of the column must not start with "_"
     * 
     * @param name
     *            the unique name of the column
     * @param valueFieldName
     *            the name of a field to be mapped with this column
     * @param label
     *            the label of the field (can be a i18n key)
     * @param sorterType
     *            the type of javascript client side sort mechanism to be
     *            associated with this column
     * @param escape
     *            if true the content is escaped to prevent HTML injection
     */
    public void addColumn(String name, String valueFieldName, String label, SorterType sorterType, boolean escape) {
        ColumnDef columnDef = new ColumnDef(name, valueFieldName, label, sorterType, escape);
        this.columnDefs.add(columnDef);
        this.columnDefsMap.put(name, columnDef);
    }

    /**
     * Set a formatter (usually a Scala closure) for the specified column
     * 
     * @param name
     *            the name of the column
     * @param formatter
     *            the formatter object (see {@link ColumnDef} for explanations)
     */
    public void setColumnFormatter(String name, Object formatter) {
        ColumnDef columnDef = getColumnDefsMap().get(name);
        if (columnDef != null) {
            columnDef.setFormatter(formatter);
        } else {
            throw new IllegalArgumentException("Unknown column " + name);
        }
    }

    /**
     * Add columns related to the custom attributes which are defined for the
     * specified object type.<br/>
     * <b>WARNING</b>: the id field name MUST be set before using this method.
     * 
     * @param objectType
     *            the list of the custom attribute definitions
     */
    public void addCustomAttributeColumns(Class<?> objectType) {
        List<CustomAttributeDefinition> customAttributeDefinitions = CustomAttributeDefinition.getOrderedCustomAttributeDefinitions(objectType);
        if (customAttributeDefinitions != null) {
            if (getIdFieldName() == null) {
                throw new IllegalArgumentException("WARNING: this method requires the ID field name to be defined");
            }
            for (CustomAttributeDefinition customAttributeDefinition : customAttributeDefinitions) {
                if (!customAttributeDefinition.isDisplayed) {
                    notDisplayedCustomAttributeColumns.add(customAttributeDefinition.uuid);
                }
                addColumn(customAttributeDefinition.uuid, getIdFieldName(), customAttributeDefinition.name, SorterType.NONE);
                setColumnFormatter(customAttributeDefinition.uuid, new CustomAttributeColumnFormatter<T>(objectType, customAttributeDefinition.id));
                if (customAttributeDefinition.attributeType.equals(ICustomAttributeValue.AttributeType.URL.name())) {
                    setColumnValueCssClass(customAttributeDefinition.uuid, "rowlink-skip");
                }
            }
        }
    }

    /**
     * Add all KPIs of an object type to the table.
     * 
     * Note: the KPIs are by default not displayed. This method should be called
     * only for a table with filtering capability.
     * 
     * @param objectType
     *            the object type
     */
    public void addKpis(Class<?> objectType) {
        IKpiService kpiService = ServiceStaticAccessor.getKpiService();
        List<Kpi> kpis = kpiService.getActiveKpisOfObjectType(objectType);
        if (kpis != null) {
            for (Kpi kpi : kpis) {
                notDisplayedCustomAttributeColumns.add(KPI_COLUMN_NAME_PREFIX + kpi.getUid());
                addKpi(kpi.getUid());
            }
        }
    }

    /**
     * Add a specific KPI to the table.
     * 
     * @param kpiUid
     *            the KPI definition uid
     */
    public void addKpi(String kpiUid) {

        if (getIdFieldName() == null) {
            throw new IllegalArgumentException("WARNING: this method requires the ID field name to be defined");
        }

        Kpi kpi = ServiceStaticAccessor.getKpiService().getKpi(kpiUid);
        if (kpi != null) {
            addColumn(KPI_COLUMN_NAME_PREFIX + kpi.getUid(), getIdFieldName(), kpi.getValueName(DataType.MAIN), Table.ColumnDef.SorterType.NONE);
            setJavaColumnFormatter(KPI_COLUMN_NAME_PREFIX + kpi.getUid(), new KpiColumnFormatter<T>(kpi.getUid()));
            if (kpi.hasLink()) {
                setColumnValueCssClass(KPI_COLUMN_NAME_PREFIX + kpi.getUid(), "rowlink-skip");
            }
        }
    }

    public void addAjaxRowAction(String label, String url, String domContainerId) {
        this.rowActions.add(new RowAction(label, url, domContainerId, false, null));
    }

    public void addAjaxRowAction(String label, String url, String domContainerId, String confirmationMessage) {
        this.rowActions.add(new RowAction(label, url, domContainerId, true, confirmationMessage));
    }

    public void addLinkRowAction(String label, String url) {
        this.rowActions.add(new RowAction(label, url, false, null));
    }

    public void addLinkRowAction(String label, String url, String confirmationMessage) {
        this.rowActions.add(new RowAction(label, url, true, confirmationMessage));
    }

    public boolean hasRowActions() {
        return this.rowActions.size() > 0 && this.allIdsUrl != null ? true : false;
    }

    public List<RowAction> getRowActions() {
        return this.rowActions;
    }

    public void setAllIdsUrl(String allIdsUrl) {
        this.allIdsUrl = allIdsUrl;
    }

    public String getAllIdsUrl() {
        return this.allIdsUrl;
    }

    /**
     * Set a java formatter for the specified column.
     * 
     * @param name
     *            the name of the column
     * @param formatter
     *            an instance of {@link IColumnFormatter} if the formatting is
     *            handled in java
     */
    public void setJavaColumnFormatter(String name, IColumnFormatter<T> formatter) {
        setColumnFormatter(name, formatter);
    }

    /**
     * Set a CSS class for a specified column (applied to the headers and to the
     * values rows)
     * 
     * @param name
     *            the name of the column
     * @param cssClass
     *            the CSS class to applied
     */
    public void setColumnCssClass(String name, String cssClass) {
        ColumnDef columnDef = getColumnDefsMap().get(name);
        if (columnDef != null) {
            columnDef.setCssClass(cssClass);
        } else {
            throw new IllegalArgumentException("Unknown column " + name);
        }
    }

    /**
     * Set a value CSS class for a specified column
     * 
     * "value CSS class" means that the CSS class is applied to the values rows
     * (td) and not to the headers row (th)
     * 
     * @param name
     *            the name of the column
     * @param valueCssClass
     *            the CSS class to applied
     */
    public void setColumnValueCssClass(String name, String valueCssClass) {
        ColumnDef columnDef = getColumnDefsMap().get(name);
        if (columnDef != null) {
            columnDef.setValueCssClass(valueCssClass);
        } else {
            throw new IllegalArgumentException("Unknown column " + name);
        }
    }

    public String getIdFieldName() {
        return this.idFieldDef.getFieldName();
    }

    public String getId() {
        return id;
    }

    /**
     * Set the name of the field to be used as the Id the rows
     * 
     * @param idFieldName
     *            the name of a field
     */
    public void setIdFieldName(String idFieldName) {
        this.idFieldDef = new ColumnDef(ID_COLUMN_NAME, idFieldName, idFieldName, SorterType.NONE);
    }

    public List<T> getValues() {
        return values;
    }

    /**
     * Return true if the table is not empty
     * 
     * @return a boolean
     */
    public boolean hasValues() {
        return values != null && values.size() != 0;
    }

    /**
     * Return a list of table headers
     * 
     * @return a list of String
     */
    public List<ColumnDef> getHeaders() {
        if (getNotDisplayedColumns().size() != 0) {
            List<ColumnDef> displayableColumnDefs = new ArrayList<ColumnDef>();
            for (ColumnDef columnDef : getColumnDefs()) {
                if (!getNotDisplayedColumns().contains(columnDef.getName())) {
                    displayableColumnDefs.add(columnDef);
                }
            }
            return displayableColumnDefs;
        }
        return getColumnDefs();
    }

    /**
     * Return a list of rows using the list of values passed as a parameter.
     * <br/>
     * A formatting is applied according to FieldDef
     * 
     * @param values
     *            a list of objects to be formatted as a table
     * @return a list of Rows (basically a row is a list of String)
     */
    public List<FormattedRow> getFormattedRows() {
        List<FormattedRow> rows = new ArrayList<FormattedRow>();
        for (T value : getValues()) {
            rows.add(getFormattedRow(value));
        }
        return rows;
    }

    /**
     * Return a list of rows using the list of values passed as a parameter.
     * <br/>
     * NO formatting is applied.
     * 
     * @param values
     *            a list of objects to be formatted as a table
     * @return a list of Rows (basically a row is a list of String)
     */
    public List<NotFormattedRow> getNotFormattedRows() {
        List<NotFormattedRow> rows = new ArrayList<NotFormattedRow>();
        for (T value : getValues()) {
            rows.add(getNotFormattedRow(value));
        }
        return rows;
    }

    /**
     * Return one single table row from the specified value object passed as a
     * parameter A formatting is applied according to FieldDef
     * 
     * @param value
     *            an object to be formatted as a table row
     * @return a Row object
     */
    public FormattedRow getFormattedRow(T value) {
        List<String> rowValues = new ArrayList<String>(getColumnDefs().size() - getNotDisplayedColumns().size());
        List<String> cssClasses = new ArrayList<String>(getColumnDefs().size() - getNotDisplayedColumns().size());
        for (ColumnDef columnDef : getColumnDefs()) {
            if (!getNotDisplayedColumns().contains(columnDef.getName())) {
                rowValues.add(getFormattedCellValue(columnDef, value));
                cssClasses.add(columnDef.getValueCssClass());
            }
        }

        String actionLink = null;
        if (getLineAction() != null) {
            Object returnValue = getLineAction().apply(value, null);
            if (returnValue != null) {
                actionLink = "<a href=\"" + String.valueOf(returnValue) + "\" class=\"hidden\"></a>";
            }
        }

        if (getIdFieldName() == null) {
            return new FormattedRow(rowValues, cssClasses, actionLink);
        } else {
            return new FormattedRow(getFormattedCellValue(getIdColumnDef(), value), rowValues, cssClasses, actionLink);
        }
    }

    /**
     * Return one single table row from the specified value object passed as a
     * parameter <b>NO</b> formatting is applied.
     * 
     * @param value
     *            an object to be formatted as a table row
     * @return a Row object
     */
    public NotFormattedRow getNotFormattedRow(T value) {
        List<Object> rowValues = new ArrayList<Object>(getColumnDefs().size() - getNotDisplayedColumns().size());
        for (ColumnDef columnDef : getColumnDefs()) {
            if (!getNotDisplayedColumns().contains(columnDef.getName())) {
                if (columnDef.formatter != null && columnDef.formatter.getClass().equals(CustomAttributeColumnFormatter.class)) {
                    @SuppressWarnings("unchecked")
                    CustomAttributeColumnFormatter<T> formatter = (CustomAttributeColumnFormatter<T>) columnDef.formatter;
                    Long id = (Long) getCellValue(columnDef, value);
                    rowValues.add(formatter.getCustomAttributeValue(id).getValueAsObject());

                } else {
                    rowValues.add(getCellValue(columnDef, value));
                }
            }
        }
        if (getIdFieldName() == null) {
            return new NotFormattedRow(rowValues);
        } else {
            return new NotFormattedRow(getFormattedCellValue(getIdColumnDef(), value), rowValues);
        }
    }

    /**
     * Get the attribute value matching the specified ColumnDef from the object
     * passed as a parameter. This cellValue is formatted automatically if a
     * formatter is associated with the column.
     * 
     * @param columnDef
     *            a column definition
     * @param value
     *            an object
     * @return a String value (blank if something unexpected happens = exception
     *         while introspecting the object)
     */
    private String getFormattedCellValue(ColumnDef columnDef, Object value) {
        return columnDef.format(value, getCellValue(columnDef, value));
    }

    /**
     * Get the attribute value matching the specified ColumnDef from the object
     * passed as a parameter. The cellValue is provided as-is (no formatting is
     * applied)
     * 
     * @param columnDef
     *            a column definition
     * @param value
     *            an object
     * @return a String value (blank if something unexpected happens = exception
     *         while introspecting the object)
     */
    private Object getCellValue(ColumnDef columnDef, Object value) {
        try {
            return PropertyUtils.getProperty(value, columnDef.getFieldName());
        } catch (IllegalAccessException e) {
            log.error("Unable to get property " + columnDef.getFieldName() + " from bean " + value);
        } catch (InvocationTargetException e) {
            log.error("Unable to get property " + columnDef.getFieldName() + " from bean " + value);
        } catch (NoSuchMethodException e) {
            log.error("Unable to get property " + columnDef.getFieldName() + " from bean " + value);
        }
        return "";
    }

    private ColumnDef getIdColumnDef() {
        return idFieldDef;
    }

    /**
     * Returns the list of column definitions
     * 
     * @return a list of field definition
     */
    private List<ColumnDef> getColumnDefs() {
        return columnDefs;
    }

    private Map<String, ColumnDef> getColumnDefsMap() {
        return columnDefsMap;
    }

    private Set<String> getNotDisplayedColumns() {
        return notDisplayedColumns;
    }

    public String getEmptyMessageKey() {
        return this.emptyMessageKey;
    }

    public void setEmptyMessageKey(String emptyMessageKey) {
        this.emptyMessageKey = emptyMessageKey;
    }

    /**
     * This class implements a table column descriptor.
     * <ul>
     * <li>name : the name of the column (must be unique)</li>
     * <li>label : the label of the column which will be used in the table
     * header</li>
     * <li>fieldName : the name of the field (must match the name of an
     * attribute of a table value)</li>
     * <li>sorterType : the type of sort to be available for this column, see
     * {@link SorterType}</li>
     * <li>escape : if true (default) the content is escaped to prevent HTML
     * injection (WARNING: columns with formatter are never "escaped")</li>
     * <li>cssClass : CSS class for the header and cells of the column</li>
     * <li>valueCssClass : CSS class only for the cells of the column</li>
     * <li>sorterUrl : the URL to be used for the header if the
     * {@link SorterType} URL_SORTER is selected</li>
     * </ul>
     * 
     * <h2>Formatters</h2 A column descriptor can be associated with a
     * formatter. A formatter is any class that has an "apply" method taking as
     * parameters: - an object of the same type as the one associated with the
     * {@link Table} (the row object) - the value that is to be set in the cell
     * 
     * For java, the recommendation is to use {@link IColumnFormatter}.
     * 
     * Alternatively (when used in a Scala template), it can be a scala closure.
     * Here is an example of usage in a Scala template :
     * 
     * <pre>
     * &#64;table.setColumnFormatter("mail", {(anObject: models.sample.Person, mail: Option[String])   =>  {
     *  if(mail.isEmpty){
     *          ""
     *          }else{
     *              "<a href=\"mailto:"+mail.get+"\">"+mail.get+"</a>"
     *          }
     *      }
     * })
     * </pre>
     * 
     * <b>WARNING:</b> please make sure that the type of the values to be
     * converted match with the one specifiedLevel in the closure. Be also
     * careful to use Option[Type] as a parameter for the cellValue if you are
     * using a scala closure. This is a standard scala practice to use Option[].
     * 
     * @author Pierre-Yves Cloux
     */
    public static class ColumnDef implements Comparable<ColumnDef> {
        private String name;
        private String label;
        private String fieldName;
        private Object formatter;
        private SorterType sorterType;
        private boolean escape = true;
        private boolean isJavaFormatter = false;
        private String cssClass;
        private String valueCssClass;

        /**
         * Enum that encapsulate the 3 types of sorter that are allowed for a
         * header.
         * <ul>
         * <li>STRING_SORTER : a string value</li>
         * <li>DATE_SORTER : a date which must be formatted as dd/mm/yyyy</li>
         * <li>NUMBER_SORTER : a number</li>
         * <li>DATE_TIME_SORTER : a date and time which must be formatted as
         * dd/mm/yyyy hh:mm</li>
         * </ul>
         */
        public enum SorterType {
            NONE(null), STRING_SORTER("maf_stringSort"), DATE_SORTER("maf_dateSort"), NUMBER_SORTER("maf_numberSort"), DATE_TIME_SORTER(
                    "maf_dateAndTimeSort");
            private String jsFunction;

            private SorterType(String jsFunction) {
                this.jsFunction = jsFunction;
            }

            public String getJsFunction() {
                return jsFunction;
            }
        }

        /**
         * Creates a new {@link ColumnDef}
         * 
         * @param name
         *            the unique name of the column
         * @param fieldName
         *            the name of the field
         * @param label
         *            the label of the field
         * @param sorterType
         *            the type of client side sort to be associated with this
         *            column
         */
        public ColumnDef(String name, String fieldName, String label, SorterType sorterType) {
            this(name, fieldName, label, sorterType, true);
        }

        /**
         * Creates a new {@link ColumnDef}
         * 
         * @param name
         *            the unique name of the column
         * @param fieldName
         *            the name of the field
         * @param label
         *            the label of the field
         * @param sorterType
         *            the type of client side sort to be associated with this
         *            column
         * @param escape
         *            if true the content is escaped to prevent HTML injection
         */
        public ColumnDef(String name, String fieldName, String label, SorterType sorterType, boolean escape) {
            super();
            this.name = name;
            this.label = label;
            this.fieldName = fieldName;
            this.sorterType = sorterType;
            this.escape = escape;

        }

        /**
         * Format as a String the specified value.<br/>
         * This will make use of the formatter if any has been defined.
         * 
         * @param value
         *            the full object (this one is passed since its the object
         *            passed to the formatters)
         * @param cellValue
         *            the value of an attribute of the object (dynamically
         *            extracted)
         * @return a String
         */
        private String format(Object value, Object cellValue) {
            if (this.formatter != null) {
                try {
                    if (isJavaFormatter()) {
                        // IColumnFormatter object implemented in java
                        @SuppressWarnings("rawtypes")
                        IColumnFormatter columnFormatter = (IColumnFormatter) this.formatter;
                        @SuppressWarnings("unchecked")
                        Object returnValue = columnFormatter.apply(value, cellValue);
                        if (returnValue != null)
                            return String.valueOf(returnValue);
                    } else {
                        // Scala closure
                        Object[] params = { value, Option.apply(cellValue) };
                        Object returnValue = MethodUtils.invokeMethod(this.formatter, "apply", params);
                        if (returnValue != null)
                            return String.valueOf(returnValue);
                    }
                } catch (Exception e) {
                    log.error("Error during the formatting of the column " + name, e);
                    throw new RuntimeException("Error while calling the dynamic column formatter " + name + ", please correct", e);
                }
                return "";
            } else {
                return cellValue == null ? "" : escapeIfRequired(String.valueOf(cellValue));
            }
        }

        /**
         * Escape the value (HTML code) if isEscape is set to true
         * 
         * @param value
         * @return
         */
        private String escapeIfRequired(String value) {
            if (isEscape()) {
                return StringEscapeUtils.escapeHtml4(value);
            }
            return value;
        }

        @Override
        public int compareTo(ColumnDef o) {
            return o.getName().compareTo(this.getName());
        }

        public String getLabel() {
            return label;
        }

        public String getSorterJsFunction() {
            return sorterType.getJsFunction();
        }

        public String getFieldName() {
            return fieldName;
        }

        private void setFormatter(Object formatter) {
            this.formatter = formatter;
            if (IColumnFormatter.class.isAssignableFrom(formatter.getClass())) {
                this.isJavaFormatter = true;
            }
        }

        public String getName() {
            return name;
        }

        public boolean isEscape() {
            return escape;
        }

        private boolean isJavaFormatter() {
            return isJavaFormatter;
        }

        public String getValueCssClass() {
            return valueCssClass;
        }

        private void setValueCssClass(String valueCssClass) {
            this.valueCssClass = valueCssClass;
        }

        public String getCssClass() {
            return cssClass;
        }

        private void setCssClass(String cssClass) {
            this.cssClass = cssClass;
        }

        public SorterType getSorterType() {
            return sorterType;
        }
    }

    /**
     * A class that represents a row of the table (where a formatting has been
     * applied).<br/>
     * A Row contains:
     * <ul>
     * <li>A list of formatted cell values (basically a List of String)</li>
     * <li>An Id to be used to identify uniquely the row (this one is optional)
     * </li>
     * </ul>
     * 
     * @author Pierre-Yves Cloux
     */
    public static class FormattedRow {
        private String id;
        private List<String> values;
        private List<String> cssClasses;
        private String actionLink;

        public FormattedRow(List<String> values) {
            super();
            this.values = values;
        }

        public FormattedRow(List<String> values, List<String> cssClasses, String actionLink) {
            super();
            this.values = values;
            this.cssClasses = cssClasses;
            this.actionLink = actionLink;
        }

        public FormattedRow(String id, List<String> values, String actionLink) {
            super();
            this.id = id;
            this.values = values;
            this.actionLink = actionLink;
        }

        public FormattedRow(String id, List<String> values, List<String> cssClasses, String actionLink) {
            super();
            this.id = id;
            this.values = values;
            this.cssClasses = cssClasses;
            this.actionLink = actionLink;
        }

        public String getId() {
            return id;
        }

        public List<String> getValues() {
            return values;
        }

        public List<String> getCssClasses() {
            return cssClasses;
        }

        public void setCssClasses(List<String> cssClasses) {
            this.cssClasses = cssClasses;
        }

        public String getActionLink() {
            return actionLink;
        }
    }

    /**
     * A class that represents a row of the table (where NO formatting has been
     * applied).<br/>
     * A Row contains:
     * <ul>
     * <li>A list of NOT formatted cell values (basically a List of Objects)
     * </li>
     * <li>An Id to be used to identify uniquely the row (this one is optional)
     * </li>
     * </ul>
     * 
     * @author Pierre-Yves Cloux
     */
    public static class NotFormattedRow {
        private String id;
        private List<Object> values;

        public NotFormattedRow(List<Object> values) {
            super();
            this.values = values;
        }

        public NotFormattedRow(String id, List<Object> values) {
            super();
            this.id = id;
            this.values = values;
        }

        public String getId() {
            return id;
        }

        public List<Object> getValues() {
            return values;
        }
    }

    public static class RowAction {

        private String label;
        private String url;
        private boolean isAjax;
        private String domContainerId;
        private boolean withConfirmation;
        private String confirmationMessage;

        private RowAction(String label, String url) {
            this.label = label;
            this.url = url;
        }

        private RowAction(String label, String url, String domContainerId, boolean withConfirmation, String confirmationMessage) {
            this(label, url);
            this.isAjax = true;
            this.domContainerId = domContainerId;
            this.withConfirmation = withConfirmation;
            this.confirmationMessage = confirmationMessage;
        }

        private RowAction(String label, String url, boolean withConfirmation, String confirmationMessage) {
            this(label, url);
            this.isAjax = false;
            this.withConfirmation = withConfirmation;
            this.confirmationMessage = confirmationMessage;
        }

        public String toJson() {
            try {
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                return ow.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return null;
            }
        }

        /**
         * @return the label
         */
        public String getLabel() {
            return label;
        }

        /**
         * @param label
         *            the label to set
         */
        public void setLabel(String label) {
            this.label = label;
        }

        /**
         * @return the url
         */
        public String getUrl() {
            return url;
        }

        /**
         * @param url
         *            the url to set
         */
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * @return the isAjax
         */
        public boolean isAjax() {
            return isAjax;
        }

        /**
         * @param isAjax
         *            the isAjax to set
         */
        public void setAjax(boolean isAjax) {
            this.isAjax = isAjax;
        }

        /**
         * @return the domContainerId
         */
        public String getDomContainerId() {
            return domContainerId;
        }

        /**
         * @param domContainerId
         *            the domContainerId to set
         */
        public void setDomContainerId(String domContainerId) {
            this.domContainerId = domContainerId;
        }

        /**
         * @return the withConfirmation
         */
        public boolean isWithConfirmation() {
            return withConfirmation;
        }

        /**
         * @param withConfirmation
         *            the withConfirmation to set
         */
        public void setWithConfirmation(boolean withConfirmation) {
            this.withConfirmation = withConfirmation;
        }

        /**
         * @return the confirmationMessage
         */
        public String getConfirmationMessage() {
            return confirmationMessage;
        }

        /**
         * @param confirmationMessage
         *            the confirmationMessage to set
         */
        public void setConfirmationMessage(String confirmationMessage) {
            this.confirmationMessage = confirmationMessage;
        }

    }

}
