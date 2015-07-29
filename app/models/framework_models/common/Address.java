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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;
import play.db.ebean.Model;
import framework.services.api.commons.IApiObject;

/**
 * Define an address.
 * 
 * @author Johann Kohler
 */
@Entity
public class Address extends Model implements IModel, IApiObject {
    private static final long serialVersionUID = -4546123321867L;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, Address> find = new Finder<Long, Address>(Long.class, Address.class);

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String lane1;

    @Column(length = IModelConstants.LARGE_STRING)
    public String lane2;

    @Column(length = IModelConstants.SMALL_STRING)
    public String zip;

    @Column(length = IModelConstants.MEDIUM_STRING)
    public String city;

    @Column(length = IModelConstants.MEDIUM_STRING)
    public String state;

    @ManyToOne(cascade = CascadeType.ALL)
    public Country country;

    public Address() {
    }

    @Override
    public String audit() {
        return "Address [id=" + id + ", lane1=" + lane1 + ", lane2=" + lane2 + ", zip=" + zip + ", city=" + city + ", state=" + state + ", country="
                + country + "]";
    }

    @Override
    public void defaults() {

    }

    @Override
    public void doDelete() {
        this.deleted = true;
        save();
    }

    @Override
    public String toString() {
        return "[lane1=" + lane1 + ", lane2=" + lane2 + ", zip=" + zip + ", city=" + city + ", state=" + state + ", country=" + country + "]";
    }

    public static Address getById(Long id) {
        return find.where().eq("deleted", false).eq("id", id).findUnique();
    }

    @Override
    public String getApiName() {
        return null;
    }

    @Override
    public boolean getApiDeleted() {
        return false;
    }
}
