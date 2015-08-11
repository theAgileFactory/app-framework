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
package framework.services.api.server;

import java.net.HttpURLConnection;
import java.util.Arrays;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Http.RawBuffer;
import play.mvc.Result;
import framework.commons.IFrameworkConstants;
import framework.commons.IFrameworkConstants.ApiAuthzMode;
import framework.security.SecurityUtils;
import framework.services.account.AccountManagementException;
import framework.services.account.IAccountManagerPlugin;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.account.IUserAccount;
import framework.services.api.AbstractApiController;
import framework.services.api.ApiError;
import framework.services.api.commons.ApiMethod;
import framework.services.api.commons.IApiConstants;
import framework.services.session.IUserSessionManagerPlugin;

/**
 * The action associated with the annotation {@link ApiAuthentication}.<br/>
 * This action performs some authentication checks for API calls.<br/>
 * There are various authentication types which could be applied:
 * <ul>
 * <li>If no authentication headers are provided, the system will assume that
 * the the authentication must be based on the currently logged user.</li>
 * <li>If all the authentication headers are provided, the system will perform a
 * full signature check.</li>
 * <li>If only the "application key" authentication header is provided, then the
 * system will check if the system preference API_AUTHZ_MODE_PREFERENCE is set
 * to APPLICATION_KEY_ONLY.
 * <ul>
 * <li>If it is : then the authentication will be based on the "application key"
 * </li>
 * <li>If it is not : then the system will assume that the system is in
 * simulation mode. In such case it will check if a user is connected and the
 * application is testable.</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * <p>
 * By default a property must be set in the application configuration
 * "maf.api.default.permission".<br/>
 * This one specifies the default permission to be requested for any API when no
 * authentication headers are provided or the system is in simulation mode.
 * </p>
 * <p>
 * <u>NB</u>: The each API can be configured with additional permissions using
 * the {@link ApiAuthentication} annotation.
 * </p>
 * 
 * @author Pierre-Yves Cloux
 */
public class ApiAuthenticationAction extends Action<ApiAuthentication> {
    @Inject
    private IPreferenceManagerPlugin preferenceManagerPlugin;
    @Inject
    private IUserSessionManagerPlugin userSessionManagerPlugin;
    @Inject
    private IAccountManagerPlugin accountManagerPlugin;
    @Inject
    private IApiSignatureService apiSignatureService;

    private String defaultPermission;

    public ApiAuthenticationAction() {
        this.defaultPermission = play.Configuration.root().getString("maf.api.default.permission");
    }

