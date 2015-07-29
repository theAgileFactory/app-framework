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
package framework.services.ext;

import play.Logger;
import framework.services.ServiceManager;

/**
 * An utility class to be used by extensions for logging and generating links.
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class ExtensionUtils {
    private static Log log = new Log();

    /**
     * Creates a link to a command identified by the specified commandId
     * 
     * @param controller
     *            a controller name (this one must contains a command with the
     *            specified Id)
     * @param commandId
     *            a unique id for a command
     * @param parameters
     *            one or more args matching the command parameters
     * @return a link
     * @throws ExtensionManagerException
     */
    public static String link(Class<?> controller, String commandId, Object... parameters) throws ExtensionManagerException {
        IExtensionManagerService extensionManagerService = ServiceManager.getService(IExtensionManagerService.NAME, IExtensionManagerService.class);
        return extensionManagerService.link(controller, commandId, parameters);
    }

    /**
     * Return the logger for the extensions.<br/>
     * WARNING : no other logger must be used.<br/>
     * Using a logger may prevent the extension to be loaded.
     * 
     * @return
     */
    public static Log getLog() {
        return log;
    }

    /**
     * A class which provides some logging features to the extension
     * 
     * @author Pierre-Yves Cloux
     */
    public static class Log {
        private Logger.ALogger log = Logger.of(ExtensionUtils.class);

        private Log() {
        }

        /**
         * Log an INFO message
         * 
         * @param extensionName
         *            the name of the extension
         * @param message
         *            a message to be logged
         */
        public void info(String extensionName, String message) {
            log.info(String.format("[EXT - %s] %s", extensionName, message));
        }

        /**
         * Log an ERROR message
         * 
         * @param extensionName
         *            the name of the extension
         * @param message
         *            a message to be logged
         */
        public void error(String extensionName, String message) {
            log.error(String.format("[EXT - %s] %s", extensionName, message));
        }

        /**
         * Log an ERROR message
         * 
         * @param extensionName
         *            the name of the extension
         * @param message
         *            a message to be logged
         * @param e
         *            an Exception
         */
        public void error(String extensionName, String message, Exception e) {
            log.error(String.format("[EXT - %s] %s", extensionName, message), e);
        }
    }
}
