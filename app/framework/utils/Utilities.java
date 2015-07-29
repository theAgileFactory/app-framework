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

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import models.framework_models.account.Preference;
import models.framework_models.parent.IModelConstants;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.twirl.api.Html;

import com.fasterxml.jackson.databind.node.ObjectNode;

import framework.commons.IFrameworkConstants;
import framework.services.ServiceManager;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.configuration.ImplementationDefineObjectServiceFactory;

/**
 * An abstract class which gathers useful features
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class Utilities {
    private static Random random = new Random(System.currentTimeMillis());
    public static final int NUMBER_OF_PAGINATION_LINKS = 8;
    public static final int FORMATS_CACHING_DURATION = 300;
    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String CAS_SSO_LANGUAGE_COOKIE = "org.springframework.web.servlet.i18n.CookieLocaleResolver.LOCALE";

    private static Map<String, DateFormat> dateFormatMap = Collections.synchronizedMap(new HashMap<String, DateFormat>());

    private static Logger.ALogger log = Logger.of(Utilities.class);

    /**
     * Return the default date pattern for this application.<br/>
     * If the locale is not null then uses the format associated with this
     * specific locale if it exists
     * 
     * @param locale
     *            a {@link Locale}
     * @return a String pattern
     */
    public static String getDefaultDatePattern(Locale locale) {
        if (locale == null) {
            return getDefaultDatePattern();
        }
        if (Play.application().configuration().keys().contains("maf.default.date.format." + locale.getLanguage())) {
            return Play.application().configuration().getString("maf.default.date.format." + locale.getLanguage());
        }
        return getDefaultDatePattern();
    }

    /**
     * Return the default date pattern for this application.
     * 
     * @return a String pattern
     */
    public static String getDefaultDatePattern() {
        return Play.application().configuration().getString("maf.default.date.format", IModelConstants.DATE_FORMAT);
    }

    /**
     * Return the default date format associated with the specified locale (if
     * any)
     * 
     * @param locale
     *            a {@link Locale}
     * @return a DateFormat instance
     */
    public static DateFormat getDefaultDateFormat(Locale locale) {
        String pattern = getDefaultDatePattern(locale);
        DateFormat dateFormat = dateFormatMap.get(pattern);
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat(pattern);
            dateFormatMap.put(pattern, dateFormat);
        }
        return dateFormat;
    }

    /**
     * Return a date format matching the specified pattern.<br/>
     * If the pattern is null then returns a format matching the current locale.
     * 
     * @param pattern
     *            the pattern for the current locale
     * @param locale
     *            the locale to be used to retrieve or compute the date format
     * @return a date format
     */
    public static DateFormat getDateFormat(String pattern, Locale locale) {
        DateFormat dateFormat = null;
        if (StringUtils.isBlank(pattern)) {
            return getDefaultDateFormat(locale);
        }
        dateFormat = (DateFormat) Cache.get(IFrameworkConstants.FORMATS_CACHE_PREFIX + pattern);
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat(pattern);
            Cache.set(IFrameworkConstants.FORMATS_CACHE_PREFIX + pattern, dateFormat, FORMATS_CACHING_DURATION);
        }
        return dateFormat;
    }

    /**
     * Return a date format matching the specified pattern.<br/>
     * If the pattern is null then returns a format matching the current locale.
     * 
     * @param pattern
     *            the pattern for the current locale
     * @return a date format
     */
    public static DateFormat getDateFormat(String pattern) {
        return getDateFormat(pattern, null);
    }

    /**
     * Return a number format matching the specified pattern.<br/>
     * If the pattern is null then returns a format matching the current locale.
     * 
     * @param pattern
     *            the pattern for the current locale
     * 
     * @return a number format
     */
    public static NumberFormat getNumberFormat(String pattern) {
        return getNumberFormat(pattern, false);
    }

    /**
     * Return a number format matching the specified pattern.<br/>
     * If the pattern is null then returns a format matching the current locale.
     * 
     * @param pattern
     *            the pattern for the current locale
     * @param signed
     *            set to true if the number must be always signed (display a +
     *            for positive numbers)
     * 
     * @return a number format
     */
    public static NumberFormat getNumberFormat(String pattern, Boolean signed) {

        NumberFormat numberFormat;

        if (pattern != null) {

            numberFormat = (NumberFormat) Cache.get(IFrameworkConstants.FORMATS_CACHE_PREFIX + pattern);
            if (numberFormat == null) {
                numberFormat = new DecimalFormat(pattern);
                Cache.set(IFrameworkConstants.FORMATS_CACHE_PREFIX + pattern, numberFormat, FORMATS_CACHING_DURATION);
            }

        } else if (play.mvc.Http.Context.current.get() != null) {

            numberFormat = NumberFormat.getInstance(play.mvc.Http.Context.current().lang().toLocale());

        } else {

            numberFormat = NumberFormat.getInstance(Locale.getDefault());

        }

        if (signed) {
            return new SignedNumberFormat(numberFormat);
        } else {
            return numberFormat;
        }
    }

    /**
     * Return a random unique key (to be used for session id or validation key)
     * 
     * @return a one time key which is fully random
     */
    public static String getRandomID() {
        return UUID.randomUUID().toString() + String.valueOf(random.nextInt(1000000) + 1000);
    }

    /**
     * Return a date which is an offset from the current date
     * 
     * @param offSet
     *            an offset integer value (0 will return the current date)
     * @return a Date
     */
    public static Date getOffSetDate(int offSet) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, offSet);
        return cal.getTime();
    }

    /**
     * Send a success flash message
     * 
     * @param message
     *            a String
     */
    public static void sendSuccessFlashMessage(String message) {
        Controller.flash("success", message);
    }

    /**
     * Send an error flash message
     * 
     * @param message
     *            a String
     */
    public static void sendErrorFlashMessage(String message) {
        Controller.flash("error", message);
    }

    /**
     * Send an info flash message
     * 
     * @param message
     *            a String
     */
    public static void sendInfoFlashMessage(String message) {
        Controller.flash("info", message);
    }

    /**
     * Get the URL to an ajax wait animated gif
     */
    public static String getAjaxWaitImageUrl() {
        IImplementationDefinedObjectService implementationDefinedObjectService = ImplementationDefineObjectServiceFactory.getInstance();
        return implementationDefinedObjectService.getRouteForAjaxWaitImage().url();
    }

    /**
     * Get the URL to download an attachment
     * 
     * @param attachmentId
     *            the attachment id
     */
    public static String getAttachmentDownloadUrl(Long attachmentId) {
        IImplementationDefinedObjectService implementationDefinedObjectService = ImplementationDefineObjectServiceFactory.getInstance();
        return implementationDefinedObjectService.getRouteForDownloadAttachedFile(attachmentId).url();
    }

    /**
     * Get the URL to delete an attachment
     * 
     * @param attachmentId
     *            the attachment id
     */
    public static String getAttachmentDeleteUrl(Long attachmentId) {
        IImplementationDefinedObjectService implementationDefinedObjectService = ImplementationDefineObjectServiceFactory.getInstance();
        return implementationDefinedObjectService.getRouteForDeleteAttachedFile(attachmentId).url();
    }

    /**
     * Get a by array by serializing the specified object as XML
     * 
     * @param object
     *            an object
     */
    public static byte[] marshallObject(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("Null is not serializable");
        }
        if (!Serializable.class.isAssignableFrom(object.getClass())) {
            throw new IllegalArgumentException("The object must be Serializable");
        }
        ByteArrayOutputStream baOut = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(baOut);
        encoder.writeObject(object);
        encoder.flush();
        encoder.close();
        return baOut.toByteArray();
    }

    /**
     * Return an object from the specified array of bytes
     * 
     * @param data
     *            an object
     */
    public static Object unmarshallObject(byte[] data) {
        if (data == null) {
            return null;
        }
        XMLDecoder decoder = null;
        try {
            ByteArrayInputStream baIn = new ByteArrayInputStream(data);
            decoder = new XMLDecoder(baIn);
            return decoder.readObject();
        } finally {
            if (decoder != null) {
                decoder.close();
            }
        }
    }

    /**
     * Select a view using the specified viewPrefix and render it.<br/>
     * The "real view" will be found by appending the current user language to
     * the view prefix.<br/>
     * The signature of this view must match the types of the provided
     * parameters. For instance if the view prefix is "myview" and the current
     * language "english", the real view which is to be looked for is
     * "myview_en".<br/>
     * The specified parameters are passed to the view.
     * 
     * @param viewPrefix
     *            the name of the view to be rendered
     * @param parameters
     *            a variable number of values to be used as place holders
     * @return an {@link Html}
     */
    public static Html renderViewI18n(String viewPrefix, Object... parameters) {
        try {
            Class<?> viewClass = Play.application().classloader().loadClass(String.format("%s_%s", viewPrefix, LanguageUtil.getCurrent().getCode()));
            @SuppressWarnings("rawtypes")
            Class[] signature = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                signature[i] = parameters[i].getClass();
            }
            return (Html) viewClass.getMethod("render", signature).invoke(viewClass, parameters);
        } catch (Exception e) {
            log.error("Error while rendering the view " + viewPrefix, e);
            throw new RuntimeException("Error while rendering the view " + viewPrefix, e);
        }
    }

    /**
     * Convert a {@link ISelectableValueHolderCollection} into JSON.<br/>
     * The structure is the following one : { "value" : {"value" : "the value",
     * "name" : "the name", "description" : "the description","url" : "the url"}
     * }
     * 
     * @param valueHoldersList
     *            a collection
     * @return a JSON object
     */
    public static <T> ObjectNode marshallAsJson(List<ISelectableValueHolder<T>> valueHoldersList) {
        ObjectNode valueHoldersAsJson = Json.newObject();
        if (valueHoldersList != null) {
            int order = 0;
            for (ISelectableValueHolder<T> valueHolder : valueHoldersList) {
                valueHoldersAsJson.put(String.valueOf(valueHolder.getValue()), marshallAsJson(valueHolder, order));
                order++;
            }
        }
        return valueHoldersAsJson;
    }

    /**
     * Convert a {@link ISelectableValueHolderCollection} into JSON.<br/>
     * The structure is the following one : { "value" : {"value" : "the value",
     * "name" : "the name", "description" : "the description","url" : "the url"}
     * }
     * 
     * @param valueHoldersList
     *            a collection
     * @return a JSON object
     */
    public static <T> ObjectNode marshallAsJson(Collection<ISelectableValueHolder<T>> valueHoldersList) {
        return marshallAsJson(new ArrayList<ISelectableValueHolder<T>>(valueHoldersList));
    }

    /**
     * Convert a {@link ISelectableValueHolder} into JSON.<br/>
     * The structure is the following one : {"value" : "the value", "name" :
     * "the name", "description" : "the description","url" : "the url"}
     * 
     * @param valueHolder
     *            a value holder
     * @return a JSON object
     */
    public static <T> ObjectNode marshallAsJson(ISelectableValueHolder<T> valueHolder, int order) {
        ObjectNode valueHolderAsJson = Json.newObject();
        valueHolderAsJson.put("name", valueHolder.getName());
        valueHolderAsJson.put("order", order);
        valueHolderAsJson.put("value", String.valueOf(valueHolder.getValue()));
        if (valueHolder.getDescription() != null) {
            valueHolderAsJson.put("description", valueHolder.getDescription());
        }
        if (valueHolder.getUrl() != null) {
            valueHolderAsJson.put("url", valueHolder.getUrl());
        }
        return valueHolderAsJson;
    }

    /**
     * For a number of objects in the system, a i18n key must be created.<br/>
     * This method create a template to be used to generate any i18n key for
     * each resource (which need so). It creates a String.format template which
     * "expects" one %s (to be replaced by the name of the attribute to be
     * i18nized)
     * 
     * @param clazz
     *            a object class name
     * @param resourceKey
     *            a unique key to be used for all the resources of a named
     *            object
     * @return a string (a valid template)
     */
    public static final String getI18nKeyTemplate(Class<?> clazz, String resourceKey) {
        return clazz.getSimpleName().toLowerCase() + "." + Base64.encodeBase64URLSafeString(resourceKey.getBytes()).toLowerCase() + ".%s";
    }

    /**
     * Generate a String representation of an Exception.<br/>
     * It manages the embedded Exceptions.
     * 
     * @param e
     *            an Exception
     * @return a String
     */
    public static String getExceptionAsString(Exception e) {
        StringBuffer sb = new StringBuffer();
        sb.append(e.getMessage());
        Throwable exp = e.getCause();
        while (exp != null) {
            sb.append(">>");
            sb.append(exp.getMessage());
            sb.append('\n');
            exp = exp.getCause();
        }
        return sb.toString();
    }

    /**
     * Update the SSO language so that it is aligned with the specified
     * language.<br/>
     * The SSO language is the one used for the login & logout page.
     * 
     * @param ctx
     *            a Context (play meaning)
     * @param languageCode
     *            a language code (en, fr, de)
     */
    public static void setSsoLanguage(Context ctx, String languageCode) {
        // For CAS, the language is stored in a cookie
        // To "update" this cookie we have to override it with a different value
        ctx.response().setCookie(CAS_SSO_LANGUAGE_COOKIE, languageCode);
    }

    /**
     * Return the password strength.$ WARNING : see the JavaScript function
     * "maf_password_rating" in the main.js library.
     * 
     * @param password
     * @return
     */
    public static int getPasswordStrength(String password) {
        if (password.length() < IFrameworkConstants.MINIMAL_PASSWORD_LENGTH) {
            return 0;
        }
        int categories = 0;
        if (password.matches(".*[a-z]+.*")) {
            categories++;
        }
        if (password.matches(".*[A-Z]+.*")) {
            categories++;
        }
        if (password.matches(".*[0-9]+.*")) {
            categories++;
        }
        if (password.matches(".*[`~!@#\\$%\\^&\\*\\(\\)_\\-\\+=\\{\\}\\[\\]\\\\\\|:;\"'<>,\\.\\?\\/]+.*")) {
            categories++;
        }
        if (categories < 2) {
            return 0;
        }
        if (categories > 2) {
            return 2;
        }
        return 1;
    }

    /**
     * Block the execution thread during a moment
     * 
     * @param duration
     *            a duration in ms
     */
    public static void wait(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            log.error("Error while stopping the current thread", e);
        }
    }

    /**
     * Read the database to find the system preference which contains the
     * authentication mode.<br/>
     * If not found, use the default one configured in the system application
     * file.
     * 
     * @return
     */
    public static IFrameworkConstants.AuthenticationMode getAuthenticationMode() {
        IFrameworkConstants.AuthenticationMode defaultAuthenticationMode = getDefaultAuthenticationMode();
        try {
            // WARNING : direct access to Preference is NOT RECOMMENDED
            // Here it is required since no access to the ServiceManager is
            // possible since the
            // Authentication mode is a parameter of the ServiceManager init
            // The preference must be a StringCustomAttributeValue
            String authModeAsString = (String) Preference.getPreferenceValueFromUuid(IFrameworkConstants.AUTHENTICATION_MODE_PREFERENCE).getValueAsObject();
            if (StringUtils.isBlank(authModeAsString)) {
                log.warn("No valid authentication mode defined in the database, using default one : " + defaultAuthenticationMode);
                return defaultAuthenticationMode;
            }
            return IFrameworkConstants.AuthenticationMode.valueOf(authModeAsString);
        } catch (Exception e) {
            log.warn("Exception while retrieving the authentication mode " + IFrameworkConstants.AUTHENTICATION_MODE_PREFERENCE
                    + " from the database, using default " + defaultAuthenticationMode, e);
        }
        return defaultAuthenticationMode;
    }

    /**
     * Return the authentication mode which is configured in the system
     * configuration file
     */
    private static IFrameworkConstants.AuthenticationMode getDefaultAuthenticationMode() {
        String authModeAsString = Play.application().configuration().getString("maf.authentication.mode");
        return IFrameworkConstants.AuthenticationMode.valueOf(authModeAsString);
    }

    /**
     * Get a value from a preference if it exists, else from a play
     * configuration.
     * 
     * Note: this method should be called after the preference manager service
     * has been started.
     * 
     * @param preferenceName
     *            the preference name
     * @param configurationName
     *            the play configuration name
     */
    public static String getPreferenceElseConfigurationValue(String preferenceName, String configurationName) {

        String preferenceValue =
                ServiceManager.getService(IPreferenceManagerPlugin.NAME, IPreferenceManagerPlugin.class).getPreferenceValueAsString(preferenceName);

        if (preferenceValue != null && !preferenceValue.equals("")) {
            return preferenceValue;
        } else {
            return Play.application().configuration().getString(configurationName);
        }

    }

    /**
     * Get an Integer value from a preference if it exists, else from a play
     * configuration.
     * 
     * Note: this method should be called after the preference manager service
     * has been started.
     * 
     * @param preferenceName
     *            the preference name
     * @param configurationName
     *            the play configuration name
     */
    public static Integer getPreferenceElseConfigurationValueAsInteger(String preferenceName, String configurationName) {

        Integer preferenceValue =
                ServiceManager.getService(IPreferenceManagerPlugin.NAME, IPreferenceManagerPlugin.class).getPreferenceValueAsInteger(preferenceName);

        if (preferenceValue != null) {
            return preferenceValue;
        } else {
            return Play.application().configuration().getInt(configurationName);
        }

    }

    /**
     * Get an Boolean value from a preference if it exists, else from a play
     * configuration.
     * 
     * Note: this method should be called after the preference manager service
     * has been started.
     * 
     * @param preferenceName
     *            the preference name
     * @param configurationName
     *            the play configuration name
     */
    public static Boolean getPreferenceElseConfigurationValueAsBoolean(String preferenceName, String configurationName) {

        Boolean preferenceValue =
                ServiceManager.getService(IPreferenceManagerPlugin.NAME, IPreferenceManagerPlugin.class).getPreferenceValueAsBoolean(preferenceName);

        if (preferenceValue != null) {
            return preferenceValue;
        } else {
            return Play.application().configuration().getBoolean(configurationName);
        }

    }

    /**
     * Get the size (bytes) of a folder.
     * 
     * @param directory
     *            the root directory
     */
    public static long folderSize(File directory) {
        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile())
                length += file.length();
            else
                length += folderSize(file);
        }
        return length;
    }
}
