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
package framework.utils;

/**
 * A default implementation of the {@link ISelectableValueHolder} interface
 * 
 * @author Pierre-Yves Cloux
 * 
 * @param <T>
 */
public class DefaultSelectableValueHolder<T> implements ISelectableValueHolder<T> {
    private T value;
    private String name;
    private String description;
    private String url;
    private Integer order;

    public DefaultSelectableValueHolder() {
    }

    public DefaultSelectableValueHolder(T value, String name) {
        super();
        this.value = value;
        this.name = name;
        this.order = null;
    }

    public DefaultSelectableValueHolder(T value, String name, String description) {
        super();
        this.value = value;
        this.name = name;
        this.description = description;
        this.order = null;
    }

    public DefaultSelectableValueHolder(T value, String name, String description, String url) {
        super();
        this.value = value;
        this.name = name;
        this.description = description;
        this.url = url;
        this.order = null;
    }

    @Override
    public String getName() {
        return Msg.get(this.name);
    }

    @Override
    public String getUrl() {
        return this.url;
    }

    @Override
    public String getDescription() {
        return Msg.get(this.description);
    }

    @Override
    public T getValue() {
        return this.value;
    }

    @Override
    public int compareTo(Object o) {

        @SuppressWarnings("unchecked")
        DefaultSelectableValueHolder<T> v = (DefaultSelectableValueHolder<T>) o;

        if (this.order == null || v.order == null) {
            return this.getName().compareTo(v.getName());
        } else {
            return this.order > v.order ? +1 : this.order < v.order ? -1 : 0;
        }
    }

    public void setValue(T value) {
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

}
