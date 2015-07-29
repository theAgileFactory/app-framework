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
package framework.commons;

import java.util.Date;

import play.data.format.Formatters;
import framework.utils.formats.AnnotationDateTypeFormatter;

/**
 * A class which containts the code call at startup in the Global class of the
 * application.
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class FrameworkStart {
    /**
     * Method called in the onStart method of the Play Global class.
     */
    public static void start() {
        // Register the data types
        DataType.add(IFrameworkConstants.User, "framework.services.account.IUserAccount", false, false);
        DataType.add(IFrameworkConstants.SystemLevelRoleType, "models.framework_models.account.SystemLevelRoleType", false, false);
        // Register the play framework date formatter
        Formatters.register(Date.class, new AnnotationDateTypeFormatter());
    }
}
