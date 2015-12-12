package framework.services.script;

import javax.inject.Inject;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang3.ArrayUtils;

import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;

/**
 * A service which is managing the script engines
 * 
 * @author Pierre-Yves Cloux
 */
public class ScriptServiceImpl implements IScriptService {
    private static Logger.ALogger log = Logger.of(ScriptServiceImpl.class);
    /**
     * Name of the attribute set in the {@link ScriptEngine} context which is
     * associated with the name of the script engine
     */
    private static final String SCRIPT_ENGINE_NAME = "___scriptEngineName";
    private NashornScriptEngineFactory factory;

    /**
     * Create a script service
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     */
    @Inject
    public ScriptServiceImpl(ApplicationLifecycle lifecycle, Configuration configuration) {
        log.info("SERVICE>>> ScriptServiceImpl starting...");
        factory = new NashornScriptEngineFactory();
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> ScriptServiceImpl stopping...");
            factory = null;
            log.info("SERVICE>>> ScriptServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> ScriptServiceImpl started");
    }

    @Override
    public synchronized ScriptEngine getEngine(String name) {
        if (log.isDebugEnabled()) {
            log.debug("Creating a script engine " + name);
        }
        ScriptEngine scriptEngine = getFactory().getScriptEngine(new ClassFilter() {
            @Override
            public boolean exposeToScripts(String className) {
                return true;
            }
        });
        scriptEngine.getContext().removeAttribute("JavaImporter", ScriptContext.ENGINE_SCOPE);
        scriptEngine.getContext().removeAttribute("Java", ScriptContext.ENGINE_SCOPE);
        scriptEngine.getContext().setAttribute(SCRIPT_ENGINE_NAME, name, ScriptContext.ENGINE_SCOPE);
        return scriptEngine;
    }

    private NashornScriptEngineFactory getFactory() {
        return factory;
    }

    @Override
    public Object callMethod(ScriptEngine scriptEngine, String method, Object... args) throws NoSuchMethodException, ScriptException {
        Invocable invocable = (Invocable) scriptEngine;
        if (log.isDebugEnabled()) {
            log.debug("Calling method [" + method + "] with parameters " + ArrayUtils.toString(args) + " in ScriptEngine [" + getScriptEngineName(scriptEngine)
                    + "]");
        }
        Object result = invocable.invokeFunction(method, args);
        if (log.isDebugEnabled()) {
            log.debug("Calling method [" + method + "] with parameters " + ArrayUtils.toString(args) + " in ScriptEngine [" + getScriptEngineName(scriptEngine)
                    + "] result is : " + result);
        }
        return result;
    }

    /**
     * Return the name of a script engine (see the getEngine method)
     * 
     * @param scriptEngine
     * @return
     */
    private static String getScriptEngineName(ScriptEngine scriptEngine) {
        try {
            return String.valueOf(scriptEngine.getContext().getAttribute(SCRIPT_ENGINE_NAME));
        } catch (Exception e) {
            return "Unknown";
        }
    }

}
