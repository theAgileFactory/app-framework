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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import framework.commons.IFrameworkConstants;
import framework.security.SecurityUtils;
import play.mvc.Call;
import play.mvc.Http.Context;

/**
 * Root class for the various navigation structures.<br/>
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class Menu {
    private List<MenuItem> menuItems = Collections.synchronizedList(new ArrayList<MenuItem>());

    public Menu() {
    }

    public List<MenuItem> getMenuItems() {
        return this.menuItems;
    }

    /**
     * Add a menu item to the current menu.<br/>
     * The menu item is added after the ones previously added.
     * 
     * @param menuItem
     *            a menuItem
     */
    public void addMenuItem(MenuItem menuItem) {
        this.menuItems.add(menuItem);
    }

    /**
     * Add a menu item as a sub-menuItem of a top menu item
     * 
     * @param uuid
     *            the uuid of a top level menu item
     * @param menuItem
     *            a menuItem
     */
    public boolean addSubMenuItemTo(String uuid, MenuItem newMenuItem) {
        for (MenuItem menuItem : getMenuItems()) {
            if (menuItem.getUuid().equals(uuid) && menuItem instanceof HeaderMenuItem) {
                HeaderMenuItem header = (HeaderMenuItem) menuItem;
                header.addSubMenuItem(newMenuItem);
                return true;
            }
        }
        return false;
    }

    /**
     * Add the specified menu item after another menu item identified by the
     * specified uuid
     * 
     * @param uuid
     *            the uuid of the menu item to look for
     * @param newMenuItem
     *            a new menu item to be added
     * @return true if the uuid was found add the menu item added
     */
    public boolean addMenuItemAfter(String uuid, MenuItem newMenuItem) {
        if (!StringUtils.isBlank(uuid)) {
            return recursiveAddMenuItem(true, uuid, newMenuItem, getMenuItems());
        }
        return false;
    }

    /**
     * Add the specified menu item before another menu item identified by the
     * specified uuid
     * 
     * @param uuid
     *            the uuid of the menu item to look for
     * @param newMenuItem
     *            a new menu item to be added
     * @return true if the uuid was found add the menu item added
     */
    public boolean addMenuItemBefore(String uuid, MenuItem newMenuItem) {
        if (!StringUtils.isBlank(uuid)) {
            return recursiveAddMenuItem(false, uuid, newMenuItem, getMenuItems());
        }
        return false;
    }

    private boolean recursiveAddMenuItem(boolean addAfter, String uuid, MenuItem newMenuItem, List<MenuItem> subMenuItems) {
        List<MenuItem> copySubMenuItems = new ArrayList<MenuItem>();
        copySubMenuItems.addAll(subMenuItems);
        for (MenuItem menuItem : copySubMenuItems) {
            if (menuItem.getUuid().equals(uuid)) {
                int index = subMenuItems.indexOf(menuItem);
                if (addAfter) {
                    subMenuItems.add(index + 1, newMenuItem);
                } else {
                    if (index != 0) {
                        index = index - 1;
                    }
                    subMenuItems.add(index, newMenuItem);
                }
                return true;
            } else {
                if (menuItem.hasSubMenuItems()) {
                    if (recursiveAddMenuItem(addAfter, uuid, newMenuItem, menuItem.getSubMenuItems())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Remove one or more menu items associated with the specified unique id.
     * <br/>
     * The method search recursively for the corresponding menu item.
     * 
     * @param uuid
     *            a unique Id.
     */
    public void removeMenuItem(String... uuids) {
        List<String> uuidList = Arrays.asList(uuids);
        // Remove the blank uuid from the list (not allowed)
        for (String uuid : uuids) {
            if (StringUtils.isBlank(uuid)) {
                uuidList.remove(uuid);
            }
        }
        // Parse recursively the top menu to remove all the matching uuids
        recursiveRemoveMenuItem(uuidList, getMenuItems());
    }

    /**
     * Private recursive method for finding matching menuItems
     * 
     * @param uuidList
     *            a list of UUID to remove
     * @param subMenuItems
     */
    private void recursiveRemoveMenuItem(List<String> uuidList, List<MenuItem> subMenuItems) {
        List<MenuItem> copySubMenuItems = new ArrayList<MenuItem>();
        copySubMenuItems.addAll(subMenuItems);
        for (MenuItem menuItem : copySubMenuItems) {
            if (uuidList.contains(menuItem.getUuid())) {
                subMenuItems.remove(menuItem);
            } else {
                if (menuItem.hasSubMenuItems()) {
                    recursiveRemoveMenuItem(uuidList, menuItem.getSubMenuItems());
                }
            }
        }
    }

    public void clear() {
        if (getMenuItems() != null) {
            getMenuItems().clear();
        }
    }

    /**
     * The class implementing an item within a MenuBar.<br/>
     * An item has :
     * <ul>
     * <li>A label : the text that is to be displayed (actually a {@link Msg}
     * key)</li>
     * <li>A CSS icon: the CSS class of the menu item icon</li>
     * <li>Some permissions : the permissions which restrict the display of the
     * menu item.</li> Le permissions are associated with AND within an array
     * and with OR between the arrays.
     * <li>A flag which tells if the menu item is a separator or not (by default
     * false)</li>
     * </ul>
     */
    public abstract static class MenuItem {
        private String uuid;
        private String label;
        private String cssIcon;
        private boolean isSelected = false;
        private boolean isImportant = false;
        private boolean isAlwaysDisplayed = false;
        private List<String[]> authorizedPermissions;
        private MenuItem parent;

        public MenuItem(String label) {
            this.uuid = UUID.randomUUID().toString();
            this.label = label;
        }

        public MenuItem(String uuid, String label) {
            this(label);
            this.uuid = uuid;
        }

        public MenuItem(String label, String cssIcon, boolean isSelected) {
            this(label);
            this.cssIcon = cssIcon;
            this.isSelected = isSelected;
        }

        public MenuItem(String uuid, String label, String cssIcon, boolean isSelected) {
            this(uuid, label);
            this.cssIcon = cssIcon;
            this.isSelected = isSelected;
        }

        public String getLabel() {
            return label;
        }

        public String getCssIcon() {
            return cssIcon;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public boolean isImportant() {
            return isImportant;
        }

        public void setIsImportant(boolean isImportant) {
            this.isImportant = isImportant;
        }

        public void setIsAlwaysDisplayed(boolean isAlwaysDisplayed) {
            this.isAlwaysDisplayed = isAlwaysDisplayed;
        }

        public List<String[]> getAuthorizedPermissions() {
            if (authorizedPermissions == null && getParent() != null) {
                return getParent().getAuthorizedPermissions();
            }
            if (authorizedPermissions == null) {
                // If no roles is found then add a default one
                authorizedPermissions = Collections.synchronizedList(new ArrayList<String[]>());
                authorizedPermissions.add(new String[] { IFrameworkConstants.DEFAULT_PERMISSION_PRIVATE });
            }
            return authorizedPermissions;
        }

        public boolean restrict() {
            try {
                return this.isAlwaysDisplayed || SecurityUtils.restrict(this.getAuthorizedPermissions(), Context.current());
            } catch (Exception e) {
                return false;
            }
        }

        public void setAuthorizedPermissions(List<String[]> authorizedRoles) {
            this.authorizedPermissions = authorizedRoles;
        }

        public abstract boolean hasSubMenuItems();

        public abstract List<MenuItem> getSubMenuItems();

        public abstract List<MenuItem> getSortedSubMenuItems();

        public abstract String getActionUrl();

        public abstract ISelectableValueHolderCollection<?> getValues();

        public abstract boolean isSeparator();

        public MenuItem getParent() {
            return parent;
        }

        public String getUuid() {
            return uuid;
        }

        protected void setParent(MenuItem parent) {
            this.parent = parent;
        }
    }

    /**
     * The class implementing an item within a MenuBar.<br/>
     * This is a <b>Header</b> of a submenu which has several sub-items.<br/>
     * A header is not clickable. An item has :
     * <ul>
     * <li>A list of sub-menu items</li>
     * </ul>
     */
    public static class HeaderMenuItem extends MenuItem {

        private List<MenuItem> subMenuItems = Collections.synchronizedList(new ArrayList<MenuItem>());

        public HeaderMenuItem(String label) {
            super(label);
        }

        public HeaderMenuItem(String label, String cssIcon, boolean isSelected) {
            super(label, cssIcon, isSelected);
        }

        public HeaderMenuItem(String uuid, String label) {
            super(uuid, label);
        }

        public HeaderMenuItem(String uuid, String label, String cssIcon, boolean isSelected) {
            super(uuid, label, cssIcon, isSelected);
        }

        @Override
        public List<MenuItem> getSortedSubMenuItems() {
            List<MenuItem> sortedSubMenuItems = new ArrayList<MenuItem>(subMenuItems);
            Collections.sort(sortedSubMenuItems, new Comparator<MenuItem>() {
                @Override
                public int compare(MenuItem m1, MenuItem m2) {
                    return m1.getLabel().compareTo(m2.getLabel());
                }
            });
            return sortedSubMenuItems;
        }

        @Override
        public List<MenuItem> getSubMenuItems() {
            return subMenuItems;
        }

        public void addSubMenuItem(MenuItem menuItem) {
            menuItem.setParent(this);
            this.subMenuItems.add(menuItem);
        }

        public void removeSubMenuItems(String... uuids) {
            List<String> uuidList = Arrays.asList(uuids);
            // Remove the blank uuid from the list (not allowed)
            for (String uuid : uuids) {
                if (StringUtils.isBlank(uuid)) {
                    uuidList.remove(uuid);
                }
            }
            // Remove the sub menu item if available
            List<MenuItem> toBeRemovedMenuItems = new ArrayList<MenuItem>();
            if (subMenuItems != null) {
                for (MenuItem subMenuItem : subMenuItems) {
                    if (uuidList.contains(subMenuItem.getUuid())) {
                        toBeRemovedMenuItems.add(subMenuItem);
                    }
                }
            }
            for (MenuItem subMenuItem : toBeRemovedMenuItems) {
                this.subMenuItems.remove(subMenuItem);
            }
        }

        public boolean hasSubMenuItems() {
            return getSubMenuItems().size() != 0;
        }

        @Override
        public String getActionUrl() {
            throw new IllegalArgumentException("A HeaderMenuItem has no action but must have sub items : " + getLabel());
        }

        @Override
        public boolean isSeparator() {
            return false;
        }

        @Override
        public ISelectableValueHolderCollection<?> getValues() {
            return null;
        }
    }

    /**
     * The class implementing an item within a MenuBar.<br/>
     * This is a <b>Clickable</b> item which is associated with:<br/>
     * <ul>
     * <li>A route : the path to which this menu is associated</li>
     * <li>An external URL : an URL to an external web site</li>
     * </ul>
     */
    public static class ClickableMenuItem extends MenuItem {
        private Call route;
        private String externalUrl;

        public ClickableMenuItem(String label, Call route) {
            super(label);
            this.route = route;
        }

        public ClickableMenuItem(String label, Call route, String cssIcon, boolean isSelected) {
            super(label, cssIcon, isSelected);
            this.route = route;
        }

        public ClickableMenuItem(String label, String externalUrl) {
            super(label);
            this.externalUrl = externalUrl;
        }

        public ClickableMenuItem(String label, String externalUrl, String cssIcon, boolean isSelected) {
            super(label, cssIcon, isSelected);
            this.externalUrl = externalUrl;
        }

        public ClickableMenuItem(String uuid, String label, Call route) {
            super(uuid, label);
            this.route = route;
        }

        public ClickableMenuItem(String uuid, String label, Call route, String cssIcon, boolean isSelected) {
            super(uuid, label, cssIcon, isSelected);
            this.route = route;
        }

        public ClickableMenuItem(String uuid, String label, String externalUrl) {
            super(uuid, label);
            this.externalUrl = externalUrl;
        }

        public ClickableMenuItem(String uuid, String label, String externalUrl, String cssIcon, boolean isSelected) {
            super(uuid, label, cssIcon, isSelected);
            this.externalUrl = externalUrl;
        }

        @Override
        public String getActionUrl() {
            if (route != null) {
                return route.url();
            }
            return this.externalUrl;
        }

        @Override
        public boolean hasSubMenuItems() {
            return false;
        }

        @Override
        public List<MenuItem> getSortedSubMenuItems() {
            throw new IllegalArgumentException("A ClickableMenuItem has no sub items but must have an action URL" + getLabel());
        }

        @Override
        public List<MenuItem> getSubMenuItems() {
            throw new IllegalArgumentException("A ClickableMenuItem has no sub items but must have an action URL" + getLabel());
        }

        @Override
        public boolean isSeparator() {
            return false;
        }

        @Override
        public ISelectableValueHolderCollection<?> getValues() {
            return null;
        }
    }

    /**
     * A menu item to be used as a separator (no label, no action, no sub menu
     * item)
     */
    public static class SeparatorMenuItem extends MenuItem {

        public SeparatorMenuItem() {
            super("If this is displayed, this is not normal");
        }

        @Override
        public boolean hasSubMenuItems() {
            return false;
        }

        @Override
        public List<MenuItem> getSortedSubMenuItems() {
            return null;
        }

        @Override
        public List<MenuItem> getSubMenuItems() {
            return null;
        }

        @Override
        public String getActionUrl() {
            return "";
        }

        @Override
        public boolean isSeparator() {
            return true;
        }

        @Override
        public ISelectableValueHolderCollection<?> getValues() {
            return null;
        }
    }
}
