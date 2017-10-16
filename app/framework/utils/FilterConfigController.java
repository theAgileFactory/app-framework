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

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import be.objectify.deadbolt.java.actions.SubjectPresent;
import framework.services.account.IAccountManagerPlugin;
import framework.services.account.IUserAccount;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.Language;
import framework.services.notification.INotificationManagerPlugin;
import framework.services.session.IUserSessionManagerPlugin;
import models.framework_models.account.NotificationCategory;
import models.framework_models.account.NotificationCategory.Code;
import models.framework_models.account.Principal;
import models.framework_models.common.FilterConfiguration;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * The actions for the filter congif.
 * 
 * @author Johann Kohler
 *
 */
public class FilterConfigController extends Controller {

    @Inject
    private IUserSessionManagerPlugin userSessionManagerPlugin;

    @Inject
    private IAccountManagerPlugin accountManagerPlugin;

    @Inject
    private II18nMessagesPlugin i18nMessagesPlugin;

    @Inject
    private INotificationManagerPlugin notificationManagerPlugin;

    /**
     * Create a new filter based on the default one.
     * 
     * This action is called when the default filter is selected and the user
     * clicks on the save button.
     */
    @SubjectPresent
    public Result filterConfigurationCreate() {

        String uid = getUserSessionManagerPlugin().getUserSessionId(ctx());
        String tableId = request().body().asJson().get("tableId").asText();
        String route = request().body().asJson().get("route").asText();
        String name = request().body().asJson().get("name").asText();
        String dataType = request().body().asJson().get("dataType").asText();

        try {

            FilterConfiguration selectedFilter = FilterConfiguration.getSelectedFilterConfiguration(uid, dataType);
            if (selectedFilter != null) {
                selectedFilter.isSelected = false;
                selectedFilter.save();
            }

            FilterConfiguration defaultFilter = FilterConfiguration.getDefaultFilterConfiguration(uid, dataType);

            FilterConfiguration newFilter = new FilterConfiguration();
            newFilter.configuration = defaultFilter.configuration;
            newFilter.dataType = dataType;
            newFilter.isDefault = false;
            newFilter.isSelected = true;
            newFilter.name = name;
            newFilter.principal = Principal.getPrincipalFromUid(uid);

            Random random = new SecureRandom();
            String letters = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
            String sharedKey = "";
            for (int i = 0; i < 12; i++) {
                int index = (int) (random.nextDouble() * letters.length());
                sharedKey += letters.substring(index, index + 1);
            }
            newFilter.sharedKey = sharedKey;

            newFilter.save();

            return ok(views.html.framework_views.parts.table.filter_configuration_selector.render(tableId, route, newFilter));

        } catch (Exception e) {
            Logger.error("error with filterConfigurationCreate for dataType '" + dataType + "' and uid '" + uid + "'", e);
            return internalServerError();
        }

    }

    /**
     * Save an existing filter configuration (set the default configuration in
     * the selected configuration).
     * 
     * This action is called only when a non-default filter is selected.
     */
    @SubjectPresent
    public Result filterConfigurationSave() {

        String uid = getUserSessionManagerPlugin().getUserSessionId(ctx());
        String tableId = request().body().asJson().get("tableId").asText();
        String route = request().body().asJson().get("route").asText();
        Long filterConfigurationId = request().body().asJson().get("filterConfigurationId").asLong();

        try {

            FilterConfiguration selectedFilter = FilterConfiguration.getById(filterConfigurationId);

            if (!selectedFilter.principal.uid.equalsIgnoreCase(uid) || selectedFilter.isDefault) {
                return forbidden();
            }

            FilterConfiguration defaultFilter = FilterConfiguration.getDefaultFilterConfiguration(uid, selectedFilter.dataType);

            selectedFilter.configuration = defaultFilter.configuration;
            selectedFilter.save();

            return ok(views.html.framework_views.parts.table.filter_configuration_selector.render(tableId, route, selectedFilter));

        } catch (Exception e) {
            Logger.error("error with filterConfigurationSave for id '" + filterConfigurationId + "'", e);
            return internalServerError();
        }

    }

