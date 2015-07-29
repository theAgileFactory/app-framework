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
package framework.services.configuration;

import play.Logger;
import play.Play;
import framework.services.ServiceManager;

/**
 * A singleton which provides the instance of
 * {@link IImplementationDefinedObjectService} associated with the current
 * environment.<br/>
 * The environment is defined by a framework.conf parameter.<br/>
 * <b>WARNING</b>: we are not using the {@link ServiceManager} because this
 * object may be used "before" the service manager is initialized.
 * 
 * @author Pierre-Yves Cloux
 */
public class ImplementationDefineObjectServiceFactory {
    private static Logger.ALogger log = Logger.of(ImplementationDefineObjectServiceFactory.class);
    private static final String IDOS_CLASS = "maf.implementation.defined.object.service.class";

    private static IImplementationDefinedObjectService instance;

    private ImplementationDefineObjectServiceFactory() {
    }

    public static IImplementationDefinedObjectService getInstance() {
        if (instance == null) {
            String className = Play.application().configuration().getString(IDOS_CLASS);
            if (className == null) {
                String message = "No implementation time object service available";
                log.error(message);
                throw new RuntimeException(message);
            }
            try {
                Class<?> clazz = Play.application().classloader().loadClass(className);
                instance = (IImplementationDefinedObjectService) clazz.newInstance();
            } catch (Exception e) {
                String message = "Failed to instanciate implementation time object service with " + className;
                log.error(message);
                throw new RuntimeException(message, e);
            }
        }
        return instance;
    }
}
