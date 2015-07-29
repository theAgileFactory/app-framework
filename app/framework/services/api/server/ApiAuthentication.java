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
package framework.services.api.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import play.mvc.With;

/**
 * An annotation to be associated with controllers or controller methods which
 * are dealing with APIs.<br/>
 * <ul>
 * <li>permissions : is an array of permissions to be checked (instead of the
 * default permission set in the configuration "maf.api.default.permission")</li>
 * <li>allowTimeDifference : if true the different between the server and the
 * client time is not checked</li>
 * </ul>
 * 
 * The constant API_LOG_NAME contains the name of the log to be used to logging
 * API calls.
 * 
 * @author Pierre-Yves Cloux
 */
@With(ApiAuthenticationAction.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiAuthentication {

    /**
     * Set to true if the action could be called only with the "root" key.
     * 
     * Note: this implies no call possible directly from BizDock with a sign-in
     * user.
     */
    public boolean onlyRootKey() default false;

    /**
     * Set the permissions which are required for accessing the APIs
     */
    public String[] permissions() default {};

    /**
     * Set to true if the time difference (replay protection must be ignored)
     * 
     * @return a boolean
     */
    public boolean allowTimeDifference() default false;

    /**
     * Additional checks.
     */
    Class<? extends IApiAuthenticationAdditionalCheck> additionalCheck() default IApiAuthenticationAdditionalCheck.class;
}
