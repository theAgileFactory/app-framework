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
package framework.datatable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Guillaume Petit
 */
public abstract class TableDefinition implements Serializable {

    public String id;

    public TableConfiguration configuration = new TableConfiguration();

    public void setId(String id) {
        this.id = id;
    }

    public void setAjax(String url, String dataSrc) {
        this.configuration.ajax = new TableConfiguration.Ajax(url, dataSrc);
    }

    public void setDeferRender(boolean deferRender) {
        this.configuration.deferRender = deferRender;
    }

    public void setStateSave(boolean stateSave) {
        this.configuration.stateSave = stateSave;
    }

    public TableConfiguration.Column addColumn(String data, String header) {
        TableConfiguration.Column column = new TableConfiguration.Column(data, header);
        this.configuration.columns.add(column);
        return column;
    }

    public static class TableConfiguration {
        public Ajax ajax;

        public boolean deferRender;

        public boolean stateSave;

        public List<Column> columns = new ArrayList<>();

        public static class Ajax {
            public String url;

            public String dataSrc;

            public Ajax(String url, String dataSrc) {
                this.url = url;
                this.dataSrc = dataSrc;
            }
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Column {
            @JsonIgnore
            public String header;

            public String data;

            public Render render;

            public Column(String data, String header) {
                this.data = data;
                this.header = header;
            }

            public void addRender(RenderType renderType, String data) {
                if (this.render == null) {
                    this.render = new Render();
                }
                switch (renderType) {
                    case ALL:
                        this.render.all = data;
                        break;
                    case DISPLAY:
                        this.render.display = data;
                        break;
                    case SORT:
                        this.render.sort = data;
                        break;
                    case FILTER:
                        this.render.filter = data;
                        break;
                    case TYPE:
                        this.render.type = data;
                        break;
                }
            }

            public enum RenderType {
                ALL, DISPLAY, SORT, FILTER, TYPE,
            }

            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class Render {

                @JsonProperty(value = "_")
                public String all;

                public String display;

                public String sort;

                public String filter;

                public String type;
            }
        }
    }

}