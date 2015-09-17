package framework.services.ext.api;

/**
 * An interface that gathers the link generation methods available in any
 * extension controller.
 * 
 * @author Pierre-Yves Cloux
 */
public interface ILinkGenerator {
    /**
     * Creates a link to a command identified by the specified commandId
     * 
     * @param commandId
     *            a unique id for a command
     * @param parameters
     *            one or more args matching the command parameters
     * @return a link
     */
    public String link(String commandId, Object... parameters);

    /**
     * Creates a link to the default command
     * 
     * @param parameters
     *            one or more args matching the command parameters
     * @return a link
     */
    public String linkDefault(Object... parameters);

}