    /**
     * Clears the current default filter and get back to the initial one
     */
    @SubjectPresent
    public Result filterConfigurationClear() {

        String uid = getUserSessionManagerPlugin().getUserSessionId(ctx());
        String tableId = request().body().asJson().get("tableId").asText();
        String route = request().body().asJson().get("route").asText();
        String dataType = request().body().asJson().get("dataType").asText();

        // Deselect the currently selected filter configuration
        FilterConfiguration.getSelectedFilterConfiguration(uid, dataType).deselect();

        // Reset the default filter to the initial filter configuration
        FilterConfiguration defaultFilterConfiguration = FilterConfiguration.getDefaultFilterConfiguration(uid, dataType).reset();

        return ok(views.html.framework_views.parts.table.filter_configuration_selector.render(tableId, route, defaultFilterConfiguration));
    }

    /**
     * Change the selected filter configuration.
     * 
     * -Set the isSelected flag to false for the old selected filter<br/>
     * -Set the isSeelcted flag to true for the new selected filter (only if it
     * is not the default)<br/>
     * -Set the selected configuration in the default configuration<br/>
     */
    @SubjectPresent
    public Result filterConfigurationChange() {

        String uid = getUserSessionManagerPlugin().getUserSessionId(ctx());
        String tableId = request().body().asJson().get("tableId").asText();
        String route = request().body().asJson().get("route").asText();
        Long filterConfigurationId = request().body().asJson().get("filterConfigurationId").asLong();

        try {

            FilterConfiguration selectedFilter = FilterConfiguration.getById(filterConfigurationId);

            if (!selectedFilter.principal.uid.equalsIgnoreCase(uid)) {
                return forbidden();
            }

            // set the isSelected flag
            FilterConfiguration oldSelectedFilter = FilterConfiguration.getSelectedFilterConfiguration(uid, selectedFilter.dataType);
            if (!oldSelectedFilter.isDefault) {
                oldSelectedFilter.isSelected = false;
                oldSelectedFilter.save();
            }
            if (!selectedFilter.isDefault) {
                selectedFilter.isSelected = true;
                selectedFilter.save();
            }

            // set the new selected filter configuration in the default
            // configuration
            if (!selectedFilter.isDefault) {
                FilterConfiguration defaultFilter = FilterConfiguration.getDefaultFilterConfiguration(uid, selectedFilter.dataType);
                defaultFilter.configuration = selectedFilter.configuration;
                defaultFilter.save();
            }

            return ok(views.html.framework_views.parts.table.filter_configuration_selector.render(tableId, route, selectedFilter));

        } catch (Exception e) {
            Logger.error("error with filterConfigurationChange for id '" + filterConfigurationId + "'", e);
            return internalServerError();
        }

    }

    /**
     * Edit the name of a filter.
     */
    @SubjectPresent
    public Result filterConfigurationEdit() {

        String uid = getUserSessionManagerPlugin().getUserSessionId(ctx());
        String tableId = request().body().asJson().get("tableId").asText();
        String route = request().body().asJson().get("route").asText();
        String name = request().body().asJson().get("name").asText();
        Long filterConfigurationId = request().body().asJson().get("filterConfigurationId").asLong();

        try {

            FilterConfiguration selectedFilter = FilterConfiguration.getById(filterConfigurationId);

            if (!selectedFilter.principal.uid.equalsIgnoreCase(uid) || selectedFilter.isDefault) {
                return forbidden();
            }

            selectedFilter.name = name;
            selectedFilter.save();

            return ok(views.html.framework_views.parts.table.filter_configuration_selector.render(tableId, route, selectedFilter));

        } catch (Exception e) {
            Logger.error("error with filterConfigurationEdit for id '" + filterConfigurationId + "'", e);
            return internalServerError();
        }

    }

    /**
     * Delete a filter.
     * 
     * The selected filter simply becomes the default.
     */
    @SubjectPresent
    public Result filterConfigurationDelete() {

        String uid = getUserSessionManagerPlugin().getUserSessionId(ctx());
        String tableId = request().body().asJson().get("tableId").asText();
        String route = request().body().asJson().get("route").asText();
        Long filterConfigurationId = request().body().asJson().get("filterConfigurationId").asLong();

        try {

            FilterConfiguration selectedFilter = FilterConfiguration.getById(filterConfigurationId);

            if (!selectedFilter.principal.uid.equalsIgnoreCase(uid) || selectedFilter.isDefault) {
                return forbidden();
            }

            FilterConfiguration defaultFilter = FilterConfiguration.getDefaultFilterConfiguration(uid, selectedFilter.dataType);

            selectedFilter.doDelete();

            return ok(views.html.framework_views.parts.table.filter_configuration_selector.render(tableId, route, defaultFilter));

        } catch (Exception e) {
            Logger.error("error with filterConfigurationDelete for id '" + filterConfigurationId + "'", e);
            return internalServerError();
        }

    }

