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
package framework.services.plugins.loader.toolkit;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Pair;

import framework.services.script.IScriptService;

/**
 * A default implementation of the {@link IGenericFileLoaderMapper} interface
 * which is using a JavaScript mapping expression to convert a CSV row into a
 * {@link ILoadableObject}.
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <K>
 */
public abstract class AbstractJavaScriptFileLoaderMapper<K extends ILoadableObject> implements IGenericFileLoaderMapper<K> {

    /**
     * The name of the javascript variable which contains the object to be
     * mapped.
     */
    public static final String JS_VARIABLE_TARGET = "target";

    /**
     * The name of the javascript variable which contains the CSV record.
     */
    public static final String JS_VARIABLE_CSV = "csv";

    /**
     * The name of the javascript variable which contains the boolean which tels
     * if the line must be ignored.
     */
    public static final String JS_VARIABLE_IGNORE_RECORD = "ignoreRecord";

    /**
     * Name of the method which will wraps the mapping script
     */
    public static final String JS_WRAPPER_METHOD_NAME = "action";

    /**
     * A javascript function to wrap the mapping script
     */
    public static final String JS_WRAPPER_METHOD = "function " + JS_WRAPPER_METHOD_NAME + "(" + JS_VARIABLE_TARGET + "," + JS_VARIABLE_CSV + "){\n%s\nreturn "
            + JS_VARIABLE_IGNORE_RECORD + ";\n}";

    private Class<K> objectClass;
    private String javaScriptMappingScript;
    private IScriptService scriptService;
    private ScriptEngine scriptEngine;

    /**
     * Default constructor.
     * 
     * @param objectClass
     *            the object class
     * @param javaScriptMappingScript
     *            the javascript mapping script
     * @param scriptService
     *            the service which is managing the {@link ScriptEngine}
     */
    public AbstractJavaScriptFileLoaderMapper(Class<K> objectClass, String javaScriptMappingScript, IScriptService scriptService) {
        this.objectClass = objectClass;
        this.javaScriptMappingScript = javaScriptMappingScript;
        this.scriptService = scriptService;
    }

    @Override
    public void init() throws ScriptException {
        this.scriptEngine = getScriptService().getEngine("FileLoaderScript" + getObjectClass());
        this.scriptEngine.eval(String.format(JS_WRAPPER_METHOD, getJavaScriptMappingScript()));
    }

    @Override
    public boolean convert(CSVRecord record, K loadableObject) throws NoSuchMethodException, ScriptException {
        return executeJavaScriptMapping(record, loadableObject);
    }

    @Override
    public void close() {
        this.scriptEngine = null;
    }

    /**
     * Execute the javascript mapping script reading the CSV content and
     * fulfilling the loadable object.
     * 
     * @param record
     *            a CSV file row
     * @param loadableObject
     *            a loadable object
     * @throws ScriptException
     * @throws NoSuchMethodException
     */
    private synchronized boolean executeJavaScriptMapping(final CSVRecord record, ILoadableObject loadableObject)
            throws NoSuchMethodException, ScriptException {
        return (Boolean) getScriptService().callMethod(getScriptEngine(), JS_WRAPPER_METHOD_NAME, loadableObject, record);
    }

    /**
     * Get the object class.
     */
    private Class<K> getObjectClass() {
        return objectClass;
    }

    /**
     * Get the javascript mapping script.
     */
    private String getJavaScriptMappingScript() {
        return javaScriptMappingScript;
    }

    @Override
    public K createNewLoadableObject() {
        try {
            return getObjectClass().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while creating a new object", e);
        }
    }

    @Override
    public Map<Long, String> validate(List<K> listOfLoadedObjects) throws IOException {
        Map<Long, String> invalidRows = new HashMap<Long, String>();
        if (listOfLoadedObjects != null) {
            for (K loadedObject : listOfLoadedObjects) {
                Pair<Boolean, String> result = loadedObject.validateAndComplete();
                if (!result.getLeft()) {
                    invalidRows.put(loadedObject.getSourceRowNumber(), result.getRight());
                }
            }
        }
        return invalidRows;
    }

    private IScriptService getScriptService() {
        return scriptService;
    }

    private ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

}
