package framework.services.ext;

/**
 * The interface which is to be called to generate a link to an extension
 * controller action.
 * 
 * @author Pierre-Yves Cloux
 */
public interface ILinkGenerationService {
    /**
     * Creates a link to a command identified by the specified commandId
     * 
     * @param controllerInstance
     *            an extension controller instance (this one must contains a
     *            command with the specified Id)
     * @param commandId
     *            a unique id for a command
     * @param args
     *            one or more args matching the command method parameters
     * @return a link
     */
    public String link(Object controllerInstance, String commandId, Object... parameters) throws ExtensionManagerException;

}
