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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import com.avaje.ebean.Model;

/**
 * A KPI data is a computed KPI value for a given date.
 * 
 * @author Johann Kohler
 */
@Entity
public class KpiData extends Model {

    public static Finder<Long, KpiData> find = new Finder<Long, KpiData>(KpiData.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    /**
     * The corresponding ID of the object type.
     */
    @Column(nullable = false)
    public Long objectId;

    /**
     * The date of the computation process is executed.
     */
    @Column(nullable = false)
    public Date timestamp;

    /**
     * The computed KPI value.
     */
    public BigDecimal value;

    /**
     * The computed KPI color rule.
     * 
     * This attribute is used only for a related KPI value definition that is
     * MAIN.
     */
    @ManyToOne(cascade = CascadeType.ALL)
    public KpiColorRule kpiColorRule;

    /**
     * The related KPI value definition.
     */
    @ManyToOne(cascade = CascadeType.ALL)
    public KpiValueDefinition kpiValueDefinition;

    /**
     * Default constructor.
     */
    public KpiData() {
    }

    /**
     * Get a KPI data by id.
     * 
     * @param id
     *            the KPI data id
     */
    public static KpiData getById(Long id) {
        return find.where().eq("deleted", false).eq("id", id).findUnique();
    }

    /**
     * Get the last KPI data of a kpiValueDefinition for a given object id.
     * 
     * @param kpiValueDefinitionId
     *            the KPI value definition id
     * @param objectId
     *            the object id
     */
    public static KpiData getLastOfKpiValueDefinitionForObjectId(Long kpiValueDefinitionId, Long objectId) {
        return find.orderBy("timestamp DESC").where().eq("deleted", false).eq("kpiValueDefinition.id", kpiValueDefinitionId).eq("objectId", objectId)
                .setMaxRows(1).findUnique();
    }

    /**
     * Get the KPI datas of the last 3 months for a kpiValueDefinition and an
     * object id.
     * 
     * @param kpiValueDefinitionId
     *            the KPI value definition id
     * @param objectId
     *            the object id
     */
    public static List<KpiData> getOfLast3MonthsForKpiValueDefinitionAndObjectId(Long kpiValueDefinitionId, Long objectId) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, -3);

        return find.orderBy("timestamp ASC").where().eq("deleted", false).eq("kpiValueDefinition.id", kpiValueDefinitionId).eq("objectId", objectId)
                .isNotNull("value").gt("timestamp", calendar.getTime()).findList();
    }

    /**
     * Get the KPI data of a value definition for a period.
     * 
     * @param kpiValueDefinitionId
     *            the KPI value definition id
     * @param objectId
     *            the object id
     * @param startDate
     *            the period start date
     * @param endDate
     *            the period end date
     */
    public static List<KpiData> getKpiDataAsListByPeriod(Long kpiValueDefinitionId, Long objectId, Date startDate, Date endDate) {
        return find.orderBy("timestamp ASC").where().eq("deleted", false).eq("kpiValueDefinition.id", kpiValueDefinitionId).eq("objectId", objectId)
                .isNotNull("value").ge("timestamp", startDate).le("timestamp", endDate).findList();
    }

}
