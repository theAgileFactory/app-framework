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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.avaje.ebean.Model;

/**
 * This entity represents a system level role.<br/>
 * Such role is independent of the involvement of the user in any portfolio
 * entry.<br/>
 * They are defined through Maf administration interface by the super user or
 * through the APIs.
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class SystemLevelRole extends Model {
    private static final long serialVersionUID = 8589276597653144474L;

    @Id
    public Long id;

    public Boolean isEnabled;

    @ManyToOne(optional = false)
    public SystemLevelRoleType systemLevelRoleType;

    @ManyToOne(optional = false)
    public Principal principal;

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, SystemLevelRole> find = new Finder<Long, SystemLevelRole>(SystemLevelRole.class);
}
