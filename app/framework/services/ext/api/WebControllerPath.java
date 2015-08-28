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
package framework.services.ext.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation which defines an alternative routing mechanism to be used with
 * {@link ExtensionUtils}
 * 
 * @author Pierre-Yves Cloux
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface WebControllerPath {
    /**
     * The web path to this resource.<br/>
     * If the annotation is for a type, then it is expected that the full path
     * is defined by a complementary method level annotation
     * 
     * @return a path
     */
    public String path();

    /**
     * An array of permissions which are required to access this path.<br/>
     * WARNING: default is blank thus the resource can be accessed by any
     * authenticated user
     * 
     * @return a permission
     */
    public String[]permissions() default {};
}
