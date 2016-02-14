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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;

import framework.services.database.IDatabaseDependencyService;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;

/**
 * Default implementation for the interface
 * {@link IAuthenticationAccountWriterPlugin}. This implementation is based on a
 * LDAP server.<br/>
 * Obviously such features are only accessible if the master mode is activated.
 * In Slave mode (read-only authentication back-end), it is not possible to
 * update the LDAP back-end.<br/>
 * Here are the properties:
 * <ul>
 * <li>ldapUrl : the LDAP url for the bind (example:
 * ldap://localhost/dc=company, dc=com)</li>
 * <li>user : the user to be used to connect to the server (example:
 * cn=Directory Manager)</li>
 * <li>password : the password for the previous user</li>
 * <li>userDnTemplate : a template to generate the DN for the newly created
 * users (example: uid=%s,ou=People) the DN is relative to the initial context
 * defined in ldapUrl</li>
 * <li>groupDnTemplate : a template to generate the DN for a group (example:
 * cn=%s,ou=Group) the DN is relative to the initial context defined in ldapUrl
 * </li>
 * <li>objectClasses : an array of objectclasses to be used to create the LDAP
 * entry (defined as a constant in the class implementation)</li>
 * <li>activationLdapAttr : the name of the attribute which contains the
 * activation status</li>
 * <li>activationLdapActivValue : the value which indicates that the user is
 * "active"</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 * 
 */
@Singleton
public class DefaultAuthenticationAccountWriterPlugin implements IAuthenticationAccountWriterPlugin {
    private Hashtable<String, String> environment;

    private String userDnTemplate = "uid=%s,ou=people";
    private String groupDnTemplate = "cn=%s,ou=groups";
    private String activationLdapAttr = "description";
    private String activationLdapActiveValue = "status=active";
    private String activationLdapLockedValue = "status=locked";
    private String[] objectClasses = { "top", "person", "organizationalPerson", "inetOrgPerson" };

    private static Logger.ALogger log = Logger.of(DefaultAuthenticationAccountWriterPlugin.class);

    public enum Config {
        LDAP_URL("maf.ldap_url"), LDAP_USER("maf.user"), LDAP_PASSWORD("maf.password"), USER_DN_TEMPLATE("maf.user_dn_template"), GROUP_DN_TEMPLATE(
                "maf.group_dn_template"), ACTIVATION_ATTRIBUTE_NAME("maf.activation_ldap_attribute"), ACTIVATION_ATTRIBUTE_VALUE(
                        "maf.activation_ldap_attribute_activated"), ACTIVATION_ATTRIBUTE_VALUE_LOCKED("maf.activation_ldap_attribute_locked");

        private String configurationKey;

        private Config(String configurationKey) {
            this.configurationKey = configurationKey;
        }

        public String getConfigurationKey() {
            return configurationKey;
        }
    }

