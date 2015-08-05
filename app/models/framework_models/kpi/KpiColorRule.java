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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.Where;

/**
 * A KPI color rule describes a rule for the background color of a KPI display.
 * 
 * If the rule is true for a specific value, then the background of the “box” or
 * the “cell” is defined by the cssClass.
 * 
 * A kpiDefinition could have a set of KPI color rules: they are interpreted
 * thanks the order attribute (the first that return true defines the cssClass
 * to apply).
 * 
 * @author Johann Kohler
 */
@Entity
public class KpiColorRule extends Model implements IModel {

    private static final long serialVersionUID = 45221693586L;

    public static Finder<Long, KpiColorRule> find = new Finder<Long, KpiColorRule>(KpiColorRule.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    /**
     * The rules are applied according to the order (for a kpiDefinition). As
     * soon as a rule matches, the process stops.
     */
    @Column(name = "`order`", nullable = false)
    public Integer order;

    /**
     * A JS code where the last statement is either true or false. The following
     * variables could be used: main, additional1, additional2.
     * 
     * <pre>
     * Example 1:
     * ---------------
     * main/additional1 > 0.2;
     * ---------------
     * 
     * Example 2:
     * ---------------
     * var i = additional1 + additional2;
     * if (i == 3) {
     *     true;
     * } else {
     *     false;
     * }
     * ---------------
     * </pre>
     * 
     * If the method returns false, then the process tries with the next
     * KpiColorRule (meaning rule with next order)
     * 
     * Usually the last KpiColorRule returns always true.
     */
    @Column(nullable = false)
    public String rule;

    /**
     * Defines the bootstrap CSS color class (for the background of the
     * cell/box).
     */
    @Column(length = IModelConstants.MEDIUM_STRING, nullable = false)
    public String cssColor;

    /**
     * If the renderType of a related kpiDefinitionValue (get throw the related
     * kpiDefinition) is "LABEL" then we display the renderLabel instead of the
     * value.
     * 
     * Usually a i18n key is used.
     */
    @Column(length = IModelConstants.MEDIUM_STRING)
    public String renderLabel;

    /**
     * The related KPI definition.
     */
    @ManyToOne(cascade = CascadeType.ALL)
    public KpiDefinition kpiDefinition;

    /**
     * The KPI datas associated to this rule.
     */
    @OneToMany(mappedBy = "kpiColorRule")
    @Where(clause = "${ta}.deleted=0")
    public List<KpiData> kpiDatas;

    /**
     * Default constructor.
     */
    public KpiColorRule() {
    }

    /**
     * Get a KPI color rule by id.
     * 
     * @param id
     *            the KPI color rule id
     */
    public static KpiColorRule getById(Long id) {
        return find.where().eq("deleted", false).eq("id", id).findUnique();
    }

    /**
     * Get the KPI color rule of a KPI definition with the previous order.
     * 
     * @param kpiDefinitionId
     *            the KPI definition id
     * @param order
     *            the current order
     */
    public static KpiColorRule getPrevious(Long kpiDefinitionId, int order) {
        return find.orderBy("order DESC").where().eq("deleted", false).eq("kpiDefinition.id", kpiDefinitionId).lt("order", order).setMaxRows(1).findUnique();
    }

    /**
     * Get the KPI color rule of a KPI definition with the next order.
     * 
     * @param kpiDefinitionId
     *            the KPI definition id
     * @param order
     *            the current order
     */
    public static KpiColorRule getNext(Long kpiDefinitionId, int order) {
        return find.orderBy("order ASC").where().eq("deleted", false).eq("kpiDefinition.id", kpiDefinitionId).gt("order", order).setMaxRows(1).findUnique();
    }

    /**
     * Get the last order rule of a KPI definition.
     * 
     * @param kpiDefinitionId
     *            the KPI definition id
     * @return
     */
    public static Integer getLastOrder(Long kpiDefinitionId) {
        KpiColorRule lastKpiColorRule = find.orderBy("order DESC").where().eq("deleted", false).eq("kpiDefinition.id", kpiDefinitionId).setMaxRows(1)
                .findUnique();
        if (lastKpiColorRule == null) {
            return -1;
        } else {
            return lastKpiColorRule.order;
        }

    }

    @Override
    public String audit() {
        return "KpiColorRule [id=" + id + ", deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", order=" + order + ", rule=" + rule + ", cssColor="
                + cssColor + ", renderLabel=" + renderLabel + "]";
    }

    @Override
    public void defaults() {
    }

    @Override
    public void doDelete() {
        this.deleted = true;
        save();
    }

}
