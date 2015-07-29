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

import java.net.URL;

/**
 * An object that encapsulate a link (to be rendered as a URL).
 * 
 * @author Pierre-Yves Cloux
 */
public class Link {
    // Default Bootstrap styles
    public static final String STYLE_DANGER = "btn btn-small btn-danger";
    public static final String STYLE_INFO = "btn btn-small btn-info";
    public static final String STYLE_PRIMARY = "btn btn-small btn-primary";
    public static final String STYLE_SUCCESS = "btn btn-small btn-success";
    public static final String STYLE_WARNING = "btn btn-small btn-warning";
    public static final String STYLE_LINK = "btn btn-link";

    private String hRefUrl;
    private String label;
    private String style = STYLE_LINK;

    /**
     * Creates a link from the specified route with the specified label.<br/>
     * The URL for the link is provided as a {@link URL} object.
     * 
     * @param hRefUrl
     * @param label
     * @param style
     *            the CSS style to apply to the link
     */
    public Link(String hRefUrl, String label, String style) {
        super();
        this.hRefUrl = hRefUrl;
        this.label = label;
        this.style = style;
    }

    /**
     * Creates a link from the specified route with the specified label.<br/>
     * The default style STYLE_LINK is applied. The URL for the link is provided
     * as a {@link URL} object.
     * 
     * @param hRefUrl
     * @param label
     */
    public Link(String hRefUrl, String label) {
        super();
        this.hRefUrl = hRefUrl;
        this.label = label;
    }

    /**
     * Return a String representation of the URL
     * 
     * @return a URL as String
     */
    public String getUrl() {
        return gethRefUrl();
    }

    public String getLabel() {
        return label;
    }

    public String getStyle() {
        return style;
    }

    private String gethRefUrl() {
        return hRefUrl;
    }
}
