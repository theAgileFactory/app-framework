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

import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.OrderBy;
import com.avaje.ebean.OrderBy.Property;
import com.fasterxml.jackson.databind.node.ObjectNode;
import framework.commons.IFrameworkConstants;
import models.framework_models.parent.IModelConstants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.i18n.Lang;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Context;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.text.*;
import java.util.*;

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
                String cacheKey = IFrameworkConstants.FORMATS_CACHE_PREFIX + pattern;
                if (play.mvc.Http.Context.current.get() != null) {
                    Lang lang = Context.current().lang();
                    DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance(lang.toLocale());
                    numberFormat = new DecimalFormat(pattern, decimalFormatSymbols);
                    cacheKey += "." + lang.code();
                } else {
                    numberFormat = new DecimalFormat(pattern);
                }
                Cache.set(cacheKey, numberFormat, FORMATS_CACHING_DURATION);
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
     * Send a warning flash message
     * 
     * @param message
     *            a String
     */
    public static void sendWarningFlashMessage(String message) {
        Controller.flash("warning", message);
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
                valueHoldersAsJson.set(String.valueOf(valueHolder.getValue()), marshallAsJson(valueHolder, order));
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

    public static void sortFiles(File[] files) {
        Arrays.sort(files, (f1, f2) -> {
            int compare = Long.compare(f2.lastModified(), f1.lastModified());
            if (compare == 0) {
                compare = f1.getName().compareTo(f2.getName());
            }
            return compare;
        });
    }

    /**
     * Update the specified expression list with the specified order by
     * structure
     * 
     * @param orderBy
     *            an order by structure
     * @param expressionList
     *            an expression list
     */
    public static <T> void updateExpressionListWithOrderBy(OrderBy<T> orderBy, ExpressionList<T> expressionList) {
        OrderBy<T> currentOrderBy = expressionList.orderBy();
        if (orderBy.getProperties() != null) {
            for (Property property : orderBy.getProperties()) {
                currentOrderBy.add(property);
            }
        }
    }

    /**
     * Return a list of array of String from an array of string.
     * 
     * Deadbolt expects a certain structure for the permissions statements.<br/>
     * The basic structure is a List of array of String (AND between the
     * permissions in an array and OR between the arrays in the list). This
     * method takes an array as a parameter and creates a list of array (one
     * array per value of the array passed as a parameter). This creates a
     * permission statement of ORed permissions.
     * 
     * @param values
     *            an array of permissions (to be associated with or)
     * @return
     */
    public static List<String[]> getListOfArray(String... values) {
        ArrayList<String[]> list = new ArrayList<String[]>();
        for (String value : values) {
            list.add(new String[] { value });
        }
        return list;
    }

    /**
     * Provide a String representation of the provided list of array of String
     * 
     * @return
     */
    public static String toString(List<String[]> values) {
        StringBuffer sb = new StringBuffer();
        if (values != null) {
            sb.append('[');
            for (String[] array : values) {
                sb.append(ArrayUtils.toString(array));
                sb.append(',');
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(']');
        } else {
            return null;
        }
        return sb.toString();
    }

    /**
     * Get the current object id from the context.<br/>
     * This feature is mainly used for authorization purpose to get the
     * "current" manipulated object from the context.
     * 
     * @param context
     *            the context
     */
    public static Long getId(Http.Context context) {
        Long id = null;

        if (context.args.containsKey(IFrameworkConstants.ID_NAME_FOR_CONTEXT)) {
            // get the id as a context argument

            id = (Long) context.args.get(IFrameworkConstants.ID_NAME_FOR_CONTEXT);
        } else if (context.request().getQueryString("id") != null) {
            // get the id as a query parameter

            id = Long.valueOf(context.request().getQueryString("id"));
        } else if (context.request().headers().get("id") != null) {
            // get the id as a header parameter

            id = Long.valueOf(context.request().headers().get("id")[0]);
        } else if (context.request().body().asFormUrlEncoded() != null && context.request().body().asFormUrlEncoded().get("id") != null) {
            // get the id as a form content parameter

            id = Long.valueOf(context.request().body().asFormUrlEncoded().get("id")[0]);
        } else if (context.request().body().asMultipartFormData() != null && context.request().body().asMultipartFormData().asFormUrlEncoded() != null
                && context.request().body().asMultipartFormData().asFormUrlEncoded().get("id") != null) {
            // get the id as a multipart form content parameter

            id = Long.valueOf(context.request().body().asMultipartFormData().asFormUrlEncoded().get("id")[0]);
        } else {
            // else try to get the id as a route parameter (only at the end of
            // the path), example: https://localhost/portfolio-entry/view/10

            try {
                id = Long.parseLong(context.request().path().substring(context.request().path().lastIndexOf('/') + 1));
            } catch (Exception e) {
                Logger.debug("impossible to find the id as a route parameter");
            }
        }

        return id;
    }

    /**
     * Get the number of working days between two dates.
     *
     * @param start the start date
     * @param end the end date
     * @return the number of working days
     */
    public static int getWorkingDaysCount(Date start, Date end) {

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(start);

        // Calendar'w week starts on sunday but we want it to start on monday.
        int d = startCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (d == 0) {
            d = 7;
        }

        // n is the number of days between the 2 dates
        int n = getDuration(start, end);

        // q is the total of complete weeks between the 2 dates
        int q = n / 7;
        // r is the number of days in addition to the complete weeks
        int r = n % 7;

        // a is the end date day of the week
        int a = d + r - 1;

        // w is the number of weekend days, at first the number of complete weeks times 2 weekend days.
        int w = q * 2;

        // If the start day is a sunday, we must remove one weekend day
        if (d == 7) {
            w--;
        }
        // if the last day is saturday, we add 1 weekend day
        if (a == 6) {
            w++;
        }
        // if the last day is over sunday, we add 2 weekend days
        if (a >= 7) {
            w += 2;
        }

        // The result is the total number of days minus the weekend days
        return n - w;
    }

    /**
     * Get the duration between 2 dates.
     * If one is null, returns -1. If the start date is after the end date, dates are switched.
     * @param startDate the start date
     * @param endDate the end date
     * @return the duration between the 2 dates
     */
    public static int getDuration(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return -1;
        }
        if (startDate.after(endDate)) {
            Date tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }
        return (int) ((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24)) + 1;
    }

    /**
     * Check if two dates are equals (also true if both are null)
     *
     * @param a the first date
     * @param b the second date
     *
     * @return true if both dates are null or equal, false instead
     */
    public static boolean datesAreEqualsOrNull(Date a, Date b) {
        return
            a == null && b == null ||
            a != null && b != null &&
            a.compareTo(b) == 0;

    }
}
