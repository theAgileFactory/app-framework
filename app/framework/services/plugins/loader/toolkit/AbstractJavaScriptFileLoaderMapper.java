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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Pair;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

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

    private Class<K> objectClass;
    private Context cx;
    private String javaScriptMappingScript;
    private Script script;

    /**
     * Default constructor.
     * 
     * @param objectClass
     *            the object class
     * @param javaScriptMappingScript
     *            the javascript mapping script
     */
    public AbstractJavaScriptFileLoaderMapper(Class<K> objectClass, String javaScriptMappingScript) {
        this.objectClass = objectClass;
        this.javaScriptMappingScript = javaScriptMappingScript;
    }

    @Override
    public void init() throws IOException {
        this.cx = Context.enter();

        // Protect the script against the use of not allowed classes
        final List<String> allowedClasses = Arrays.asList(getObjectClass().getName(), "org.apache.commons.csv.CSVRecord", "java.lang.String");
        this.cx.setClassShutter(new ClassShutter() {
            @Override
            public boolean visibleToScripts(String className) {
                return allowedClasses.contains(className);
            }
        });

        // Compile the script
        this.script = cx.compileString(getJavaScriptMappingScript(), "mappingScript", 1, null);
    }

    @Override
    public boolean convert(CSVRecord record, K loadableObject) {
        return executeJavaScriptMapping(record, loadableObject);
    }

    @Override
    public void close() {
        try {
            Context.exit();
        } catch (Exception e) {
        }
        this.cx = null;
        this.script = null;
    }

    /**
     * Execute the javascript mapping script reading the CSV content and
     * fulfilling the loadable object.
     * 
     * @param record
     *            a CSV file row
     * @param loadableObject
     *            a loadable object
     */
    private boolean executeJavaScriptMapping(final CSVRecord record, ILoadableObject loadableObject) {
        Scriptable scope = getCx().initStandardObjects();
        Object wrappedActorLoadObject = Context.javaToJS(loadableObject, scope);
        Object wrappedCsv = Context.javaToJS(record, scope);
        ScriptableObject.putProperty(scope, JS_VARIABLE_TARGET, wrappedActorLoadObject);
        ScriptableObject.putProperty(scope, JS_VARIABLE_CSV, wrappedCsv);
        getScript().exec(getCx(), scope);
        return (Boolean) ScriptableObject.getProperty(scope, JS_VARIABLE_IGNORE_RECORD);
    }

    /**
     * Get the object class.
     */
    private Class<K> getObjectClass() {
        return objectClass;
    }

    /**
     * Get the context.
     */
    private Context getCx() {
        return cx;
    }

    /**
     * Get the javascript mapping script.
     */
    private String getJavaScriptMappingScript() {
        return javaScriptMappingScript;
    }

    /**
     * Get the script.
     */
    private Script getScript() {
        return script;
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

}
