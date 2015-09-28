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

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import com.avaje.ebean.Model;

import models.framework_models.parent.IModelConstants;

/**
 * A class which represents a workspace to which some plugins can be registered
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class Workspace extends Model {
    /**
     * Default finder for the entity class
     */
    public static Finder<Long, Workspace> find = new Finder<Long, Workspace>(Workspace.class);

    @Id
    public Long id;

    public boolean isActive = true;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.LARGE_STRING)
    public String name;

    @Column(length = IModelConstants.VLARGE_STRING)
    public String description;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "workspace")
    public List<WorkspaceMember> members;

    public Workspace() {
    }
}
