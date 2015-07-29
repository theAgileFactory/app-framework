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
package framework.services.kpi;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;

import play.Logger;
import framework.services.kpi.Kpi.DataType;

/**
 * The custom KPI computation class (for an internal and non-standard KPI).
 * 
 * @author Johann Kohler
 */
public class CustomKpi implements IKpiRunner {

    @Override
    public BigDecimal computeMain(Kpi kpi, Long objectId) {
        return computeValue(kpi, objectId, DataType.MAIN);
    }

    @Override
    public BigDecimal computeAdditional1(Kpi kpi, Long objectId) {
        return computeValue(kpi, objectId, DataType.ADDITIONAL1);
    }

    @Override
    public BigDecimal computeAdditional2(Kpi kpi, Long objectId) {
        return computeValue(kpi, objectId, DataType.ADDITIONAL2);
    }

    @Override
    public String link(Long objectId) {
        return null;
    }

    /**
     * Compute a value.
     * 
     * @param kpi
     *            the KPI
     * @param objectId
     *            the object id
     * @param dataType
     *            the value type
     * @return
     */
    private BigDecimal computeValue(Kpi kpi, Long objectId, DataType dataType) {

        Object object = kpi.getKpiObjectsContainer().getObjectByIdForKpi(objectId);

        BigDecimal value = null;

        Context cx = null;
        try {

            cx = Context.enter();

            WrapFactory wrapFactory = new WrapFactory();
            wrapFactory.setJavaPrimitiveWrap(false);
            cx.setWrapFactory(wrapFactory);

            // Protect the script against the use of not allowed classes
            final List<String> allowedClasses = Arrays.asList(kpi.getObjectType());
            cx.setClassShutter(new ClassShutter() {
                @Override
                public boolean visibleToScripts(String className) {
                    return allowedClasses.contains(className);
                }
            });

            // Compile the script
            Script script = cx.compileString(kpi.getComputationJsCode(dataType), "colorScript", 1, null);

            // Inject the values
            Scriptable scope = cx.initStandardObjects();
            ScriptableObject.putProperty(scope, "object", object);

            Object result = script.exec(cx, scope);

            // convert the return value to a BigDecimal
            try {
                value = new BigDecimal((Integer) result);
            } catch (Exception e1) {
                try {
                    value = new BigDecimal((Double) result);
                } catch (Exception e2) {
                    try {
                        value = new BigDecimal((Long) result);
                    } catch (Exception e3) {
                        try {
                            value = (BigDecimal) result;
                        } catch (Exception e4) {
                        }
                    }
                }
            }

            if (value == null) {
                String message = "The " + dataType.name().toLowerCase() + " value for the KPI " + kpi.getUid() + " is not a number";
                Logger.error(message);
            }

        } catch (Exception e) {

            String message = "Error while computing the " + dataType.name().toLowerCase() + " value for the KPI " + kpi.getUid();
            Logger.error(message, e);

        } finally {

            try {
                Context.exit();
            } catch (Exception e) {
            }
        }

        return value;
    }

}
