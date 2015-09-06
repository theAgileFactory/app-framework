package framework.services.ext.api;

import framework.services.ext.ExtensionManagerException;
import framework.services.ext.ILinkGenerationService;
import play.Logger;
import play.mvc.Controller;

/**
 * The root class to be implemented by the extension controllers.<br/>
 * This one provides various utilities. This class implements the interface
 * {@link ILinkGenerator}.<br/>
 * This allow to pass the controller to a view to support link generation
 * without creating a "hard link" with the controller.<br/>
 * <b>WARNING</b><br/>
 * The constructor must be injected with a {@link ILinkGenerationService} so
 * that the link method can work.
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class AbstractExtensionController extends Controller implements ILinkGenerator {
    private ILinkGenerationService linkGenerationService;
    private static Log log = new Log();

    public AbstractExtensionController(ILinkGenerationService linkGenerationService) {
        this.linkGenerationService = linkGenerationService;
    }

    /**
     * Creates a link to a command identified by the specified commandId
     * 
     * @param commandId
     *            a unique id for a command
     * @param parameters
     *            one or more args matching the command parameters
     * @return a link
     */
    public String link(String commandId, Object... parameters) {
        try {
            return getLinkGenerationService().link(this, commandId, parameters);
        } catch (ExtensionManagerException e) {
            throw new IllegalArgumentException("Cannot generate link", e);
        }
    }

    /**
     * Return the logger for the extensions.<br/>
     * WARNING : no other logger must be used.<br/>
     * Using a logger may prevent the extension to be loaded.
     * 
     * @return
     */
    public Log getLog() {
        return log;
    }

    /**
     * A class which provides some logging features to the extension
     * 
     * @author Pierre-Yves Cloux
     */
    public static class Log {
        private Logger.ALogger log = Logger.of(AbstractExtensionController.class);

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

    private ILinkGenerationService getLinkGenerationService() {
        return linkGenerationService;
    }
}
