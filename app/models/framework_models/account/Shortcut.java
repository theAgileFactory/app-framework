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
package models.framework_models.account;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import models.framework_models.parent.IModelConstants;
import play.db.ebean.Model;
import framework.utils.DefaultSelectableValueHolderCollection;
import framework.utils.ISelectableValueHolder;
import framework.utils.ISelectableValueHolderCollection;

/**
 * Represent a shortcut to a specific route
 * 
 * @author Johann Kohler
 */
@Entity
public class Shortcut extends Model implements ISelectableValueHolder<Long> {

    private static final long serialVersionUID = 46546789789765L;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, Shortcut> find = new Finder<Long, Shortcut>(Long.class, Shortcut.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @ManyToOne(cascade = CascadeType.ALL, optional = true)
    public Principal principal;

    @Column(length = IModelConstants.MEDIUM_STRING)
    public String name;

    @Column(length = IModelConstants.LARGE_STRING)
    public String route;

    public Shortcut() {
        super();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getUrl() {
        return this.route;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Long getValue() {
        return this.id;
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setUrl(String url) {
        this.route = url;
    }

    @Override
    public int compareTo(Object o) {
        @SuppressWarnings("unchecked")
        ISelectableValueHolder<Long> v = (ISelectableValueHolder<Long>) o;
        return this.getName().compareTo(v.getName());
    }

    public void doDelete() {
        deleted = true;
        save();
    }

    public static Shortcut getById(Long id) {
        return find.where().eq("deleted", false).eq("id", id).findUnique();
    }

    public static Shortcut getByPrincipalAndRoute(Long principalId, String route) {
        return find.where().eq("deleted", false).eq("principal.id", principalId).eq("route", route).findUnique();
    }

    public static List<Shortcut> getByPrincipal(Long principalId) {
        return find.where().eq("deleted", false).eq("principal.id", principalId).findList();
    }

    public static ISelectableValueHolderCollection<Long> getByPrincipalAsValueHolderCollection(Long principalId) {
        return new DefaultSelectableValueHolderCollection<Long>(getByPrincipal(principalId));
    }

}
