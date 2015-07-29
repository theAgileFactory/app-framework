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

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import framework.services.api.client.ISignatureGenerator;
import framework.services.api.commons.ApiMethod;
import framework.services.api.commons.ApiSignatureException;

/**
 * The configuration of an application.<br/>
 * 
 * @author Pierre-Yves Cloux
 */
public class ApiApplicationConfiguration implements IApiApplicationConfiguration {
    private static Pattern pattern = Pattern.compile(ApiSignatureServiceImpl.AUTHORIZATION_PARSING_REGEXPR);

    private String applicationName;
    private String description;
    private boolean testable;
    private String apiAuthorizationsAsString;
    private ISignatureGenerator signatureGenerator;
    private List<Pair<ApiMethod, Pattern>> allowedApiPatterns;

    public ApiApplicationConfiguration(String applicationName, String description, boolean testable, ISignatureGenerator signatureGenerator,
            String apiAuthorizationsAsString) throws ApiSignatureException {
        super();
        this.applicationName = applicationName;
        this.description = description;
        this.testable = testable;
        this.signatureGenerator = signatureGenerator;
        this.apiAuthorizationsAsString = apiAuthorizationsAsString;
        fillAllowedApiPatterns(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * framework.services.api.server.IApiApplicationConfiguration#checkUrl(framework
     * .services.api.commons.ApiMethod, java.lang.String)
     */
    @Override
    public void checkUrl(ApiMethod method, String url) throws ApiSignatureException {
        try {
            URL urlStructure = new URL(url);
            String path = urlStructure.getPath();
            for (Pair<ApiMethod, Pattern> allowedApiPattern : allowedApiPatterns) {
                if (allowedApiPattern.getLeft().equals(method)) {
                    Matcher matcher = allowedApiPattern.getRight().matcher(path);
                    if (matcher.matches()) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            throw new ApiSignatureException("Unauthorized URL", e);
        }
        throw new ApiSignatureException("Unauthorized URL:" + url + ", no matching authorization found");
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.services.api.server.IApiApplicationConfiguration#
     * getSignatureGenerator()
     */
    @Override
    public ISignatureGenerator getSignatureGenerator() {
        return signatureGenerator;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * framework.services.api.server.IApiApplicationConfiguration#getApplicationName
     * ()
     */
    @Override
    public String getApplicationName() {
        return applicationName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * framework.services.api.server.IApiApplicationConfiguration#getDescription
     * ()
     */
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isTestable() {
        return testable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.services.api.server.IApiApplicationConfiguration#
     * getApiAuthorizationsAsString()
     */
    @Override
    public String getApiAuthorizationsAsString() {
        return apiAuthorizationsAsString;
    }

    void setApiAuthorizations(String apiAuthorizationsAsString) throws ApiSignatureException {
        this.apiAuthorizationsAsString = apiAuthorizationsAsString;
        fillAllowedApiPatterns(true);
    }

    /**
     * Set the api authorizations by parsing the String representation of the
     * authorizations
     * 
     * @param throwException
     *            true if an exception must be thrown in case of error
     * @throws ApiSignatureException
     */
    private void fillAllowedApiPatterns(boolean throwException) throws ApiSignatureException {
        allowedApiPatterns = new ArrayList<Pair<ApiMethod, Pattern>>();
        for (Pair<ApiMethod, String> apiAuthorization : parseAuthorization(getApplicationName(), getApiAuthorizationsAsString(), pattern, throwException)) {
            Pattern pattern = Pattern.compile(apiAuthorization.getRight());
            allowedApiPatterns.add(Pair.of(apiAuthorization.getLeft(), pattern));
        }
    }

    /**
     * Parse the authorization structure using the provided {@link Pattern}
     * 
     * @param applicationName
     *            the name of the application
     * @param apiAuthorization
     *            the API authorization statements
     * @param pattern
     *            a parsing regexpr pattern
     * @param throwException
     *            true if an exception must be thrown in case of error
     * @return
     */
    private List<Pair<ApiMethod, String>> parseAuthorization(String applicationName, String apiAuthorization, Pattern pattern, boolean throwException)
            throws ApiSignatureException {
        List<Pair<ApiMethod, String>> parsedApiAuthorization = new ArrayList<Pair<ApiMethod, String>>();
        try {
            BufferedReader reader = new BufferedReader(new StringReader(apiAuthorization));
            String lineRead = null;
            while ((lineRead = reader.readLine()) != null) {
                if (!lineRead.startsWith(ApiSignatureServiceImpl.IGNORE_LINE_CHARACTER)) {
                    Matcher m = pattern.matcher(lineRead);
                    if (m.matches()) {
                        String method = m.group(1);
                        String apiAuthorizationRegExpr = m.group(3);
                        parsedApiAuthorization.add(Pair.of(ApiMethod.valueOf(method), apiAuthorizationRegExpr));
                    } else {
                        String message = "Error while parsing the authorization statement " + lineRead + " from the API registration " + applicationName;
                        ApiSignatureServiceImpl.log.info(message);
                        if (throwException) {
                            throw new ApiSignatureException(message);
                        }
                    }
                }
            }
        } catch (Exception e) {
            ApiSignatureServiceImpl.log.error("Error while parsing the authorization for the API registration " + applicationName, e);
            if (throwException) {
                if (e instanceof ApiSignatureException)
                    throw (ApiSignatureException) e;
                throw new ApiSignatureException("Error while parsing the authorization for the API registration " + applicationName, e);
            }
        }
        return parsedApiAuthorization;
    }

    void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    void setDescription(String description) {
        this.description = description;
    }

    void setSignatureGenerator(ISignatureGenerator signatureGenerator) {
        this.signatureGenerator = signatureGenerator;
    }

    void setTestable(boolean testable) {
        this.testable = testable;
    }
}