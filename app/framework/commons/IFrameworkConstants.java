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
package framework.commons;

import framework.utils.Msg;

public interface IFrameworkConstants {
    // -------------------------------------------------------------------------------------
    // Framework objects type constant
    // -------------------------------------------------------------------------------------
	/*
	 The prefix for all the objects (models) which are framework objects.
	 This is used to identity certain category of objects in some processes (see attachements for instance)
	 */
    String FRAMEWORK_OBJECT_TYPE_PREFIX="models.framework_models.";
	
    // -------------------------------------------------------------------------------------
    // DataType name constants
    // -------------------------------------------------------------------------------------

    String User = "User";
    String SystemLevelRoleType = "SystemLevelRoleType";

    // -------------------------------------------------------------------------------------
    // Permissions constants
    // -------------------------------------------------------------------------------------
    String DEFAULT_PERMISSION_PRIVATE = "DEFAULT_PERMISSION_PRIVATE";
    String ADMIN_PLUGIN_MANAGER_PERMISSION = "ADMIN_PLUGIN_MANAGER_PERMISSION";

    // -------------------------------------------------------------------------------------
    // Preference constants
    // -------------------------------------------------------------------------------------
    String DISPLAY_LIST_PAGE_SIZE_PREFERENCE = "DISPLAY_LIST_PAGE_SIZE_PREFERENCE";

    /**
     * Input folder in local storage.
     */
    String INPUT_FOLDER_NAME = "inputs";

    /**
     * Output folder in local storage.
     */
    String OUTPUT_FOLDER_NAME = "outputs";

    /**
     * Default value for null / empty data.
     */
    String DEFAULT_VALUE_EMPTY_DATA = "-";

    /**
     * Plugin small image folder template.
     */
    String PLUGIN_SMALL_IMAGE_TEMPLATE = "public/images/plugins/%s/small.png";

    /**
     * Plugin big image folder template.
     */
    String PLUGIN_BIG_IMAGE_TEMPLATE = "public/images/plugins/%s/big.png";

    /**
     * The minimal password length. WARNING : see the JavaScript function
     * "maf_password_rating" in the main.js library. This value is hardcoded
     * into the library.
     */
    int MINIMAL_PASSWORD_LENGTH = 8;

    /**
     * The name of the context argument that is used to inject an object id.
     */
    String ID_NAME_FOR_CONTEXT = "_maf_object_id";

    // -------------------------------------------------------------------------------------
    // Prefix for the data stored into the Play Cache
    // -------------------------------------------------------------------------------------
    /**
     * Prefix for the data stored in cache.
     */
    String MAF_CACHE_PREFIX = "maf.cache.";

    /**
     * Prefix for the user authorization token.<br/>
     * This part of the cache may contains some temporary authorization for a
     * user to access a resource
     */
    String AUTHZ_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "authz.token.";

    /**
     * Authorization for "reading" an attachment (downloading it).
     */
    String ATTACHMENT_READ_AUTHZ_CACHE_PREFIX = IFrameworkConstants.AUTHZ_CACHE_PREFIX + "attachment.read.";

    /**
     * Authorization for "writing" an attachment (downloading it).
     */
    String ATTACHMENT_WRITE_AUTHZ_CACHE_PREFIX = IFrameworkConstants.AUTHZ_CACHE_PREFIX + "attachment.write.";

    /**
     * Prefix for the preferences cache.
     */
    String SYSTEM_PREFERENCE_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "systempreference.";

    /**
     * Prefix for the formats cache (Date format, Number format).
     */
    String FORMATS_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "templateUtils.formats.";

    /**
     * Prefix for the cache which stores the user accounts.
     */
    String USER_ACCOUNT_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "useraccount.";

    /**
     * Prefix for the i18n keys.
     */
    String I18N_RESOURCES_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "i18n.";

    /**
     * Prefix for the dynamic permission keys.
     */
    String DYNAMIC_PERMISSION_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "dynamicpermission.";

    /**
     * Prefix for captcha.
     */
    String CAPTCHA_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "captcha.";

