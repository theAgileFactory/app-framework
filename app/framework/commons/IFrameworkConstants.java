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


public interface IFrameworkConstants {
    // -------------------------------------------------------------------------------------
    // DataType name constants
    // -------------------------------------------------------------------------------------

    public static final String User = "User";
    public static final String SystemLevelRoleType = "SystemLevelRoleType";

    // -------------------------------------------------------------------------------------
    // Permissions constants
    // -------------------------------------------------------------------------------------
    public static final String DEFAULT_PERMISSION_PRIVATE = "DEFAULT_PERMISSION_PRIVATE";

    // -------------------------------------------------------------------------------------
    // Preference constants
    // -------------------------------------------------------------------------------------
    public static final String DISPLAY_LIST_PAGE_SIZE_PREFERENCE = "DISPLAY_LIST_PAGE_SIZE_PREFERENCE";

    /**
     * Input folder in local storage.
     */
    public static final String INPUT_FOLDER_NAME = "inputs";

    /**
     * Output folder in local storage.
     */
    public static final String OUTPUT_FOLDER_NAME = "outputs";

    /**
     * Default value for null / empty data.
     */
    public static final String DEFAULT_VALUE_EMPTY_DATA = "-";

    /**
     * Plugin small image folder template.
     */
    public static final String PLUGIN_SMALL_IMAGE_TEMPLATE = "public/images/plugins/%s/small.png";

    /**
     * Plugin big image folder template.
     */
    public static final String PLUGIN_BIG_IMAGE_TEMPLATE = "public/images/plugins/%s/big.png";

    /**
     * The minimal password length. WARNING : see the JavaScript function
     * "maf_password_rating" in the main.js library. This value is hardcoded
     * into the library.
     */
    public static final int MINIMAL_PASSWORD_LENGTH = 8;

    // -------------------------------------------------------------------------------------
    // Prefix for the data stored into the Play Cache
    // -------------------------------------------------------------------------------------
    /**
     * Prefix for the data stored in cache.
     */
    public static final String MAF_CACHE_PREFIX = "maf.cache.";

    /**
     * Cache configuration for the {@link AdPanelUtils}.
     */
    public static final String AD_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "ad_panel.";

    /**
     * Prefix for the user authorization token.<br/>
     * This part of the cache may contains some temporary authorization for a
     * user to access a resource
     */
    public static final String AUTHZ_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "authz.token.";

    /**
     * Authorization for "reading" an attachment (downloading it).
     */
    public static final String ATTACHMENT_READ_AUTHZ_CACHE_PREFIX = IFrameworkConstants.AUTHZ_CACHE_PREFIX + "attachment.read.";

    /**
     * Authorization for "writing" an attachment (downloading it).
     */
    public static final String ATTACHMENT_WRITE_AUTHZ_CACHE_PREFIX = IFrameworkConstants.AUTHZ_CACHE_PREFIX + "attachment.write.";

    /**
     * Prefix for the preferences cache.
     */
    public static final String SYSTEM_PREFERENCE_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "systempreference.";

    /**
     * Prefix for the formats cache (Date format, Number format).
     */
    public static final String FORMATS_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "templateUtils.formats.";

    /**
     * Prefix for the cache which stores the user accounts.
     */
    public static final String USER_ACCOUNT_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "useraccount.";

    /**
     * Prefix for the i18n keys.
     */
    public static final String I18N_RESOURCES_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "i18n.";

    /**
     * Prefix for the dynamic permission keys.
     */
    public static final String DYNAMIC_PERMISSION_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "dynamicpermission.";

    /**
     * Prefix for captcha.
     */
    public static final String CAPTCHA_CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "captcha.";

    // -------------------------------------------------------------------------------------
    // The authentication mode for BizDock
    // -------------------------------------------------------------------------------------
    /**
     * The possible authentication mode which can be activated for an
     * application.
     * 
     * @author Pierre-Yves Cloux
     */
    public enum AuthenticationMode {
        STANDALONE, /* The authentication is managed within the play application */
        FEDERATED, /*
                    * The authentication is delegated to a SAMLv2 server and the
                    * credentials provisioning is not managed
                    */
        CAS_SLAVE, /*
                    * The authentication is delegated to CAS but the credentials
                    * provisioning is not managed
                    */
        CAS_MASTER; /*
                     * The authentication is delegated to CAS and the
                     * credentials provisioning is managed
                     */
    }

    /**
     * The possible API authentication and authorizations modes.<br/>
     * 
     * @author Pierre-Yves Cloux
     */
    public enum ApiAuthzMode {
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
     * System preference for storing the authentication mode into the database.<br/>
     * The possible authentication mode are defined by
     * {@link AuthenticationMode}
     */
    public static final String AUTHENTICATION_MODE_PREFERENCE = "AUTHENTICATION_MODE_PREFERENCE";

    /**
     * Preference to select the API authentication and authorization mode.<br/>
     * 
     */
    public static final String API_AUTHZ_MODE_PREFERENCE = "API_AUTHZ_MODE_PREFERENCE";

    /**
     * System preferences to overload base configurations
     */

    public static final String PUBLIC_URL_PREFERENCE = "PUBLIC_URL_PREFERENCE";

    /**
     * System preference for the Swagger API base path
     */

    public static final String SWAGGER_API_BASEPATH_PREFERENCE = "SWAGGER_API_BASEPATH_PREFERENCE";

    /**
     * System preference for SMTP
     */

