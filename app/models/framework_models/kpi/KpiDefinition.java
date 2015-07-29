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
import javax.persistence.OrderBy;
import javax.persistence.Version;

import com.avaje.ebean.annotation.Where;

import models.framework_models.parent.IModelConstants;
import play.db.ebean.Model;

/**
 * A KPI definition represents all elements that describes a KPI.<br/>
 * -Where a located the data (inside BizDock or externally)<br/>
 * -The concerned BizDock object type<br/>
 * -How the computation of the values is done<br/>
 * ...
 * 
 * A KPI could have 1 (main) or 3 (main, additional1, additional2) values. Each
 * value is independent (in terms of computation) of each other.
 * 
 * A KPI could be rendered by 2 displays:<br/>
 * -cell: represents the "main" value of the KPI, displayed as a cell in the
 * object tables.<br/>
 * -box: represents the 3 values of the KPI, displayed as a nice box (usually in
 * the object dashboards).<br/>
 * The background of a cell or a box is computed thanks a specific criteria of
 * the KPI.<br/>
 * The box display exists only if the 3 values are defined.
 * 
 * There are 2 main dimensions to define a KPI:<br/>
 * -external VS internal<br/>
 * -standard VS custom<br/>
 * So there are 4 "kinds" of KPI.
 * 
 * The computation of the KPI values depends of its "kind":<br/>
 * -external: no computation is needed (because the data are provided by a
 * plugin and stored in the kpi_data table)<br/>
 * -internal and standard: the computation is done thanks a Java class that is
 * embedded in the BizDock application (so the standard KPIs could not be
 * modified between the releases).<br/>
 * -internal and custom: the computation is done thanks a JS code (stored in the
 * DB, so the custom KPIs could be created/modified at any time).
 * 
 * For the internal KPIs, there is one more dimension that could have 3 states:
 * <br/>
 * -without scheduler: the values are note stored in the kpi_data table and so
 * the computation is always done one the fly, no trend is also available.<br/>
 * -with scheduler and not real time: the values are stored in the kpi_data
 * table (so a trend is available), and are displayed thanks the last stored.
 * <br/>
 * -with scheduler and real time: the values are stored in the kpi_data table
 * (so a trend is available), and are displayed (in cell/box displays) on real
 * time (the computation is done on the fly).
 * 
 * So a trend is available for a KPI if the values are stored in the kpi_data
 * table, this is true when:<br/>
 * -the KPI is external<br/>
 * -the KPI is internal and has a scheduler
 * 
 * @author Johann Kohler
 */
@Entity
public class KpiDefinition extends Model {

    private static final long serialVersionUID = 1445645678989L;