    // -------------------------------------------------------------------------------------
    // The authentication mode for BizDock
    // -------------------------------------------------------------------------------------
    /**
     * The possible authentication mode which can be activated for an
     * application.
     * 
     * @author Pierre-Yves Cloux
     */
    enum AuthenticationMode {
        STANDALONE, /*
                     * The authentication is managed within the play application
                     */
        FEDERATED, /*
                    * The authentication is delegated to a SAMLv2 server and the
                    * credentials provisioning is not managed
                    */
        CAS_SLAVE, /*
                    * The authentication is delegated to CAS but the credentials
                    * provisioning is not managed
                    */
        CAS_MASTER /*
                     * The authentication is delegated to CAS and the
                     * credentials provisioning is managed
                     */
    }

    /**
     * The possible API authentication and authorizations modes.<br/>
     * 
     * @author Pierre-Yves Cloux
     */
    enum ApiAuthzMode {
        APPLICATION_KEY_ONLY, /*
                               * With this mode the authentication system will
                               * look only to an application key, there is no
                               * signature
                               */
        SIGNATURE /*
                   * With this mode the authentication is mode secured and rely
                   * on a secret key which is used to "sign" the request.<br/>
                   * this must be the default mode.
                   */
    }

    /**
     * System preference for storing the authentication mode into the database.
     * <br/>
     * The possible authentication mode are defined by
     * {@link AuthenticationMode}
     */
    String AUTHENTICATION_MODE_PREFERENCE = "AUTHENTICATION_MODE_PREFERENCE";

    /**
     * Preference to select the API authentication and authorization mode.<br/>
     * 
     */
    String API_AUTHZ_MODE_PREFERENCE = "API_AUTHZ_MODE_PREFERENCE";

    /**
     * System preferences to overload base configurations
     */

    String PUBLIC_URL_PREFERENCE = "PUBLIC_URL_PREFERENCE";

    /**
     * System preference for the Swagger API base path
     */

    String SWAGGER_API_BASEPATH_PREFERENCE = "SWAGGER_API_BASEPATH_PREFERENCE";

    /**
     * System preference for the display of milestone names
     */
    String GOVERNANCE_MILESTONE_DISPLAY_PREFERENCE = "GOVERNANCE_MILESTONE_DISPLAY_PREFERENCE";

    /**
     * System preference for SMTP
     */
    String SMTP_HOST_PREFERENCE = "SMTP_HOST_PREFERENCE";
    String SMTP_PORT_PREFERENCE = "SMTP_PORT_PREFERENCE";
    String SMTP_SSL_PREFERENCE = "SMTP_SSL_PREFERENCE";
    String SMTP_TLS_PREFERENCE = "SMTP_TLS_PREFERENCE";
    String SMTP_USER_PREFERENCE = "SMTP_USER_PREFERENCE";
    String SMTP_PASSWORD_PREFERENCE = "SMTP_PASSWORD_PREFERENCE";

    /**
     * User preference: current perspective
     */
    String CURRENT_PERSPECTIVE_PREFERENCE = "CURRENT_PERSPECTIVE_PREFERENCE";

    /**
     * The notification sending system preference.
     */
    String NOTIFICATION_SENDING_SYSTEM_PREFERENCE = "NOTIFICATION_SENDING_SYSTEM_PREFERENCE";

    // -------------------------------------------------------------------------------------
    // Syntax supported for code edition (plugins configuration blocks)
    // -------------------------------------------------------------------------------------

    /**
     * The various syntax to be used for code edition in various part of the
     * application (plugins, KPIs, etc.).
     * 
     * @author Pierre-Yves Cloux
     */
    enum Syntax {
        XML, JAVASCRIPT, PROPERTIES, VELOCITY
    }

    // -------------------------------------------------------------------------------------
    // Bootstrap classes constants
    // -------------------------------------------------------------------------------------
    String PLUGIN_MENU_PREFIX = "<i class=\"fa fa-cloud-upload\"></i>&nbsp;";

    String BOOTSTRAP_COLUMN_1 = "col-md-1";
    String BOOTSTRAP_COLUMN_2 = "col-md-2";
    String BOOTSTRAP_COLUMN_3 = "col-md-3";
    String BOOTSTRAP_COLUMN_4 = "col-md-4";
    String BOOTSTRAP_COLUMN_5 = "col-md-5";
    String BOOTSTRAP_COLUMN_6 = "col-md-6";
    String BOOTSTRAP_COLUMN_7 = "col-md-7";
    String BOOTSTRAP_COLUMN_8 = "col-md-8";
    String BOOTSTRAP_COLUMN_9 = "col-md-9";
    String BOOTSTRAP_COLUMN_10 = "col-md-10";
    String BOOTSTRAP_COLUMN_11 = "col-md-11";
    String BOOTSTRAP_COLUMN_12 = "col-md-12";