    public static final String SMTP_HOST_PREFERENCE = "SMTP_HOST_PREFERENCE";
    public static final String SMTP_PORT_PREFERENCE = "SMTP_PORT_PREFERENCE";
    public static final String SMTP_SSL_PREFERENCE = "SMTP_SSL_PREFERENCE";
    public static final String SMTP_TLS_PREFERENCE = "SMTP_TLS_PREFERENCE";
    public static final String SMTP_USER_PREFERENCE = "SMTP_USER_PREFERENCE";
    public static final String SMTP_PASSWORD_PREFERENCE = "SMTP_PASSWORD_PREFERENCE";

    /**
     * User preference: current perspective
     */

    public static final String CURRENT_PERSPECTIVE_PREFERENCE = "CURRENT_PERSPECTIVE_PREFERENCE";

    // -------------------------------------------------------------------------------------
    // Syntax supported for code edition (plugins configuration blocks)
    // -------------------------------------------------------------------------------------

    /**
     * The various syntax to be used for code edition in various part of the
     * application (plugins, KPIs, etc.).
     * 
     * @author Pierre-Yves Cloux
     */
    public enum Syntax {
        XML, JAVASCRIPT, PROPERTIES, VELOCITY;
    }

    // -------------------------------------------------------------------------------------
    // Bootstrap classes constants
    // -------------------------------------------------------------------------------------
    public static final String BOOTSTRAP_COLUMN_1 = "col-md-1";
    public static final String BOOTSTRAP_COLUMN_2 = "col-md-2";
    public static final String BOOTSTRAP_COLUMN_3 = "col-md-3";
    public static final String BOOTSTRAP_COLUMN_4 = "col-md-4";
    public static final String BOOTSTRAP_COLUMN_5 = "col-md-5";
    public static final String BOOTSTRAP_COLUMN_6 = "col-md-6";
    public static final String BOOTSTRAP_COLUMN_7 = "col-md-7";
    public static final String BOOTSTRAP_COLUMN_8 = "col-md-8";
    public static final String BOOTSTRAP_COLUMN_9 = "col-md-9";
    public static final String BOOTSTRAP_COLUMN_10 = "col-md-10";
    public static final String BOOTSTRAP_COLUMN_11 = "col-md-11";
    public static final String BOOTSTRAP_COLUMN_12 = "col-md-12";

    public static final String BOOTSTRAP_TEXT_ALIGN_LEFT = "text-left";
    public static final String BOOTSTRAP_TEXT_ALIGN_CENTER = "text-center";
    public static final String BOOTSTRAP_TEXT_ALIGN_RIGHT = "text-right";
    public static final String BOOTSTRAP_TEXT_ALIGN_JUSTIFY = "text-justify";

    // File types
    public static final String BOOTSTRAP_FILETYPE_TXT = "<span class=\"text-primary filetypes filetypes-txt\" style=\"font-size: 2em;\"></span>";
    public static final String BOOTSTRAP_FILETYPE_PROPERTIES = "<span class=\"text-primary filetypes filetypes-ini\" style=\"font-size: 2em;\"></span>";
    public static final String BOOTSTRAP_FILETYPE_XML = "<span class=\"text-primary filetypes filetypes-xml\" style=\"font-size: 2em;\"></span>";
    public static final String BOOTSTRAP_FILETYPE_JAVASCRIPT = "<span class=\"text-primary filetypes filetypes-js\" style=\"font-size: 2em;\"></span>";
    public static final String BOOTSTRAP_FILETYPE_BINARY = "<span class=\"text-primary filetypes filetypes-bin\" style=\"font-size: 2em;\"></span>";

    // Labels
    public static final String LABEL_DEFAULT_FORMAT = "<span class=\"label label-default\">%s</span>";
    public static final String LABEL_PRIMARY_FORMAT = "<span class=\"label label-primary\">%s</span>";
    public static final String LABEL_SUCCESS_FORMAT = "<span class=\"label label-success\">%s</span>";
    public static final String LABEL_INFO_FORMAT = "<span class=\"label label-info\">%s</span>";
    public static final String LABEL_WARNING_FORMAT = "<span class=\"label label-warning\">%s</span>";
    public static final String LABEL_DANGER_FORMAT = "<span class=\"label label-danger\">%s</span>";

    // Common URLs format
    public static final String DISPLAY_URL_FORMAT = "<a href=\"%s\"><span class=\"glyphicons glyphicons-eye-open\"></span></a>";
    public static final String EDIT_URL_FORMAT = "<a href=\"%s\"><span class=\"glyphicons glyphicons-edit\"></span></a>";
    public static final String REQUEST_URL_FORMAT = "<a href=\"%s\"><span class=\"glyphicons glyphicons-circle-arrow-right\"></span></a>";
    public static final String DELETE_URL_FORMAT = "<a href=\"%s\"><span class=\"glyphicons glyphicons-bin\"></span></a>";
    public static final String DOWNLOAD_URL_FORMAT = "<a href=\"%s\" target=\"_blank\"><span class=\"glyphicons glyphicons-download\"></span></a>";
    public static final String CONFIG_URL_FORMAT = "<a href=\"%s\"><span class=\"glyphicons glyphicons-cogwheel\"></span></a>";
    public static final String ADD_URL_FORMAT = "<a href=\"%s\"><span class=\"glyphicons glyphicons-plus\"></span></a>";

    // WARNING this message must be handled by java.text.MessageFormat to
    // specify the error message to be used (default can be
    // Msg.get("default.delete.confirmation.message"))
    public static final String DELETE_URL_FORMAT_WITH_CONFIRMATION = "<a onclick=\"return maf_confirmAction(''{0}'');\" href=\"%s\">"
            + "<span class=\"glyphicons glyphicons-bin\"></span></a>";

    // Other common formats
    public static final String TITLE_DESCRIPTION_FORMAT = "<dl><dt>%s</dt><dd>%s</dd></dl>";
}