    /**
     * Share a filter (send a notification to the recipient).
     */
    @SubjectPresent
    public Result filterConfigurationShare() {

        String uid = getUserSessionManagerPlugin().getUserSessionId(ctx());
        String recipient = request().body().asJson().get("recipient").asText();
        String route = request().body().asJson().get("route").asText();
        Long filterConfigurationId = request().body().asJson().get("filterConfigurationId").asLong();

        try {

            FilterConfiguration selectedFilter = FilterConfiguration.getById(filterConfigurationId);

            if (!selectedFilter.principal.uid.equalsIgnoreCase(uid) || selectedFilter.isDefault) {
                return forbidden();
            }

            // get the current user
            IUserAccount account = getAccountManagerPlugin().getUserAccountFromUid(uid);
            String userName = account.getFirstName() + " " + account.getLastName();

            // get the principal (recipient)
            Principal principal = Principal.getPrincipalFromUid(recipient);

            // send the message
            if (principal != null) {

                // get the language
                Language language = new Language(principal.preferredLanguage);

                // construct the title and the message
                String message = null;
                String title = null;
                if (getMessagesPlugin().isLanguageValid(language.getCode())) {
                    message = Msg.get(language.getLang(), "object.filter_configuration.share.notification.message", userName);
                    title = Msg.get(language.getLang(), "object.filter_configuration.share.notification.title");
                } else {
                    message = Msg.get("object.filter_configuration.share.notification.message", userName);
                    title = Msg.get("object.filter_configuration.share.notification.title");
                }

                getNotificationManagerPlugin().sendNotification(recipient, NotificationCategory.getByCode(Code.INFORMATION), title, message,
                        selectedFilter.getLink(route));

                return ok();

            } else {
                return internalServerError();
            }

        } catch (Exception e) {
            Logger.error("error with filterConfigurationShare for id '" + filterConfigurationId + "'", e);
            return internalServerError();
        }

    }

    /**
     * Search a principal with a keyword (used in the share form).
     */
    public Result filterConfigurationSearchPrincipal() {

        String query = request().queryString().get("query") != null ? request().queryString().get("query")[0] : null;
        String value = request().queryString().get("value") != null ? request().queryString().get("value")[0] : null;

        if (query != null) {

            if (!query.equals("*")) {
                query = "*" + query.trim() + "*";
            }

            try {

                ISelectableValueHolderCollection<String> selectableValues = new DefaultSelectableValueHolderCollection<String>();
                List<IUserAccount> userAccounts = getAccountManagerPlugin().getUserAccountsFromName(query);
                if (userAccounts != null) {
                    for (IUserAccount userAccount : userAccounts) {
                        if (userAccount.isActive() && userAccount.isDisplayed()) {
                            selectableValues.add(new DefaultSelectableValueHolder<String>(userAccount.getUid(),
                                    String.format("%s %s", userAccount.getFirstName(), userAccount.getLastName())));
                        }
                    }
                }
                return ok(Utilities.marshallAsJson(selectableValues.getValues()));

            } catch (Exception e) {
                Logger.error("unexpected error with filterConfigurationSearchPrincipal", e);
            }
        }

        if (value != null) {

            try {

                IUserAccount userAccount = getAccountManagerPlugin().getUserAccountFromUid(value);
                ISelectableValueHolder<String> valueHolder = new DefaultSelectableValueHolder<String>(userAccount.getUid(),
                        String.format("%s %s", userAccount.getFirstName(), userAccount.getLastName()));
                return ok(Utilities.marshallAsJson(valueHolder, 0));

            } catch (Exception e) {
                Logger.error("unexpected error with filterConfigurationSearchPrincipal", e);
            }
        }

        return ok(Json.newObject());

    }

    /**
     * Get the user session manager service.
     */
    private IUserSessionManagerPlugin getUserSessionManagerPlugin() {
        return userSessionManagerPlugin;
    }

    /**
     * Get the account manager service.
     */
    private IAccountManagerPlugin getAccountManagerPlugin() {
        return accountManagerPlugin;
    }

    /**
     * Get the i18n messages service.
     */
    private II18nMessagesPlugin getMessagesPlugin() {
        return i18nMessagesPlugin;
    }

    /**
     * Get the notification manager service.
     */
    private INotificationManagerPlugin getNotificationManagerPlugin() {
        return notificationManagerPlugin;
    }

}
