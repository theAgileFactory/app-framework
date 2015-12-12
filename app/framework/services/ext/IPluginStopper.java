package framework.services.ext;

/**
 * A callback interface to be used to "request" stopping all the plugins of a
 * specified type (identifier).
 * 
 * @author Pierre-Yves Cloux
 */
public interface IPluginStopper {
    /**
     * Request stopping all the instances if the plugin with the specified
     * identifier.
     * 
     * @param pluginDefinitionIdentifier
     * @return true if a plugin was stopped (false if no started plugins were
     *         found)
     */
    public boolean stopAllPluginsWithIdentifier(String pluginDefinitionIdentifier);
}
