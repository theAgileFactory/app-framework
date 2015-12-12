package framework.services.script;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * The standard interface for the service managing {@link ScriptEngine}
 * instances
 * 
 * @author Pierre-Yves Cloux
 */
public interface IScriptService {
    /**
     * Return a script engine
     * 
     * @param name
     *            the name of the engine (please be explicit about its purpose)
     * @return a script engine
     */
    public ScriptEngine getEngine(String name);

    /**
     * Call a script method using the specified script engine which should
     * contains the evaluated code.<br/>
     * WARNING : please ensure that the context in which this method is called
     * is thread safe since {@link ScriptEngine} is not.
     * 
     * @param scriptEngine
     *            a script engine
     * @param method
     *            the method to be called
     * @param args
     *            the parameters for the method
     * @return the returned object if any
     */
    public Object callMethod(ScriptEngine scriptEngine, String method, Object... args) throws NoSuchMethodException, ScriptException;

    /**
     * Evaluate a script dynamically reusing a previously defined ScriptEngine.
     * <br/>
     * WARNING : please ensure that the context in which this method is called
     * is thread safe since {@link ScriptEngine} is not.
     * 
     * @param scriptEngine
     *            a script engine
     * @param script
     *            a script to be evaluated
     * @param scriptContext
     *            a script context which will be used for the evaluation
     * @return the returned object if any
     * @throws ScriptException
     */
    public Object evaluateScript(ScriptEngine scriptEngine, String script, ScriptContext scriptContext) throws ScriptException;

    /**
     * Evaluate a script dynamically.<br/>
     * A script engine will be allocated for thus purpose.
     * 
     * @param scriptName
     *            the name of the script (used in logs)
     * @param script
     *            a script to be evaluated
     * @param scriptContext
     *            a script context which will be used for the evaluation
     * @return the returned object if any
     * @throws ScriptException
     */
    public Object evaluateScript(String scriptName, String script, ScriptContext scriptContext) throws ScriptException;
}