    public static Finder<Long, KpiDefinition> find = new Finder<Long, KpiDefinition>(Long.class, KpiDefinition.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    /**
     * A unique ID to identify the KPI.
     */
    @Column(length = IModelConstants.MEDIUM_STRING, nullable = false)
    public String uid;

    /**
     * CSS glyphicons used in the "box" display.
     */
    @Column(length = IModelConstants.MEDIUM_STRING)
    public String cssGlyphicon;

    /**
     * Defines if the KPI is active (computed if a scheduler is configured and
     * could be displayed).
     */
    @Column(nullable = false)
    public Boolean isActive = true;

    /**
     * Defines if the KPI is by default displayed (in dashboards when global
     * rendering method is called for an objectType).
     */
    @Column(nullable = false)
    public Boolean isDisplayed = false;

    /**
     * Defines the order of the KPI (comparing to the orther KPIs with the same
     * objectType).
     */
    @Column(name = "`order`", nullable = false)
    public Integer order;

    /**
     * Defines the related BizDock object type (for example
     * models.pmo.PortfolioEntry). This information is used to find the list of
     * all KPIs of an object type.
     * 
     * Note: this field doesn't describe how the KPI is computed but when it is
     * rendered.
     */
    @Column(length = IModelConstants.LARGE_STRING, nullable = false)
    public String objectType;

    /**
     * If true: the KPI values are provided (and computed) by an external source
     * (usually a plugin). They are stored in the kpi_data table. So the KPI
     * doesn't not include computation methods.<br/>
     * If false: the KPI values are provided and computed thanks the BizDock
     * data. So the KPI should include the computation methods.
     */
    @Column(nullable = false)
    public Boolean isExternal = false;

    /**
     * A standard KPI is embedded inside BizDock, meaning we can explicitly use
     * it at any place of the application (as a standard field). A non-standard
     * (also call "custom") KPI could be considered as a "custom field".
     * 
     * If true: the SQL should be included in the DBMDL.<br/>
     * If false: the SQL are manually executed inside the specific BizDock
     * instance.
     */
    @Column(nullable = false)
    public Boolean isStandard = true;

    /**
     * Only and mandatory if isExternal is false and isStandard is true.
     * 
     * Defines the Java class that provides the computation methods. This class
     * should implement the IKpiComputation interface.
     */
    @Column(length = IModelConstants.LARGE_STRING)
    public String clazz;

    /**
     * Only if isExternal is false.
     * 
     * Defines the start time of the scheduler execution for the computation.
     * After the computation is called according to the schedulerFrequency.
     * 
     * If null: the KPI values are always computed on the fly.<br/>
     * If settled: the KPI values are stored in the kpi_data table and so could
     * be rendered statically (depending of the schedulerRealTime value) and a
     * trend is available.
     */
    public String schedulerStartTime;

    /**
     * Same constraints as schedulerStartDate.
     * 
     * Defines the scheduler frequency in minutes (after the start date).
     */
    public Integer schedulerFrequency;

    /**
     * Same constraints as schedulerStartDate.
     * 
     * If true: the values are displayed and computed on the "fly" in cell and
     * box displays.<br/>
     * If false: the values are displayed thanks the last computed data
     * available in the kpi_data table.
     * 
     * Note:<br/>
     * If isExternal is true: the values are displayed with the kpi_data table
     * and so not in a real time.<br/>
     * If isExternal is false and there is no scheduler: the values are
     * displayed in a real time.
     */
    public Boolean schedulerRealTime;

    /**
     * Only if isExternal is false and isStandard is true.
     * 
     * Defines a set of parameters that could be used in the Java computation
     * methods.
     * 
     * <pre>
     * Example:
     * test1=123
     * test2=A value
     * test3=25.5
     * </pre>
     */
    public String parameters;

    /**
     * Defines the main KPI value.
     */
    @OneToOne
    public KpiValueDefinition mainKpiValueDefinition;

    /**
     * Defines the additional KPI that is displayed on the bottom left of the
     * "box".
     */
    @OneToOne
    public KpiValueDefinition additional1KpiValueDefinition;

    /**
     * Defines the additional KPI that is displayed on the bottom right of the
     * "box".
     */
    @OneToOne
    public KpiValueDefinition additional2KpiValueDefinition;

    /**
     * Defines the rules to display the background color of a KPI (cell and
     * box).
     */
    @OneToMany(mappedBy = "kpiDefinition")
    @Where(clause = "${ta}.deleted=0")
    @OrderBy("order")
    public List<KpiColorRule> kpiColorRules;

    /**
     * Default constructor.
     */
    public KpiDefinition() {
    }

    /**
     * Get a KPI definition by id.
     * 
     * @param id
     *            the KPI definition id
     */
    public static KpiDefinition getById(Long id) {
        return find.where().eq("deleted", false).eq("id", id).findUnique();
    }

    /**
     * Get a KPI definition by uid.
     * 
     * @param uid
     *            the KPI definition uid
     */
    public static KpiDefinition getByUid(String uid) {
        return find.where().eq("deleted", false).eq("uid", uid).findUnique();
    }

    /**
     * Get the "displayed" KPI definition of an object type with the previous
     * order.
     * 
     * 
     * @param objectType
     *            the object type
     * @param order
     *            the current order
     */
    public static KpiDefinition getPrevious(String objectType, int order) {
        return find.orderBy("order DESC").where().eq("deleted", false).eq("objectType", objectType).eq("isDisplayed", true).lt("order", order).setMaxRows(1)
                .findUnique();
    }

    /**
     * Get the last order for an object type.
     * 
     * @param objectType
     *            the object type
     */
    public static Integer getLastOrder(String objectType) {
        KpiDefinition last = find.orderBy("order DESC").where().eq("deleted", false).eq("objectType", objectType).setMaxRows(1).findUnique();
        if (last == null) {
            return -1;
        } else {
            return last.order;
        }
    }

    /**
     * Get the "displayed" KPI definition of an object type with the next order.
     * 
     * @param objectType
     *            the object type
     * @param order
     *            the current order
     */
    public static KpiDefinition getNext(String objectType, int order) {
        return find.orderBy("order ASC").where().eq("deleted", false).eq("objectType", objectType).eq("isDisplayed", true).gt("order", order).setMaxRows(1)
                .findUnique();
    }

    /**
     * Get all KPI definitions.
     */
    public static List<KpiDefinition> getAll() {
        return find.orderBy("objectType, isDisplayed DESC, order").where().eq("deleted", false).findList();
    }

    /**
     * Get all active KPI definition.
     */
    public static List<KpiDefinition> getAllActive() {
        return find.orderBy("objectType, isDisplayed DESC, order").where().eq("deleted", false).eq("isActive", true).findList();
    }

    /**
     * Get all active and to display KPI definition of an object type.
     * 
     * @param objectType
     *            the object type
     */
    public static List<KpiDefinition> getActiveAndToDisplayOfObjectType(Class<?> objectType) {
        return find.orderBy("order").where().eq("deleted", false).eq("isActive", true).eq("isDisplayed", true).eq("objectType", objectType.getName())
                .findList();
    }

    /**
     * Get all active KPI definition of an object type.
     * 
     * @param objectType
     *            the object type
     */
    public static List<KpiDefinition> getActiveOfObjectType(Class<?> objectType) {
        return find.orderBy("isDisplayed DESC, order").where().eq("deleted", false).eq("isActive", true).eq("objectType", objectType.getName()).findList();
    }

}
