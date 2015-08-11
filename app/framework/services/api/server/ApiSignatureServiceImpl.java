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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import models.framework_models.api.ApiRegistration;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;

import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import framework.services.api.client.SignatureGeneratorImpl;
import framework.services.api.commons.ApiMethod;
import framework.services.api.commons.ApiSignatureException;
import framework.services.database.IDatabaseDependencyService;

/**
 * A class which holds some methods useful for API signature and authentication
 * management. Here are the supported attributes:
 * <ul>
 * <li>keyLength : the length of the application and secret keys</li>
 * <li>allowedTimeDifference : how much time (in ms) is allowed (plus/minus). If
 * a larger time difference is detected the authentication will fail since the
 * system will assume the request is a replay.</li>
 * <li>hashAlgorithm : the hash algorithm to be used for signature (example:
 * SHA-512)</li>
 * <li>protocolVersion : the version of the signature protocol</li>
 * <li>publicUrl : the URL through which the APIs are exposed</li>
 * </ul>
 * 
 * <p>
 * The data are cached in memory for performance reason. The database must not
 * be modified directly.
 * </p>
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class ApiSignatureServiceImpl implements IApiSignatureService {
    static Logger.ALogger log = Logger.of(ApiSignatureServiceImpl.class);
    static final String AUTHORIZATION_PARSING_REGEXPR = "(" + ApiMethod.DELETE.name() + "|" + ApiMethod.POST.name() + "|" + ApiMethod.PUT.name() + "|"
            + ApiMethod.GET.name() + ")(\\s*)(.*)";

    private int keyLength;
    private long allowedTimeDifference;
    private String hashAlgorithm;
    private int protocolVersion;
    private String publicUrl;
    private Map<String, ApiApplicationConfiguration> applicationConfigRegistry = Collections
            .synchronizedMap(new HashMap<String, ApiApplicationConfiguration>());

    public enum Config {
        KEYS_LENGTH("maf.api.keys.length"), ALLOWED_TIME_DIFF("maf.api.allowed.timediff"), HASH_ALGORITHM("maf.api.hash.algoritm"), PROTOCOL_VERSION(
                "maf.api.protocol.version"), PUBLIC_URL("maf.public.url");
        private String configurationKey;

        private Config(String configurationKey) {
            this.configurationKey = configurationKey;
        }

        public String getConfigurationKey() {
            return configurationKey;
        }
    }

    /**
     * Create a new ApiSignatureServiceImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param databaseDependencyService
     * @throws ApiSignatureException
     */
    @Inject
    public ApiSignatureServiceImpl(ApplicationLifecycle lifecycle, Configuration configuration, IDatabaseDependencyService databaseDependencyService)
            throws ApiSignatureException {
        log.info("SERVICE>>> ApiSignatureServiceImpl starting...");
        this.keyLength = configuration.getInt(Config.KEYS_LENGTH.getConfigurationKey());
        this.allowedTimeDifference = configuration.getInt(Config.ALLOWED_TIME_DIFF.getConfigurationKey());
        this.hashAlgorithm = configuration.getString(Config.HASH_ALGORITHM.getConfigurationKey());
        this.protocolVersion = configuration.getInt(Config.PROTOCOL_VERSION.getConfigurationKey());
        this.publicUrl = configuration.getString(Config.PUBLIC_URL.getConfigurationKey());
        init();
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> ApiSignatureServiceImpl stopping...");
            log.info("SERVICE>>> ApiSignatureServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> ApiSignatureServiceImpl started");
    }

    public void init() throws ApiSignatureException {
        // load registrations
        loadApplicationConfigurations();
    }

    /**
     * Load the configurations
     */
    private void loadApplicationConfigurations() throws ApiSignatureException {
        getApplicationConfigRegistry().clear();
        List<ApiRegistration> apiRegistrations = ApiRegistration.getAllRegistrations();
        for (ApiRegistration apiRegistration : apiRegistrations) {
            SignatureGeneratorImpl signatureGenerator = new SignatureGeneratorImpl(apiRegistration.sharedSecret, apiRegistration.applicationKey);
            signatureGenerator.setHashAlgorithm(getHashAlgorithm());
            signatureGenerator.setProtocolVersion(getProtocolVersion());
            ApiApplicationConfiguration apiAppConfig = new ApiApplicationConfiguration(apiRegistration.name, apiRegistration.description,
                    apiRegistration.testable, signatureGenerator, new String(apiRegistration.apiAuthorization));
            getApplicationConfigRegistry().put(apiRegistration.applicationKey, apiAppConfig);
        }
    }

    @Override
    public IApiApplicationConfiguration setApplicationConfiguration(String applicationName, String description, boolean testable, String apiAuthorization)
            throws ApiSignatureException {
        if (applicationName == null || applicationName.equals(ROOT_APPLICATION)) {
            throw new ApiSignatureException("Invalid application name " + applicationName + " (null or reserved)");
        }

        // Look for an existing application
        ApiApplicationConfiguration apiAppConfig = null;
        ApiRegistration apiRegistration = ApiRegistration.getFromApplicationName(applicationName);
        if (apiRegistration == null) {
            String applicationKey = getRandomApplicationKey();
            SignatureGeneratorImpl signatureGenerator = new SignatureGeneratorImpl(getRandomKey(), applicationKey);
            signatureGenerator.setHashAlgorithm(getHashAlgorithm());
            signatureGenerator.setProtocolVersion(getProtocolVersion());
            apiAppConfig = new ApiApplicationConfiguration(applicationName, description, testable, signatureGenerator, apiAuthorization);

            // Create the API registration into the database
            apiRegistration = new ApiRegistration();
            apiRegistration.name = applicationName;
            apiRegistration.description = description;
            apiRegistration.testable = testable;
            apiRegistration.applicationKey = apiAppConfig.getSignatureGenerator().getApplicationKey();
            apiRegistration.sharedSecret = apiAppConfig.getSignatureGenerator().getSharedSecret();
            apiRegistration.apiAuthorization = apiAuthorization.getBytes();
            apiRegistration.save();

            getApplicationConfigRegistry().put(applicationKey, apiAppConfig);
            ApiLog.log.info("Application " + applicationName + " has been created");
        } else {
            apiAppConfig = getApplicationConfigRegistry().get(apiRegistration.applicationKey);
            apiAppConfig.setApiAuthorizations(apiAuthorization);
            apiAppConfig.setDescription(description);
            apiAppConfig.setTestable(testable);

            // Update the API registration into the database
            apiRegistration.description = description;
            apiRegistration.apiAuthorization = apiAuthorization.getBytes();
            apiRegistration.testable = testable;
            apiRegistration.save();

            getApplicationConfigRegistry().put(apiRegistration.applicationKey, apiAppConfig);
            ApiLog.log.info("Application " + applicationName + " has been updated");
        }

        return apiAppConfig;
    }

    @Override
    public IApiApplicationConfiguration resetApplicationConfigurationKeys(String applicationName) throws ApiSignatureException {
        ApiRegistration apiRegistration = ApiRegistration.getFromApplicationName(applicationName);
        if (apiRegistration == null) {
            throw new ApiSignatureException("Unknown application " + applicationName);
        }

        String previousApplicationKey = apiRegistration.applicationKey;
        ApiApplicationConfiguration apiAppConfig = getApplicationConfigRegistry().get(previousApplicationKey);

        String applicationKey = getRandomApplicationKey();
        SignatureGeneratorImpl signatureGenerator = new SignatureGeneratorImpl(getRandomKey(), applicationKey);
        signatureGenerator.setHashAlgorithm(getHashAlgorithm());
        signatureGenerator.setProtocolVersion(getProtocolVersion());

        apiAppConfig.setSignatureGenerator(signatureGenerator);

        // Update the API registration into the database
        apiRegistration.applicationKey = apiAppConfig.getSignatureGenerator().getApplicationKey();
        apiRegistration.sharedSecret = apiAppConfig.getSignatureGenerator().getSharedSecret();
        apiRegistration.save();

        getApplicationConfigRegistry().remove(previousApplicationKey);
        getApplicationConfigRegistry().put(apiRegistration.applicationKey, apiAppConfig);

        ApiLog.log.info("Keys for application " + applicationName + " have been reseted");

        return apiAppConfig;
    }

    @Override
    public List<IApiApplicationConfiguration> listAuthorizedApplications() throws ApiSignatureException {
        List<IApiApplicationConfiguration> list = new ArrayList<IApiApplicationConfiguration>();
        list.addAll(getApplicationConfigRegistry().values());
        try {
            list.remove(getApplicationConfigurationFromApplicationName(ROOT_APPLICATION));
        } catch (Exception e) {
            log.error("No root application defined, this would prevent any remote administration", e);
        }
        return list;
    }

    @Override
    public List<IApiApplicationConfiguration> listAuthorizedAndTestableApplications() throws ApiSignatureException {
        List<IApiApplicationConfiguration> list = listAuthorizedApplications();
        for (IApiApplicationConfiguration appConfig : list) {
            if (!appConfig.isTestable()) {
                list.remove(appConfig);
            }
        }
        return list;
    }

    @Override
    public void deleteApplicationConfiguration(String applicationName) throws ApiSignatureException {
        if (applicationName.equals(ROOT_APPLICATION)) {
            log.error("Attempt to delete the ROOT_APPLICATION");
            return;
        }
        ApiRegistration apiRegistration = ApiRegistration.getFromApplicationName(applicationName);
        if (apiRegistration == null) {
            throw new ApiSignatureException("Unknown application " + applicationName);
        }
        getApplicationConfigRegistry().remove(apiRegistration.applicationKey);
        apiRegistration.doDelete();
        ApiLog.log.info("Application " + applicationName + " has been deleted");
    }

    @Override
    public IApiApplicationConfiguration getApplicationConfigurationFromApplicationKey(String applicationKey) throws ApiSignatureException {
        if (!getApplicationConfigRegistry().containsKey(applicationKey)) {
            throw new ApiSignatureException("Unknown application for key : " + applicationKey);
        }
        return getApplicationConfigRegistry().get(applicationKey);
    }

    @Override
    public boolean isApplicationNameExists(String applicationName) {
        return getApplicationConfigRegistry().containsKey(applicationName);
    }

    @Override
    public IApiApplicationConfiguration changeApplicationConfigurationName(String oldApplicationName, String newApplicationName) throws ApiSignatureException {
        ApiRegistration apiRegistration = ApiRegistration.getFromApplicationName(oldApplicationName);
        if (apiRegistration == null) {
            throw new ApiSignatureException("Unknown application name : " + oldApplicationName);
        }
        ApiApplicationConfiguration appConfig = getApplicationConfigRegistry().get(apiRegistration.applicationKey);

        apiRegistration.name = newApplicationName;
        apiRegistration.save();

        appConfig.setApplicationName(newApplicationName);

        ApiLog.log.info("Application " + oldApplicationName + " has been renamed to " + newApplicationName);

        return appConfig;
    }

    @Override
    public IApiApplicationConfiguration getApplicationConfigurationFromApplicationName(String applicationName) throws ApiSignatureException {
        ApiRegistration apiRegistration = ApiRegistration.getFromApplicationName(applicationName);
        if (apiRegistration == null) {
            throw new ApiSignatureException("Unknown application name : " + applicationName);
        }
        return getApplicationConfigRegistry().get(apiRegistration.applicationKey);
    }

    @Override
    public void checkApiSignature(String applicationKey, byte[] signature, ApiMethod method, String path, byte[] body, long timeStamp,
            boolean allowTimeDifference) throws ApiSignatureException {
        IApiApplicationConfiguration apiAppConfig = getApplicationConfigRegistry().get(applicationKey);
        if (ApiLog.log.isDebugEnabled()) {
            ApiLog.log.debug("Checking signature for application key [" + applicationKey + "]" + " with method [" + method + "] and path [" + path
                    + "] and timestamp [" + timeStamp + "] " + "and time difference is " + (allowTimeDifference ? "Allowed" : "Not allowed")
                    + (body == null ? "" : ((new String(body)) + "=" + ArrayUtils.toString(body))));
        }
        if (apiAppConfig == null) {
            throw new ApiSignatureException("Unknown application key");
        }
        if (!allowTimeDifference) {
            if (Math.abs(System.currentTimeMillis() - timeStamp) > getAllowedTimeDifference()) {
                throw new ApiSignatureException("Time difference is exceeding the allowance, this may be a replay");
            }
        }
        String url = getPublicUrl() + path;
        apiAppConfig.checkUrl(method, url);
        byte[] comparisonSignature = apiAppConfig.getSignatureGenerator().getRequestSignature(method, url, body, timeStamp);

        if (ApiLog.log.isDebugEnabled()) {
            ApiLog.log.debug("Received signature is : " + new String(signature));
            ApiLog.log.debug("Expected signature is : " + new String(comparisonSignature));
        }

        if (!Arrays.equals(comparisonSignature, signature)) {
            throw new ApiSignatureException("Invalid signature");
        }
    }

    @Override
    public void checkApiAuthorizations(String applicationKey, ApiMethod method, String path) throws ApiSignatureException {
        IApiApplicationConfiguration apiAppConfig = getApplicationConfigRegistry().get(applicationKey);
        if (apiAppConfig == null) {
            throw new ApiSignatureException("Unknown application key");
        }
        String url = getPublicUrl() + path;
        apiAppConfig.checkUrl(method, url);
    }

    /**
     * Generate an application key.<br/>
     * This method checks if the application key is not already existing since
     * this one must be unique.
     * 
     * @return a String
     */
    private String getRandomApplicationKey() {
        String randomKey = null;
        while (getApplicationConfigRegistry().containsKey((randomKey = getRandomKey()))) {
        }
        return randomKey;
    }

    /**
     * Generate a random key.<br/>
     * 
     * @return a String
     */
    private String getRandomKey() {
        return Base64.encodeBase64URLSafeString(RandomStringUtils.random(getKeyLength()).getBytes());
    }

    private int getKeyLength() {
        return keyLength;
    }

    private Map<String, ApiApplicationConfiguration> getApplicationConfigRegistry() {
        return applicationConfigRegistry;
    }

    private long getAllowedTimeDifference() {
        return allowedTimeDifference;
    }

    private String getHashAlgorithm() {
        return hashAlgorithm;
    }

    private int getProtocolVersion() {
        return protocolVersion;
    }

    private String getPublicUrl() {
        return publicUrl;
    }
}
