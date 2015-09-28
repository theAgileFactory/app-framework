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
package models.framework_models.workspace;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.avaje.ebean.Model;

import models.framework_models.account.Principal;

/**
 * A class which represents a member of a workspace.<br/>
 * "admin" is true if the member is administrator of the workspace.
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class WorkspaceMember extends Model {
    /**
     * Default finder for the entity class
     */
    public static Finder<Long, WorkspaceMember> find = new Finder<Long, WorkspaceMember>(WorkspaceMember.class);

    @Id
    public Long id;

    @ManyToOne(optional = false)
    public Principal principal;

    @ManyToOne(optional = false)
    public Workspace workspace;

    public boolean admin = true;

    public WorkspaceMember() {
    }
}
