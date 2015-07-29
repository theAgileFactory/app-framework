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
package framework.services.account;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import play.Logger;

/**
 * The default implementation for the class which performs the queries on the
 * authentication back-end.<br/>
 * The present implementation is using LDAP.
 * 
 * @author Pierre-Yves Cloux
 * 
 */
public class DefaultAuthenticationAccountReaderPlugin implements IAuthenticationAccountReaderPlugin {
    private static Logger.ALogger log = Logger.of(DefaultAuthenticationAccountReaderPlugin.class);
    private String userSearchBase;
    private Hashtable<String, String> environment;
    private String userUniqueIdAttribute;
    private String userSearchFilter;
    private String userEmailSearchFilter;
    private String userFullNameSearchFilter;
    private String activationLdapAttr;
    private String activationLdapActiveValue;

    public DefaultAuthenticationAccountReaderPlugin(String ldapUrl, String user, String password, String userSearchBase, String userSearchFilter,
            String userEmailSearchFilter, String userFullNameSearchFilter, String userUniqueIdAttribute, String activationLdapAttr,
            String activationLdapActiveValue) {
        environment = new Hashtable<String, String>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, ldapUrl);
        environment.put(Context.SECURITY_PRINCIPAL, user);
        environment.put(Context.SECURITY_CREDENTIALS, password);
        environment.put("com.sun.jndi.ldap.connect.pool", "true");
        this.userSearchBase = userSearchBase;
        this.userSearchFilter = userSearchFilter;
        this.userUniqueIdAttribute = userUniqueIdAttribute;
        this.userEmailSearchFilter = userEmailSearchFilter;
        this.userFullNameSearchFilter = userFullNameSearchFilter;

        this.activationLdapAttr = activationLdapAttr;
        this.activationLdapActiveValue = activationLdapActiveValue;

