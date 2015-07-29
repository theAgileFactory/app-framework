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
package framework.utils.formats;

import java.util.List;

import framework.utils.IColumnFormatter;
import framework.utils.ISelectableValueHolderCollection;

/**
 * Format a list of values as a String.<br/>
 * The value are displayed:
 * <ul>
 * <li>Either by simply applying a toString to the value (null is displayed as
 * blank)</li>
 * <li>Either by using a hook to a value holder collection to find the
 * displayable name from the value</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <T>
 */
public class ListOfValuesFormatter<T> implements IColumnFormatter<T> {
    private Hook<T> hook;
    private boolean noDescription = false;

    public ListOfValuesFormatter() {
    }

    public ListOfValuesFormatter(Hook<T> hook) {
        this.hook = hook;
    }

    public ListOfValuesFormatter(Hook<T> hook, boolean noDescription) {
        this(hook);
        this.noDescription = noDescription;
    }

    private Hook<T> getHook() {
        return hook;
    }

    @Override
    public String apply(T value, Object cellValue) {
        if (getHook() == null) {
            return views.html.framework_views.parts.formats.display_list_of_values.render((List<?>) cellValue, null).body();
        } else {
            ISelectableValueHolderCollection<?> valueHolderCollection = getHook().getValueHolderCollection(value);
            return views.html.framework_views.parts.formats.display_value_holder_collection.render(valueHolderCollection, isNoDescription()).body();
        }
    }

    /**
     * A hook called when the value is displayed in order to "render" the value
     * using a {@link ISelectableValueHolderCollection}
     * 
     * @author Pierre-Yves Cloux
     * 
     * @param <T>
     */
    public interface Hook<T> {
        public ISelectableValueHolderCollection<?> getValueHolderCollection(T value);
    }

    private boolean isNoDescription() {
        return noDescription;
    }
}
