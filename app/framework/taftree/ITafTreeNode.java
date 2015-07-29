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

import java.util.List;

/**
 * The interface for a taf tree node. Each object that represents a node in the
 * taf tree must implement this interface.
 * 
 * @author Johann Kohler
 * 
 */
public interface ITafTreeNode {

    /**
     * Get the id of the node, usually the id in the DB.
     */
    public Long getId();

    /**
     * Set a node as deleted, usually the deleted flag in the DB.
     * 
     * @param deleted
     *            set to true if the node is deleted
     */
    public void setDeleted(boolean deleted);

    /**
     * Get the name of the node.
     */
    public String getName();

    /**
     * Set the name of the node.
     * 
     * @param name
     *            the node name
     */
    public void setName(String name);

    /**
     * Define if the node can be managed (edit, delete, up, down).
     * 
     * Note: the add children possibility is always available even if
     * isManageable returns false.
     */
    public boolean isManageable();

    /**
     * Set the manageable flag, usually represented by a specific flag in the
     * DB.
     * 
     * @param isManageable
     *            set to true if the node can be managed
     */
    public void setManageable(boolean isManageable);

    /**
     * Get the order of the node relative to the other nodes in the same level
     * of the tree, usually represented an order field in the DB.
     */
    public int getOrder();

    /**
     * Set the order of the node
     * 
     * @param order
     *            the node order
     */
    public void setOrder(int order);

    /**
     * Set the parent of the node.
     * 
     * Note: the parent of a node is another node and the root nodes hasn't
     * parent.
     * 
     * @param parentId
     *            the id of the parent node
     */
    public void setParent(Long parentId);

    /**
     * Get the parent node.
     */
    public ITafTreeNode getParent();

    /**
     * Define if a node has at least one child.
     */
    public boolean hasChildren();

    /**
     * Get the children of a node.
     */
    public List<? extends ITafTreeNode> getChildren();

    /**
     * Get the higher order of the children of a node. This value must be
     * computed by comparing all children order.
     */
    public int getLastChildrenOrder();
}