    @Override
    public Promise<Result> call(Context context) throws Throwable {

        if (!this.configuration.additionalCheck().isInterface()) {
            Pair<Boolean, String> additionalCheck = this.configuration.additionalCheck().newInstance().before();
            if (!additionalCheck.getLeft()) {
                return returnUnauthorized(additionalCheck.getRight());
            }
        }

        if (ApiLog.log.isDebugEnabled()) {
            ApiLog.log.debug("API call " + context.request().uri());
        }
        String signatureHeader = context.request().getHeader(IApiConstants.SIGNATURE_HEADER);
        String timeStampHeader = context.request().getHeader(IApiConstants.TIMESTAMP_HEADER);
        String applicationKeyHeader = context.request().getHeader(IApiConstants.APPLICATION_KEY_HEADER);

        if (ApiLog.log.isDebugEnabled()) {
            ApiLog.log.debug("Signature header : " + signatureHeader);
            ApiLog.log.debug("Timestamp header : " + timeStampHeader);
            ApiLog.log.debug("Application header : " + applicationKeyHeader);
        }

        // All the authentication headers are provided : signature mode is
        // assumed (even if the APP_KEY mode is activated)
        if (isAllAuthenticationHeaderPresent(signatureHeader, timeStampHeader, applicationKeyHeader)) {

            if (ApiLog.log.isDebugEnabled()) {
                ApiLog.log.debug("All headers found in the request, API authentication mode applied is : SIGNATURE");
            }

            // Authentication by signature
            Pair<Boolean, String> result = authenticationBySignature(context, signatureHeader, timeStampHeader, applicationKeyHeader);
            if (!result.getLeft()) {
                return returnUnauthorized(result.getRight());
            }

        }

        // Only the application key is provided, check the system preference
        if (isOnlyApplicationKeyHeaderPresent(signatureHeader, applicationKeyHeader)) {
            String authzMode = getPreferenceManagerPlugin().getPreferenceValueAsString(IFrameworkConstants.API_AUTHZ_MODE_PREFERENCE);
            // Authentication mode configured at system level is application key
            // only
            if (authzMode != null && authzMode.equals(ApiAuthzMode.APPLICATION_KEY_ONLY.name())) {
                if (ApiLog.log.isDebugEnabled()) {
                    ApiLog.log.debug("Only Application Key header found, API authentication mode applied is APP_KEY (system preference is " + authzMode + ")");
                }
                // AppKey authentication mode is activated test by application
                // key
                Pair<Boolean, String> result = authenticationByKey(context, applicationKeyHeader);
                if (!result.getLeft()) {
                    ApiLog.log.debug(result.getRight());
                    return returnUnauthorized(result.getRight());
                }
            }
            // Authentication mode configured at system level is SIGNATURE
            if (authzMode == null || !authzMode.equals(ApiAuthzMode.APPLICATION_KEY_ONLY.name())) {
                if (ApiLog.log.isDebugEnabled()) {
                    ApiLog.log.debug("Only Application Key header found, API authentication mode is simulation (access"
                            + " from the API browser with a testable application selected) because system preference is " + authzMode);
                }
                // The AppKey mode is NOT activated, this means that the system
                // is certainly in simulation mode
                // The application key authentication is used but only if the
                // API is testable
                if (!isTestable(applicationKeyHeader)) {
                    if (ApiLog.log.isDebugEnabled()) {
                        ApiLog.log.debug("Application is not testable, can't accept an application authentication");
                    }
                    return returnUnauthorized("Not a testable application");
                }
                Pair<Boolean, String> result = authenticateByAuthorizations(context, false);
                if (!result.getLeft()) {
                    ApiLog.log.debug("Application is testable but [" + result.getRight() + "] (please remember that test mode is"
                            + " ONLY allowed from the API browser)");
                    return returnUnauthorized(result.getRight());
                }
                result = authenticationByKey(context, applicationKeyHeader);
                if (!result.getLeft()) {
                    ApiLog.log.debug("Application is testable but [" + result.getRight() + "] (please remember that test mode is"
                            + " ONLY allowed from the API browser)");
                    return returnUnauthorized(result.getRight());
                }
            }

        }

        // Authentication by permission since no authentication header is found
        if (isNoAuthenticationHeaderPresent(signatureHeader, applicationKeyHeader)) {

            if (ApiLog.log.isDebugEnabled()) {
                ApiLog.log.debug("No authentication headers found, API authentication is authorization (access from a user session)");
            }

            // Test if the user has the default permission but also the API
            // required permissions
            Pair<Boolean, String> result = authenticateByAuthorizations(context, true);
            if (!result.getLeft()) {
                ApiLog.log.debug(result.getRight());
                return returnUnauthorized(result.getRight());
            }
        }

        // if the action is for the root key only
        if (this.configuration.onlyRootKey()) {

            // if the authentication headers are given
            if (isAllAuthenticationHeaderPresent(signatureHeader, timeStampHeader, applicationKeyHeader)
                    || isOnlyApplicationKeyHeaderPresent(signatureHeader, applicationKeyHeader)) {

                IApiApplicationConfiguration sourceKey = getApiSignatureService().getApplicationConfigurationFromApplicationKey(applicationKeyHeader);

                if (!sourceKey.getApplicationName().equals(IApiSignatureService.ROOT_APPLICATION)) {
                    return returnUnauthorized("the root key should be used for a root action");
                }

            } else {
                return returnUnauthorized("the authentication headers should be given for a root action");
            }
        }

        if (!this.configuration.additionalCheck().isInterface()) {
            Pair<Boolean, String> additionalCheck = this.configuration.additionalCheck().newInstance().after();
            if (!additionalCheck.getLeft()) {
                return returnUnauthorized(additionalCheck.getRight());
            }
        }

        return delegate.call(context);
    }

