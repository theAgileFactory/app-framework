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
package models.framework_models.kpi;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Version;

import models.framework_models.parent.IModelConstants;
import play.db.ebean.Model;

import com.avaje.ebean.annotation.EnumMapping;
import com.avaje.ebean.annotation.Where;

/**
 * A KPI definition value describes how to render a KPI value.
 * 
 * @author Johann Kohler
 */
@Entity
public class KpiValueDefinition extends Model {

    private static final long serialVersionUID = 638555493756L;

    public static Finder<Long, KpiValueDefinition> find = new Finder<Long, KpiValueDefinition>(Long.class, KpiValueDefinition.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    /**
     * Name of the KPI value (used in the header tables for the "cell" display
     * and as the box titles for the "box" display).
     */
    @Column(length = IModelConstants.MEDIUM_STRING, nullable = false)
    public String name;

    /**
     * Defines the render type of the KPI value.
     * 
     * VALUE: the KPI value is displayed without prefix and suffix<br/>
     * PATTERN: the KPI value is displayed thanks a pattern<br/>
     * LABEL: a label is displayed instead of the KPI value (usually used only
     * for the "main" value)
     */
    @Column(length = IModelConstants.SMALL_STRING, nullable = false)
    public RenderType renderType;

    /**
     * Only if renderType is "PATTERN".
     * 
     * Defines the pattern, it’s possible to include in the pattern the
     * following variables:<br/>
     * :i for the rounding KPI value (for example 12)<br/>
     * :si for the rounding KPI value with a sign (for example +12)<br/>
     * :d for the decimal KPI value (for example 12.32)<br/>
     * :sd for the decimal KPI value with a sign (for example +12.32)<br/>
     * :default_currency_code for the default currency code (for example CHF)
     * 
     * Example 1: :default_currency_code :d
     * 
     * Example 2: :i %
     */
    @Column(length = IModelConstants.MEDIUM_STRING)
    public String renderPattern;

    /**
     * Only and mandatory if the related kpiDefinition has isStandard to false
     * AND isExternal to false.
     * 
     * Defines the computation method for the KPI value. It is represented by a
     * JS code (that will be interpreted by JAVA). A variable "object" is
     * available and represents the instance of the object. The latest statement
     * of the code should be the value, it must be a number.
     * 
     * Example:
     * 
     * <pre>
     * var budgetOpex = object.getBudget(true);
     * var budgetCapex = object.getBudget(false);
     * budgetOpex + budgetCapex;
     * </pre>
     */

    public String computationJsCode;

    /**
     * The related kpi definition.
     */
    @OneToOne(mappedBy = "mainKpiValueDefinition", optional = true)
    public KpiDefinition kpiDefinitionForMainValue;

    @OneToOne(mappedBy = "additional1KpiValueDefinition", optional = true)
    public KpiDefinition kpiDefinitionForAdditional1Value;

    @OneToOne(mappedBy = "additional2KpiValueDefinition", optional = true)
    public KpiDefinition kpiDefinitionForAdditional2Value;

    /**
     * Only if the related kpiDefinition has isExternal to true OR has a
     * scheduler.
     */
    @OneToMany(mappedBy = "kpiValueDefinition")
    @Where(clause = "${ta}.deleted=0")
    public List<KpiData> kpiDatas;

    /**
     * Default constructor.
     */
    public KpiValueDefinition() {

    }

    /**
     * Get the KPI definition.
     */
    public KpiDefinition getKpiDefinition() {
        if (kpiDefinitionForMainValue != null) {
            return kpiDefinitionForMainValue;
        } else if (kpiDefinitionForAdditional1Value != null) {
            return kpiDefinitionForAdditional1Value;
        } else if (kpiDefinitionForAdditional2Value != null) {
            return kpiDefinitionForAdditional2Value;
        } else {
            return null;
        }
    }

    /**
     * Get a KPI value definition by id.
     * 
     * @param id
     *            the KPI value definition id
     */
    public static KpiValueDefinition getById(Long id) {
        return find.where().eq("deleted", false).eq("id", id).findUnique();
    }

    /**
     * Define the render types of a KpiValueDefinition.
     * 
     * @author Johann Kohler
     */
    @EnumMapping(nameValuePairs = "VALUE=VALUE, PATTERN=PATTERN, LABEL=LABEL", integerType = false)
    public static enum RenderType {
        VALUE, PATTERN, LABEL;
    }

}
