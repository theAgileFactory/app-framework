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

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import models.framework_models.parent.IModelConstants;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.Where;

import framework.utils.DefaultSelectableValueHolderCollection;
import framework.utils.ISelectableValueHolder;
import framework.utils.ISelectableValueHolderCollection;

/**
 * All available countries.
 * 
 * @author Johann Kohler
 */
@Entity
public class Country extends Model implements ISelectableValueHolder<String> {

    private static final long serialVersionUID = -4564798724123L;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, Country> find = new Finder<Long, Country>(Country.class);

    @Id
    public Long id;

    public boolean isActive;

    @Column(length = 2)
    public String code;

    @Column(length = IModelConstants.LARGE_STRING)
    public String name;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "country")
    @Where(clause = "${ta}.deleted=0")
    public List<Address> addresses;

    public Country() {
    }

    @Override
    public String toString() {
        return code;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getValue() {
        return this.code;
    }

    @Override
    public boolean isSelectable() {
        return this.isActive;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public void setUrl(String url) {
    }

    @Override
    public int compareTo(Object o) {
        Country c = (Country) o;
        return this.getName().compareTo(c.getName());
    }

    public static Country getById(Long id) {
        return find.where().eq("id", id).findUnique();
    }

    public static Country getByCode(String code) {
        return find.where().eq("code", code).findUnique();
    }

    public static List<Country> getAllActive() {
        return find.where().eq("isActive", true).findList();
    }

    public static ISelectableValueHolderCollection<String> getAllActiveAsVHC() {
        return new DefaultSelectableValueHolderCollection<String>(getAllActive());
    }

}