        if (log.isDebugEnabled()) {
            log.debug("Initialized with " + environment.toString());
        }

    }

    @Override
    public IUserAuthenticationAccount getAccountFromUid(String uid) throws AccountManagementException {
        try {
            List<IUserAuthenticationAccount> userAccounts = getAccountsFromLdapFilter(String.format(getUserSearchFilter(), uid.toLowerCase()));
            if (userAccounts.size() > 1)
                log.warn(String.format("Multiple uid %s in the authentication backend", uid));
            if (userAccounts.size() > 0)
                return userAccounts.get(0);
            return null;
        } catch (NamingException e) {
            throw new AccountManagementException(e);
        }
    }

    @Override
    public IUserAuthenticationAccount getAccountFromEmail(String mail) throws AccountManagementException {
        try {
            List<IUserAuthenticationAccount> userAccounts = getAccountsFromLdapFilter(String.format(getUserEmailSearchFilter(), mail.toLowerCase()));
            if (userAccounts.size() > 1)
                log.warn(String.format("Multiple mail %s in the authentication backend", mail));
            if (userAccounts.size() > 0)
                return userAccounts.get(0);
            return null;
        } catch (NamingException e) {
            throw new AccountManagementException(e);
        }
    }

    @Override
    public List<IUserAuthenticationAccount> getAccountsFromName(String nameCriteria) throws AccountManagementException {
        try {
            return getAccountsFromLdapFilter(String.format(getUserFullNameSearchFilter(), nameCriteria));
        } catch (NamingException e) {
            throw new AccountManagementException(e);
        }
    }

    @Override
    public boolean isMailAlreadyExist(String mail) throws AccountManagementException {
        try {
            String mailFilter = String.format(getUserEmailSearchFilter(), mail);
            return isAccountExists(mailFilter);
        } catch (NamingException e) {
            throw new AccountManagementException(e);
        }
    }

    @Override
    public boolean isUidAlreadyExist(String uid) throws AccountManagementException {
        try {
            String userFilter = String.format(getUserSearchFilter(), uid);
            return isAccountExists(userFilter);
        } catch (NamingException e) {
            throw new AccountManagementException(e);
        }
    }

    @Override
    public boolean checkPassword(String uid, String password) throws AccountManagementException {
        try {
            return bindToLdap(uid, password);
        } catch (NamingException e) {
            log.info(String.format("Attenpt to authenticate for user %s failed", uid), e);
        }
        return false;
    }

    /**
     * Attempt to authenticate to LDAP with the specified password
     * 
     * @param uid
     *            a user id
     * @param password
     *            a user password
     * @throws NamingException
     */
    private boolean bindToLdap(String uid, String password) throws NamingException {
        DirContext ctx = null;
        try {
            Hashtable<String, String> copyEnv = new Hashtable<String, String>(getEnvironment());
            copyEnv.put(Context.SECURITY_PRINCIPAL, getDnForId(uid));
            copyEnv.put(Context.SECURITY_CREDENTIALS, password);
            ctx = new InitialDirContext(copyEnv);
            return true;
        } catch (NamingException e) {
            throw e;
        } finally {
            try {
                ctx.close();
            } catch (Exception e2) {
            }
        }
    }

    /**
     * Look for a user associated with the specified search filter
     * 
     * @param filter
     *            a LDAP filter
     * @param ctx
     * @return some search results
     * @throws NamingException
     */
    private NamingEnumeration<SearchResult> searchLdapUserUsingFilter(String filter, DirContext ctx) throws NamingException {
        SearchControls sc = new SearchControls();
        String[] userAttrIDs = null;
        if (getActivationLdapAttr() != null) {
            userAttrIDs = new String[] { "givenname", "sn", "mail", "uid", "cn", getActivationLdapAttr() };
        } else {
            userAttrIDs = new String[] { "givenname", "sn", "mail", "uid", "cn" };
        }
        sc.setReturningAttributes(userAttrIDs);
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> searchResults = ctx.search(getUserSearchBase(), filter, sc);
        return searchResults;
    }

    /**
     * Look for a user associated with the specified uid and return its dn
     * 
     * @param uid
     *            an e-mail address
     * @return a DN
     * @throws NamingException
     */
    private String getDnForId(String uid) throws NamingException {
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(getEnvironment());
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String userFilter = String.format(getUserSearchFilter(), uid);
            NamingEnumeration<SearchResult> searchResults = ctx.search(getUserSearchBase(), userFilter, sc);
            if (log.isDebugEnabled()) {
                log.debug("Search LDAP for " + userFilter);
            }
            if (searchResults.hasMoreElements()) {
                SearchResult result = searchResults.next();
                if (result != null) {
                    return result.getNameInNamespace();
                }
            }
            return null;
        } catch (NamingException e) {
            throw e;
        } finally {
            try {
                ctx.close();
            } catch (Exception e2) {
            }
        }
    }

    /**
     * Look for a user associated with the specified mail address
     * 
     * @param filter
     *            a user search filter
     * @return some search results
     * @throws NamingException
     */
    private boolean isAccountExists(String filter) throws NamingException {
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(getEnvironment());
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration<SearchResult> searchResults = ctx.search(getUserSearchBase(), filter, sc);
            if (log.isDebugEnabled()) {
                log.debug("Search LDAP for " + filter);
            }
            return searchResults.hasMoreElements();
        } catch (NamingException e) {
            throw e;
        } finally {
            try {
                ctx.close();
            } catch (Exception e2) {
            }
        }
    }

    /**
     * Get the user profile from the LDAP server
     * 
     * @param uid
     *            the unique identifier for the user provided by the
     *            authentication system
     * @return a list of IUserProfile instance or null if the user is not found
     * @throws NamingException
     */
    private List<IUserAuthenticationAccount> getAccountsFromLdapFilter(String filter) throws NamingException {
        DirContext ctx = null;
        DefaultUserAccount userAccount = null;
        List<IUserAuthenticationAccount> userAccounts = new ArrayList<IUserAuthenticationAccount>();
        try {
            ctx = new InitialDirContext(getEnvironment());

            // Search for user object
            NamingEnumeration<SearchResult> searchResults = searchLdapUserUsingFilter(filter, ctx);
            while (searchResults.hasMoreElements()) {
                userAccount = new DefaultUserAccount();
                SearchResult result = searchResults.nextElement();

                // Check if the account is active or not
                boolean isActive = false;
                if (getActivationLdapAttr() != null) {
                    Attribute activationAttribute = result.getAttributes().get(getActivationLdapAttr());
                    if (activationAttribute != null && activationAttribute.size() > 0) {
                        String value = String.valueOf(activationAttribute.get());
                        isActive = value.equals(getActivationLdapActiveValue());
                    }
                } else {
                    isActive = true;
                }

                // fill the user profile with the LDAP content
                userAccount.fill(getUserUniqueIdAttribute(), result.getAttributes(), isActive);

                if (log.isDebugEnabled()) {
                    log.debug("Search LDAP for filter " + filter + " user found");
                }

                // Add an account to a list
                userAccounts.add(userAccount);
            }
        } catch (NamingException e) {
            throw e;
        } finally {
            try {
                ctx.close();
            } catch (Exception e2) {
            }
        }
        return userAccounts;
    }

    private String getActivationLdapAttr() {
        return activationLdapAttr;
    }

    private String getActivationLdapActiveValue() {
        return activationLdapActiveValue;
    }

    private String getUserSearchFilter() {
        return userSearchFilter;
    }

    private String getUserUniqueIdAttribute() {
        return userUniqueIdAttribute;
    }

    private String getUserEmailSearchFilter() {
        return userEmailSearchFilter;
    }

    private Hashtable<String, String> getEnvironment() {
        return environment;
    }

    private String getUserSearchBase() {
        return userSearchBase;
    }

    private String getUserFullNameSearchFilter() {
        return userFullNameSearchFilter;
    }
}
