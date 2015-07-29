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
package models.framework_models.plugin;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import models.framework_models.parent.IModelConstants;
import play.db.ebean.Model;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.SqlUpdate;

/**
 * Object which stores the associations between a plugin ID and a target system
 * id
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class PluginIdentificationLink extends Model {
    private static final long serialVersionUID = -7889963173982219739L;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, PluginIdentificationLink> find = new Finder<Long, PluginIdentificationLink>(Long.class, PluginIdentificationLink.class);

    @Id
    public Long id;

    public Long internalId;

    @Column(length = IModelConstants.LARGE_STRING)
    public String externalId;

    @Column(length = IModelConstants.SMALL_STRING)
    public String linkType;

    @Version
    public Timestamp lastUpdate;

    @ManyToOne(optional = false)
    public PluginConfiguration pluginConfiguration;

    @ManyToOne(optional = true)
    public PluginIdentificationLink parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    public List<PluginIdentificationLink> children;

    public PluginIdentificationLink() {
    }

    /**
     * Get a link by id.
     * 
     * @param id
     *            the link id
     */
    public static PluginIdentificationLink getById(Long id) {
        return find.where().eq("id", id).findUnique();
    }

    /**
     * Find the link associated with the specified pluginId or internalId
     * 
     * @param pluginConfigurationId
     *            a plugin id
     * @param internalId
     *            the internal id of a BizDock object
     * @param linkType
     *            a link type (optional: if null it is ignored)
     * @return a plugin identification link instance
     */
    public static PluginIdentificationLink getUniqueOneToOneLink(Long pluginConfigurationId, Long internalId, String externalId, String linkType) {
        ExpressionList<PluginIdentificationLink> expr =
                find.where().eq("pluginConfiguration.id", pluginConfigurationId).or(Expr.eq("externalId", externalId), Expr.eq("internalId", internalId));
        if (linkType != null) {
            expr = expr.eq("linkType", linkType);
        }
        return expr.findUnique();
    }

    /**
     * Find the link associated with the specified pluginId and internalId
     * 
     * @param pluginConfigurationId
     *            a plugin id
     * @param internalId
     *            the internal id of a BizDock object
     * @param linkType
     *            a link type (optional: if null it is ignored)
     * @return a plugin identification link instance
     */
    public static PluginIdentificationLink getUniqueLink(Long pluginConfigurationId, Long internalId, String externalId, String linkType) {
        ExpressionList<PluginIdentificationLink> expr =
                find.where().eq("pluginConfiguration.id", pluginConfigurationId).eq("externalId", externalId).eq("internalId", internalId);
        if (linkType != null) {
            expr = expr.eq("linkType", linkType);
        }
        return expr.findUnique();
    }

    /**
     * Find the children of link for a type.
     * 
     * @param pluginConfigurationId
     *            the plugin configuration id
     * @param internalId
     *            the internal ID of the parent
     * @param externalId
     *            the external ID of the parent
     * @param linkType
     *            the link type of the parent
     * @param childLinkType
     *            the link type of children
     */
    public static List<PluginIdentificationLink> getChildren(Long pluginConfigurationId, Long internalId, String externalId, String linkType,
            String childLinkType) {
        return find.where().eq("pluginConfiguration.id", pluginConfigurationId).eq("linkType", childLinkType).eq("parent.internalId", internalId)
                .eq("parent.externalId", externalId).eq("parent.linkType", linkType).findList();
    }

    /**
     * Find the link associated with the specified pluginId and internalId
     * 
     * @param pluginConfigurationId
     *            a plugin id
     * @param internalId
     *            the internal id of a BizDock object
     * @param linkType
     *            a link type (optional: if null it is ignored)
     * @return a plugin identification link instance
     */
    public static List<PluginIdentificationLink> getLinksForPluginAndInternalId(Long pluginConfigurationId, Long internalId, String linkType) {
        ExpressionList<PluginIdentificationLink> expr = find.where().eq("pluginConfiguration.id", pluginConfigurationId).eq("internalId", internalId);
        if (linkType != null) {
            expr = expr.eq("linkType", linkType);
        }
        return expr.findList();
    }

    /**
     * Find the link associated with the specified pluginId and externalId
     * 
     * @param pluginConfigurationId
     *            a plugin id
     * @param externalId
     *            the external id of a BizDock object
     * @param linkType
     *            a link type (optional: if null it is ignored)
     * @return a plugin identification link instance
     */
    public static List<PluginIdentificationLink> getLinksForPluginAndExternalId(Long pluginConfigurationId, String externalId, String linkType) {
        ExpressionList<PluginIdentificationLink> expr = find.where().eq("pluginConfiguration.id", pluginConfigurationId).eq("externalId", externalId);
        if (linkType != null) {
            expr = expr.eq("linkType", linkType);
        }
        return expr.findList();
    }

    /**
     * Find the link associated with the specified pluginId, externalId and
     * parentId
     * 
     * @param pluginConfigurationId
     *            a plugin id
     * @param externalId
     *            the external id of a BizDock object
     * @param parentId
     *            the id of the parent link
     * @param linkType
     *            a link type (optional: if null it is ignored)
     */
    public static List<PluginIdentificationLink> getLinksForPluginAndExternalIdAndParentId(Long pluginConfigurationId, String externalId, Long parentId,
            String linkType) {
        ExpressionList<PluginIdentificationLink> expr =
                find.where().eq("pluginConfiguration.id", pluginConfigurationId).eq("externalId", externalId).eq("parent.id", parentId);
        if (linkType != null) {
            expr = expr.eq("linkType", linkType);
        }
        return expr.findList();
    }

    /**
     * Flush all associations for the specified pluginConfiguration
     * 
     * @return the number of links deleted
     */
    public static int flushLinksForPlugin(Long pluginConfigurationId) {
        String sql = "delete plugin_identification_link where plugin_configuration_id=:plugin_configuration_id";
        SqlUpdate update = Ebean.createSqlUpdate(sql);
        update.setParameter("plugin_configuration_id", pluginConfigurationId);
        return Ebean.execute(update);
    }

    /**
     * Create a new identification link
     * 
     * @param pluginConfigurationId
     *            a plugin configuration id
     * @param internalId
     *            the id of the internal BizDock object
     * @param externalId
     *            the id of the external object to which the BizDock object is
     *            linked
     * @param linkType
     *            a type of link
     * @return 1 of the creation was successfull
     */
    public static int createLink(Long pluginConfigurationId, Long internalId, String externalId, String linkType) {
        String sql =
                "insert into plugin_identification_link (plugin_configuration_id, internal_id, external_id, link_type, last_update)"
                        + " values (:pluginConfigurationId,:internalId,:externalId,:linkType, NOW())";
        SqlUpdate update = Ebean.createSqlUpdate(sql);
        update.setParameter("pluginConfigurationId", pluginConfigurationId);
        update.setParameter("internalId", internalId);
        update.setParameter("externalId", externalId);
        update.setParameter("linkType", linkType);
        return update.execute();
    }
}