    /**
     * Return true if the only authentication header provided is the application
     * key
     * 
     * @param signatureHeader
     * @param applicationKeyHeader
     * @return
     */
    private boolean isOnlyApplicationKeyHeaderPresent(String signatureHeader, String applicationKeyHeader) {
        return signatureHeader == null && applicationKeyHeader != null;
    }

    /**
     * Return true if no authentication header is provided
     * 
     * @param signatureHeader
     * @param applicationKeyHeader
     * @return
     */
    private boolean isNoAuthenticationHeaderPresent(String signatureHeader, String applicationKeyHeader) {
        return signatureHeader == null && applicationKeyHeader == null;
    }

    /**
     * Return true if All the authentication headers are provided
     * 
     * @param signatureHeader
     * @param timeStampHeader
     * @param applicationKeyHeader
     * @return
     */
    private boolean isAllAuthenticationHeaderPresent(String signatureHeader, String timeStampHeader, String applicationKeyHeader) {
        return signatureHeader != null && timeStampHeader != null && applicationKeyHeader != null;
    }

    /**
     * Authenticate the API using the specified keys.<br/>
     * If the returned value is false then the API was not authenticated and a
     * badRequest must be returned
     * 
     * @param context
     * @param signatureHeader
     * @param timeStampHeader
     * @param applicationKeyHeader
     * @return true if everything went well
     */
    private Pair<Boolean, String> authenticationBySignature(Context context, String signatureHeader, String timeStampHeader, String applicationKeyHeader) {
        long timeStampAsLong;
        try {
            timeStampAsLong = Long.valueOf(timeStampHeader);
        } catch (NumberFormatException e) {
            String message = "Invalid timestamp header";
            ApiLog.log.error(message, e);
            return Pair.of(false, message);
        }
        try {
            ApiMethod method = ApiMethod.valueOf(context.request().method().toUpperCase());
            byte[] body = null;
            if (method.equals(ApiMethod.POST) || method.equals(ApiMethod.PUT)) {
                RawBuffer rawBuffer = context.request().body().asRaw();
                if (rawBuffer == null) {
                    String message = "The request body is empty while the HTTP method is " + method.name();
                    ApiLog.log.error(message);
                    return Pair.of(false, message);
                }
                body = rawBuffer.asBytes();
            }
            getApiSignatureService().checkApiSignature(applicationKeyHeader, signatureHeader.getBytes(), method, context.request().uri(), body,
                    timeStampAsLong, configuration.allowTimeDifference());
        } catch (Exception e) {
            String message = "Unauthorized API call : invalid signature or not enough rights";
            ApiLog.log.error(message, e);
            return Pair.of(false, message);
        }
        logCall(context.request().method().toUpperCase(), context.request().uri(), applicationKeyHeader);
        return Pair.of(true, null);
    }

