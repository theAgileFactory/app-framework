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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import framework.services.configuration.II18nMessagesPlugin;
import play.Logger;
import play.mvc.Http;
import play.mvc.Http.Request;

/**
 * The taf tree helper provides the methods to manage the objects that represent
 * a node.
 * 
 * @author Johann Kohler
 * 
 */
public class TafTreeHelper {
    /**
     * Get the id of node from the JSON request.
     * 
     * @param request
     *            the http request
     */
    public static Long getId(Request request) {

        JsonNode json = request.body().asJson();
        if (json.findPath("id").canConvertToLong()) {
            return json.findPath("id").asLong();
        }
        return null;

    }

    /**
     * Get the action name from the JSON request.
     * 
     * @param request
     *            the http request
     */
    public static String getAction(Request request) {

        JsonNode json = request.body().asJson();
        String action = json.findPath("action").asText();
        if (!action.equals("")) {
            return action;
        }

        return null;
    }

    /**
     * Fill a node object (meaning an object that implements the ITafTreeNode
     * interface) with the values corresponding to a specific action.
     * 
     * The actions are:<br/>
     * -addRoot: create a new root node<br/>
     * -add: create a new node that is not a root<br/>
     * -edit: edit the name of a node<br/>
     * -changeOrder: change the order of a node<br/>
     * -delete: delete a node
     * 
     * @param request
     *            the http request
     * @param node
     *            the node to fill
     * @param messagesPlugin
     *            the i18n management service
     */
    public static void fill(Request request, ITafTreeNode node, II18nMessagesPlugin messagesPlugin) {

        JsonNode json = request.body().asJson();

        String action = json.findPath("action").asText();
        String name = json.findPath("name").asText();
        int order = json.findPath("order").asInt();
        Long parentId = json.findPath("parentId").asLong();

        if (action.equals("addRoot")) {

            node.setDeleted(false);
            node.setManageable(true);
            setName(true, node, name, messagesPlugin);
            node.setOrder(order);
            node.setParent(null);

        } else if (action.equals("add")) {

            node.setDeleted(false);
            node.setManageable(true);
            setName(true, node, name, messagesPlugin);
            node.setOrder(order);
            node.setParent(parentId);

        } else if (action.equals("edit")) {

            setName(false, node, name, messagesPlugin);

        } else if (action.equals("changeOrder")) {

            node.setOrder(order);

        } else if (action.equals("delete")) {

            node.setDeleted(true);

        } else {

            Logger.debug("Unknown action: " + action);
            throw new IllegalArgumentException();

        }

    }

    /**
     * Get the JSON response (used by the taf tree jQuery plugin) for a node.
     * 
     * @param node
     *            the node to jsonify
     * @param messagesPlugin
     *            the i18n management service
     */
    public static JsonNode get(ITafTreeNode node, II18nMessagesPlugin messagesPlugin) {

        JSTafTreeNode jsNode = new JSTafTreeNode(node.getId(), getName(node, messagesPlugin), node.isManageable(), node.getOrder(), node.hasChildren(),
                node.getLastChildrenOrder());

        ObjectMapper mapper = new ObjectMapper();

        return mapper.valueToTree(jsNode);
    }

    /**
     * Get the JSON response for a list of nodes.
     * 
     * @param nodes
     *            the nodes to jsonify
     * @param messagesPlugin
     *            the i18n management service
     */
    public static JsonNode gets(List<? extends ITafTreeNode> nodes, II18nMessagesPlugin messagesPlugin) {

        List<JSTafTreeNode> jsNodes = new ArrayList<JSTafTreeNode>();
        for (ITafTreeNode node : nodes) {
            JSTafTreeNode jsNode = new JSTafTreeNode(node.getId(), getName(node, messagesPlugin), node.isManageable(), node.getOrder(), node.hasChildren(),
                    node.getLastChildrenOrder());
            jsNodes.add(jsNode);
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.valueToTree(jsNodes);

    }

    /**
     * Set the name of a node as a i18n key.
     * 
     * @param isNew
     *            set to true if the node doesn't exist in the DB
     * @param node
     *            the node
     * @param name
     *            the name
     * @param messagesPlugin
     *            the i18n management service
     */
    private static void setName(boolean isNew, ITafTreeNode node, String name, II18nMessagesPlugin messagesPlugin) {

        String key = null;
        if (isNew) {
            key = "node." + UUID.randomUUID();
        } else {
            key = node.getName();
        }

        String lang = Http.Context.current().lang().code();

        messagesPlugin.add(key, name, lang);

        node.setName(key);
    }

    /**
     * Get the name of a node.
     * 
     * @param node
     *            the node
     */
    public static String getName(ITafTreeNode node, II18nMessagesPlugin messagesPlugin) {
        return messagesPlugin.get(node.getName());
    }
}
