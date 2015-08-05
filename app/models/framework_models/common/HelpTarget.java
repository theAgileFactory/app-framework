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
package models.framework_models.common;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import models.framework_models.parent.IModelConstants;

import com.avaje.ebean.Model;

/**
 * An help target is a mapping between a BizDock route and an external target.
 * 
 * @author Johann Kohler
 */
@Entity
public class HelpTarget extends Model {

    private static final long serialVersionUID = -8381580722836654720L;

    public static Finder<Long, HelpTarget> find = new Finder<Long, HelpTarget>(HelpTarget.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String route;

    @Column(length = IModelConstants.LARGE_STRING)
    public String target;

    /**
     * Get an help target by id.
     * 
     * @param id
     *            the help target id
     */
    public static HelpTarget getById(Long id) {
        return find.where().eq("deleted", false).eq("id", id).findUnique();
    }

    /**
     * Get an help target by route.
     * 
     * @param route
     *            the route
     */
    public static HelpTarget getByRoute(String route) {
        return find.where().eq("deleted", false).eq("route", route).findUnique();
    }
}