    /**
     * Creates a new DefaultAuthenticationAccountWriterPlugin
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param databaseDependencyService
     */
    @Inject
    public DefaultAuthenticationAccountWriterPlugin(ApplicationLifecycle lifecycle, Configuration configuration,
            IDatabaseDependencyService databaseDependencyService) {
        log.info("SERVICE>>> DefaultAuthenticationAccountWriterPlugin starting...");
        environment = new Hashtable<String, String>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, configuration.getString(Config.LDAP_URL.getConfigurationKey()));
        environment.put(Context.SECURITY_PRINCIPAL, configuration.getString(Config.LDAP_USER.getConfigurationKey()));
        environment.put(Context.SECURITY_CREDENTIALS, configuration.getString(Config.LDAP_PASSWORD.getConfigurationKey()));
        environment.put("com.sun.jndi.ldap.connect.pool", "true");
        this.userDnTemplate = configuration.getString(Config.USER_DN_TEMPLATE.getConfigurationKey());
        this.groupDnTemplate = configuration.getString(Config.GROUP_DN_TEMPLATE.getConfigurationKey());
        this.activationLdapAttr = configuration.getString(Config.ACTIVATION_ATTRIBUTE_NAME.getConfigurationKey());
        this.activationLdapActiveValue = configuration.getString(Config.ACTIVATION_ATTRIBUTE_VALUE.getConfigurationKey());
        this.activationLdapLockedValue = configuration.getString(Config.ACTIVATION_ATTRIBUTE_VALUE_LOCKED.getConfigurationKey());
        if (log.isDebugEnabled()) {
            log.debug("Initialized with " + environment.toString());
        }
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> DefaultAuthenticationAccountWriterPlugin stopping...");
            log.info("SERVICE>>> DefaultAuthenticationAccountWriterPlugin stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> DefaultAuthenticationAccountWriterPlugin started");
    }

    /**
     * Creates manually an account writer
     * 
     * @param ldapUrl
     *            a LDAP URL (ex: ldap://host:389)
     * @param ldapUser
     *            a LDAP user allowed to write the directory
     * @param ldapPassword
     *            the password for the LDAP user
     */
    public DefaultAuthenticationAccountWriterPlugin(String ldapUrl, String ldapUser, String ldapPassword) {
        log.info("SERVICE>>> DefaultAuthenticationAccountWriterPlugin starting...");
        environment = new Hashtable<String, String>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, ldapUrl);
        environment.put(Context.SECURITY_PRINCIPAL, ldapUser);
        environment.put(Context.SECURITY_CREDENTIALS, ldapPassword);
        environment.put("com.sun.jndi.ldap.connect.pool", "true");
        if (log.isDebugEnabled()) {
            log.debug("Initialized with " + environment.toString());
        }
    }

    private Hashtable<String, String> getEnvironment() {
        return environment;
    }

    @Override
    public void createUserProfile(String uid, String firstName, String lastName, String mail, String password) throws AccountManagementException {
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(getEnvironment());
            Attributes userAttrs = new BasicAttributes(true);
            userAttrs.put(new BasicAttribute("uid", uid));
            userAttrs.put(new BasicAttribute("cn", String.format("%s %s", firstName, lastName)));
            userAttrs.put(new BasicAttribute("givenname", firstName));
            userAttrs.put(new BasicAttribute("sn", lastName));
            userAttrs.put(new BasicAttribute("mail", mail));
            userAttrs.put(new BasicAttribute("userpassword", password));

            // Set the objectclasses for the entry
            BasicAttribute objectClassAttribute = new BasicAttribute("objectclass");
            for (String objectClassName : getObjectClasses()) {
                objectClassAttribute.add(objectClassName);
            }
            userAttrs.put(objectClassAttribute);

            // Set the activation status
            if (getActivationLdapAttr() != null) {
                userAttrs.put(new BasicAttribute(getActivationLdapAttr(), getActivationLdapActiveValue()));
            }

            String dn = String.format(getUserDnTemplate(), uid);
            ctx.bind(dn, ctx, userAttrs);
            log.info(String.format("User created in LDAP with dn %s", dn));
        } catch (NamingException e) {
            log.error("Exception while creating the user account " + uid, e);
            throw new AccountManagementException(e);
        } finally {
            try {
                ctx.close();
            } catch (Exception e2) {
            }
        }
    }

    @Override
    public void updateUserProfile(String uid, String firstName, String lastName, String mail) throws AccountManagementException {
        DirContext ctx = null;
        try {
            String userDn = String.format(getUserDnTemplate(), uid);
            ctx = new InitialDirContext(getEnvironment());

            List<ModificationItem> mods = new ArrayList<ModificationItem>();

            if (firstName != null && lastName != null) {
                Attribute attr1 = new BasicAttribute("givenname", firstName);
                mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attr1));
                Attribute attr2 = new BasicAttribute("sn", lastName);
                mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attr2));
                Attribute attr3 = new BasicAttribute("cn", String.format("%s %s", firstName, lastName));
                mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attr3));
            }
            if (mail != null) {
                Attribute attr = new BasicAttribute("mail", mail);
                mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attr));
            }

            ModificationItem[] modsArray = mods.toArray(new ModificationItem[mods.size()]);

            ctx.modifyAttributes(userDn, modsArray);
            log.info(String.format("User modified in LDAP with dn %s", userDn));
        } catch (NamingException e) {
            log.error("Exception while creating the user account " + uid, e);
            throw new AccountManagementException(e);
        } finally {
            try {
                ctx.close();
            } catch (Exception e2) {
            }
        }
    }

    /**
     * Replace the specified attribute in the LDAP entry
     * 
     * @param userDn
     *            the DN of the LDAP entry to be modified
     * @param attributeName
     *            the name of the attribute to update
     * @param value
     *            the value of this attribute
     * @throws NamingException
     */
    private void replaceAttribute(String userDn, String attributeName, String value) throws NamingException {
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(getEnvironment());
            ModificationItem[] mods = new ModificationItem[1];
            Attribute mod0 = new BasicAttribute(attributeName, value);
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, mod0);
            ctx.modifyAttributes(userDn, mods);
        } catch (NamingException e) {
            throw e;
        } finally {
            try {
                ctx.close();
            } catch (Exception e2) {
            }
        }
    }

    @Override
    public void deleteUserProfile(String uid) throws AccountManagementException {
        DirContext ctx = null;
        try {
            String userDn = String.format(getUserDnTemplate(), uid);
            ctx = new InitialDirContext(getEnvironment());
            ctx.destroySubcontext(userDn);
            log.info(String.format("User deleted in LDAP with dn %s", userDn));
        } catch (NamingException e) {
            log.error("Exception while deleting the user account " + uid, e);
            throw new AccountManagementException(e);
        } finally {
            try {
                ctx.close();
            } catch (Exception e2) {
            }
        }
    }

    @Override
    public void changePassword(String uid, String password) throws AccountManagementException {
        try {
            String userDn = String.format(getUserDnTemplate(), uid);
            replaceAttribute(userDn, "userpassword", password);
            log.info(String.format("User password changed in LDAP with dn %s", userDn));
        } catch (NamingException e) {
            log.error("Exception changing the user password " + uid, e);
            throw new AccountManagementException(e);
        }
    }

    @Override
    public void changeActivationStatus(String uid, boolean isActive) throws AccountManagementException {
        if (getActivationLdapAttr() != null) {
            try {
                String userDn = String.format(getUserDnTemplate(), uid);
                replaceAttribute(userDn, getActivationLdapAttr(), isActive ? getActivationLdapActiveValue() : getActivationLdapLockedValue());
                log.info(String.format("User %s activation status changed in LDAP for %s", userDn, isActive));
            } catch (NamingException e) {
                log.error("Exception changing the user password " + uid, e);
                throw new AccountManagementException(e);
            }
        }
    }

    @Override
    public void addUserToGroup(String uid, String groupName) throws AccountManagementException {
        DirContext ctx = null;
        try {
            String userDn = String.format(getUserDnTemplate(), uid);
            String groupDn = String.format(getGroupDnTemplate(), groupName);
            ctx = new InitialDirContext(getEnvironment());
            String fullUserDn = String.format("%s,%s", userDn, ctx.getNameInNamespace());

            DirContext groupEntry = (DirContext) ctx.lookup(groupDn);
            Attributes groupAttrs = groupEntry.getAttributes("");
            Attribute uniqueMemberAttribute = groupAttrs.get("uniqueMember");

            ModificationItem[] mods = new ModificationItem[1];
            if (uniqueMemberAttribute != null) {
                // Check if the specified user is already member
                boolean isAlreadyMember = false;
                NamingEnumeration<?> valueEnums = uniqueMemberAttribute.getAll();
                while (valueEnums.hasMore() && !isAlreadyMember) {
                    String value = (String) valueEnums.next();
                    isAlreadyMember = value.equalsIgnoreCase(fullUserDn);
                }
                if (isAlreadyMember)
                    return;

                // The attribute exists, this is an update
                uniqueMemberAttribute.add(fullUserDn);
                mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, uniqueMemberAttribute);
            } else {
                // The attribute does not exists, this is create
                Attribute mod0 = new BasicAttribute("uniqueMember", fullUserDn);
                mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, mod0);
            }
            ctx.modifyAttributes(groupDn, mods);
            log.info(String.format("User %s added in LDAP to the group %s", userDn, groupDn));
        } catch (NamingException e) {
            log.error(String.format("User %s added in LDAP to the group %s", uid, groupName), e);
            throw new AccountManagementException(e);
        } finally {
            try {
                ctx.close();
            } catch (Exception e2) {
            }
        }
    }

    @Override
    public void removeUserFromGroup(String uid, String groupName) throws AccountManagementException {
        DirContext ctx = null;
        try {
            String userDn = String.format(getUserDnTemplate(), uid);
            String groupDn = String.format(getGroupDnTemplate(), groupName);
            ctx = new InitialDirContext(getEnvironment());
            String fullUserDn = String.format("%s,%s", userDn, ctx.getNameInNamespace());

            DirContext groupEntry = (DirContext) ctx.lookup(groupDn);
            Attributes groupAttrs = groupEntry.getAttributes("");
            Attribute uniqueMemberAttribute = groupAttrs.get("uniqueMember");

            ModificationItem[] mods = new ModificationItem[1];
            if (uniqueMemberAttribute != null) {
                // Check if the specified user is already member
                boolean isAlreadyMember = false;
                NamingEnumeration<?> valueEnums = uniqueMemberAttribute.getAll();
                while (valueEnums.hasMore() && !isAlreadyMember) {
                    String value = (String) valueEnums.next();
                    isAlreadyMember = value.equalsIgnoreCase(fullUserDn);
                }
                // If is not member, then stop here
                if (!isAlreadyMember)
                    return;

                // Update the attribute
                uniqueMemberAttribute.remove(fullUserDn);
                mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, uniqueMemberAttribute);
            }
            ctx.modifyAttributes(groupDn, mods);
            log.info(String.format("User %s removed in LDAP from the group %s", userDn, groupDn));
        } catch (NamingException e) {
            log.error(String.format("User %s removed in LDAP from the group %s", uid, groupName), e);
            throw new AccountManagementException(e);
        } finally {
            try {
                ctx.close();
            } catch (Exception e2) {
            }
        }
    }

    public String getUserDnTemplate() {
        return userDnTemplate;
    }

    public String getActivationLdapActiveValue() {
        return activationLdapActiveValue;
    }

    public String[] getObjectClasses() {
        return objectClasses;
    }

    public String getActivationLdapAttr() {
        return activationLdapAttr;
    }

    public String getActivationLdapLockedValue() {
        return activationLdapLockedValue;
    }

    public String getGroupDnTemplate() {
        return groupDnTemplate;
    }

    public void setUserDnTemplate(String userDnTemplate) {
        this.userDnTemplate = userDnTemplate;
    }

    public void setGroupDnTemplate(String groupDnTemplate) {
        this.groupDnTemplate = groupDnTemplate;
    }

    public void setActivationLdapAttr(String activationLdapAttr) {
        this.activationLdapAttr = activationLdapAttr;
    }

    public void setActivationLdapActiveValue(String activationLdapActiveValue) {
        this.activationLdapActiveValue = activationLdapActiveValue;
    }

    public void setActivationLdapLockedValue(String activationLdapLockedValue) {
        this.activationLdapLockedValue = activationLdapLockedValue;
    }

    public void setObjectClasses(String[] objectClasses) {
        this.objectClasses = objectClasses;
    }
}
