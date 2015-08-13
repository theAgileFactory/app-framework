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
package models.framework_models.api;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Version;

import com.avaje.ebean.Model;

import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;

/**
 * An entity which stores the API authorizations.<br/>
 * Here are the attributes:
 * <ul>
 * <li>name : the unique name of the application (a 64 letters code which cannot
 * be changed later)</li>
 * <li>description : a description of what is the application and what it does
 * with BizDock data</li>
 * <li>applicationKey : the base 64 encoded application key</li>
 * <li>sharedSecret : the base 64 encoded secret key</li>
 * <li>apiAuthorization : the API authorization structure</li>
 * </ul>
 * 
 * Here is how are structured the authorizations:
 * 
 * <pre>
 * GET (.*)
 * POST (.*)
 * DELETE (.*)
 * </pre>
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class ApiRegistration extends Model implements IModel {

    public static Finder<Long, ApiRegistration> find = new Finder<Long, ApiRegistration>(ApiRegistration.class);

    public ApiRegistration() {
    }

    @Id
    public Long id;

    public boolean deleted = false;

    @Version
    public Timestamp lastUpdate;

    @Column(length = IModelConstants.MEDIUM_STRING, nullable = false)
    public String name;

    @Column(length = IModelConstants.LARGE_STRING, nullable = false)
    public String description;

    @Column(length = IModelConstants.VLARGE_STRING, nullable = false)
    public String applicationKey;

    @Column(length = IModelConstants.VLARGE_STRING, nullable = false)
    public String sharedSecret;

    @Lob
    public byte[] apiAuthorization;

    public boolean testable;

    public boolean isDisplayed;

    /**
     * Return all the registrations from the database
     * 
     * @return a list of registration objects
     */
    public static List<ApiRegistration> getAllRegistrations() {
        return find.where().eq("deleted", false).findList();
    }

    /**
     * Return the API registration associated with the specified application
     * name
     * 
     * @param applicationName
     *            the name of an application
     * @return
     */
    public static ApiRegistration getFromApplicationName(String applicationName) {
        return find.where().eq("name", applicationName).eq("deleted", false).findUnique();
    }

    @Override
    public String audit() {
        return "ApiRegistration [id=" + id + ", deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", name=" + name + ", description=" + description
                + ", applicationKey=" + applicationKey + ", sharedSecret=" + sharedSecret + ", testable=" + testable + ", apiAuthorization="
                + Arrays.toString(apiAuthorization) + "]";
    }

    @Override
    public void defaults() {
        deleted = false;
    }

    @Override
    public void doDelete() {
        deleted = true;
        save();
    }
}