    String BOOTSTRAP_TEXT_ALIGN_LEFT = "text-left";
    String BOOTSTRAP_TEXT_ALIGN_CENTER = "text-center";
    String BOOTSTRAP_TEXT_ALIGN_RIGHT = "text-right";
    String BOOTSTRAP_TEXT_ALIGN_JUSTIFY = "text-justify";

    // File types
    String BOOTSTRAP_FILETYPE_TXT = "<span class=\"text-primary fa fa-file-text-o\" style=\"font-size: 2em;\"></span>";
    String BOOTSTRAP_FILETYPE_PROPERTIES = "<span class=\"text-primary fa fa-file-powerpoint-o\" style=\"font-size: 2em;\"></span>";
    String BOOTSTRAP_FILETYPE_XML = "<span class=\"text-primary fa fa-file-code-o\" style=\"font-size: 2em;\"></span>";
    String BOOTSTRAP_FILETYPE_JAVASCRIPT = "<span class=\"text-primary fa fa-file-code-o\" style=\"font-size: 2em;\"></span>";
    String BOOTSTRAP_FILETYPE_BINARY = "<span class=\"text-primary fa fa-file-o\" style=\"font-size: 2em;\"></span>";

    // Labels
    String LABEL_DEFAULT_FORMAT = "<span class=\"label label-default\">%s</span>";
    String LABEL_PRIMARY_FORMAT = "<span class=\"label label-primary\">%s</span>";
    String LABEL_SUCCESS_FORMAT = "<span class=\"label label-success\">%s</span>";
    String LABEL_INFO_FORMAT = "<span class=\"label label-info\">%s</span>";
    String LABEL_WARNING_FORMAT = "<span class=\"label label-warning\">%s</span>";
    String LABEL_DANGER_FORMAT = "<span class=\"label label-danger\">%s</span>";

    // Common URLs format
    String DISPLAY_URL_FORMAT = "<a data-toggle=\"tooltip\" title=\"" + Msg.get("tooltip.display") + "\" href=\"%s\"><span class=\"fa fa-eye\"></span></a>";
    String EDIT_URL_FORMAT = "<a data-toggle=\"tooltip\" title=\"" + Msg.get("tooltip.edit") + "\" href=\"%s\"><span class=\"fa fa-pencil-square-o\"></span></a>";
    String REQUEST_URL_FORMAT = "<a data-toggle=\"tooltip\" title=\"" + Msg.get("tooltip.request") + "\" href=\"%s\"><span class=\"fa fa-arrow-circle-right\"></span></a>";
    String DELETE_URL_FORMAT = "<a data-toggle=\"tooltip\" title=\"" + Msg.get("tooltip.delete") + "\" href=\"%s\"><span class=\"fa fa-trash\"></span></a>";
    String DOWNLOAD_URL_FORMAT = "<a data-toggle=\"tooltip\" title=\"" + Msg.get("tooltip.download") + "\" href=\"%s\" target=\"_blank\"><span class=\"fa fa-arrow-circle-o-down\"></span></a>";
    String CONFIG_URL_FORMAT = "<a data-toggle=\"tooltip\" title=\"" + Msg.get("tooltip.config") + "\" href=\"%s\"><span class=\"fa fa-cog\"></span></a>";
    String ADD_URL_FORMAT = "<a data-toggle=\"tooltip\" title=\"" + Msg.get("tooltip.add") + "\" href=\"%s\"><span class=\"fa fa-plus\"></span></a>";
    String CANCEL_URL_FORMAT = "<a data-toggle=\"tooltip\" title=\"" + Msg.get("tooltip.cancel") + "\" href=\"%s\"><span class=\"fa fa-times-circle-o\"></span></a>";

    // WARNING this message must be handled by java.text.MessageFormat to
    // specify the error message to be used (default can be
    // Msg.get("default.delete.confirmation.message"))
    String DELETE_URL_FORMAT_WITH_CONFIRMATION = "<a onclick=\"return maf_confirmAction(''{0}'');\" href=\"%s\">"
            + "<span data-toggle=\"tooltip\" title=\"" + Msg.get("tooltip.delete") + "\" class=\"fa fa-trash\"></span></a>";

    // Other common formats
    String TITLE_DESCRIPTION_FORMAT = "<dl><dt>%s</dt><dd>%s</dd></dl>";
}
