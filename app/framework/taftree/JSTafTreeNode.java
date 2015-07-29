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
package framework.taftree;

/**
 * Define a node used in the taf tree node jQuery plugin.
 * 
 * @author Johann Kohler
 * 
 */
public class JSTafTreeNode {

    public Long id;
    public String name;
    public boolean manageable;
    public int order;
    public boolean hasChildren;
    public int lastChildrenOrder;

    /**
     * Default constructor.
     * 
     * @param id
     *            the node id
     * @param name
     *            the node name
     * @param manageable
     *            set to true if the node can be managed
     * @param order
     *            the node order
     * @param hasChildren
     *            the node children
     * @param lastChildrenOrder
     *            the higher order of the children
     */
    public JSTafTreeNode(Long id, String name, boolean manageable, int order, boolean hasChildren, int lastChildrenOrder) {
        super();
        this.id = id;
        this.name = name;
        this.manageable = manageable;
        this.order = order;
        this.hasChildren = hasChildren;
        this.lastChildrenOrder = lastChildrenOrder;
    }

}