    /**
     * Authenticate the current API using the currently logged user (as well as
     * its authorizations)
     * 
     * @param context
     * @param addApiRequiredAuthorizations
     *            if true the authorizations associated to the API (using the
     *            {@link ApiAuthentication} annotations are required (and
     *            tested)
     * @return true if everything went well otherwise a badRequest must be
     *         issued
     * @throws AccountManagementException
     */
    private Pair<Boolean, String> authenticateByAuthorizations(Context context, boolean addApiRequiredAuthorizations) throws AccountManagementException {
        String userSessionId = getUserSessionManagerPlugin().getUserSessionId(context);
        if (userSessionId == null) {
            return Pair.of(false, "No valid user session");
        }
        IUserAccount userAccount = getAccountManagerPlugin().getUserAccountFromUid(userSessionId);

        // If the default permission is null this is not normal
        if (StringUtils.isBlank(getDefaultPermission())) {
            String message = "Unauthorized API call : no default permission set, please contact your administrator";
            ApiLog.log.error(message);
            return Pair.of(false, message);
        }

        String[] roles = new String[] { getDefaultPermission() };
        if (addApiRequiredAuthorizations && configuration.permissions() != null && configuration.permissions().length != 0) {
            roles = ArrayUtils.add(configuration.permissions(), getDefaultPermission());
            if (ApiLog.log.isDebugEnabled()) {
                ApiLog.log.debug("Adding permissions to default : " + Arrays.toString(roles));
            }
        }
        if (userAccount == null || !(SecurityUtils.hasAllRoles(userAccount, roles))) {
            ApiLog.log.error("Unauthorized API call : permissions of user " + userAccount.getUid() + " are not sufficient expecting " + Arrays.toString(roles));
            return Pair.of(false, "Unauthorized API call for API browser access : insufficient permissions for the currently logged user");
        }
        logCall(context.request().method().toUpperCase(), context.request().uri(), userAccount.getUid());
        return Pair.of(true, null);
    }

    /**
     * Authenticate the API using the specified application key only.<br/>
     * If the returned value is false then the API was not authenticated and a
     * badRequest must be returned
     * 
     * @param context
     * @param applicationKeyHeader
     * @return true if everything went well
     */
    private Pair<Boolean, String> authenticationByKey(Context context, String applicationKeyHeader) {
        try {
            ApiMethod method = ApiMethod.valueOf(context.request().method().toUpperCase());
            getApiSignatureService().checkApiAuthorizations(applicationKeyHeader, method, context.request().path());
        } catch (Exception e) {
            String message = "Unauthorized API call : invalid signature or not enough rights";
            ApiLog.log.error(message, e);
            return Pair.of(false, message);
        }
        logCall(context.request().method().toUpperCase(), context.request().uri(), applicationKeyHeader);
        return Pair.of(true, null);
    }

    /**
     * Authenticate the API using the specified application key only.<br/>
     * If the returned value is false then the API was not authenticated and a
     * badRequest must be returned
     * 
     * @param context
     * @param applicationKeyHeader
     * @return true if everything went well
     */
    private boolean isTestable(String applicationKeyHeader) {
        try {
            return getApiSignatureService().getApplicationConfigurationFromApplicationKey(applicationKeyHeader).isTestable();
        } catch (Exception e) {
            ApiLog.log.error("Unauthorized API call", e);
            return false;
        }
    }

    /**
     * Log an API call
     * 
     * @param method
     *            the method of the URL call
     * @param request
     *            the called URI
     * @param application
     *            the application which is calling (or the user name)
     */
    private void logCall(String method, String request, String application) {
        ApiLog.log.info(String.format("API call for [%s %s] from [%s]", method, request, application));
    }

    private Promise<Result> returnUnauthorized(final String message) {
        return Promise.promise(new Function0<Result>() {
            @Override
            public Result apply() throws Throwable {
                return AbstractApiController.getJsonErrorResponse(new ApiError(HttpURLConnection.HTTP_UNAUTHORIZED, message));
            }
        });
    }

    private String getDefaultPermission() {
        return defaultPermission;
    }

    private IPreferenceManagerPlugin getPreferenceManagerPlugin() {
        return preferenceManagerPlugin;
    }

    private IUserSessionManagerPlugin getUserSessionManagerPlugin() {
        return userSessionManagerPlugin;
    }

    private IAccountManagerPlugin getAccountManagerPlugin() {
        return accountManagerPlugin;
    }

    private IApiSignatureService getApiSignatureService() {
        return apiSignatureService;
    }
}
