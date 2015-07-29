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
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;

import play.Logger;

/**
 * Default implementation for the interface
 * {@link IAuthenticationAccountWriterPlugin}. This implementation is based on a
 * LDAP server.<br/>
 * Obviously such features are only accessible if the master mode is activated.
 * In Slave mode (read-only authentication back-end), it is not possible to
 * update the LDAP back-end.
 * 
 * @author Pierre-Yves Cloux
 * 
 */
public class DefaultAuthenticationAccountWriterPlugin implements IAuthenticationAccountWriterPlugin {
    private Hashtable<String, String> environment;

    private String userDnTemplate;
    private String groupDnTemplate;
    private String activationLdapAttr;
    private String activationLdapActiveValue;
    private String activationLdapLockedValue;
    private String[] objectClasses;

    private static Logger.ALogger log = Logger.of(DefaultAuthenticationAccountWriterPlugin.class);

    /**
     * Creates a new instance
     * 
     * @param ldapUrl
     *            the LDAP url for the bind (example:
     *            ldap://localhost/dc=company, dc=com)
     * @param user
     *            the user to be used to connect to the server (example:
     *            cn=Directory Manager)
     * @param password
     * @param userDnTemplate
     *            a template to generate the DN for the newly created users
     *            (example: uid=%s,ou=People) the DN is relative to the initial
     *            context defined in ldapUrl
     * @param groupDnTemplate
     *            a template to generate the DN for a group (example:
     *            cn=%s,ou=Group) the DN is relative to the initial context
     *            defined in ldapUrl
     * @param objectClasses
     *            an array of objectclasses to be used to create the LDAP entry
     * @param activationLdapAttr
     *            the name of the attribute which contains the activation status
     * @param activationLdapActivValue
     *            the value which indicates that the user is "active"
     */
    public DefaultAuthenticationAccountWriterPlugin(String ldapUrl, String user, String password, String userDnTemplate, String groupDnTemplate,
            String[] objectClasses, String activationLdapAttr, String activationLdapActivValue, String activationLdapLockedValue) {
        environment = new Hashtable<String, String>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, ldapUrl);
        environment.put(Context.SECURITY_PRINCIPAL, user);
        environment.put(Context.SECURITY_CREDENTIALS, password);
        environment.put("com.sun.jndi.ldap.connect.pool", "true");
        this.userDnTemplate = userDnTemplate;
        this.groupDnTemplate = groupDnTemplate;
        this.activationLdapAttr = activationLdapAttr;
        this.activationLdapActiveValue = activationLdapActivValue;
        this.activationLdapLockedValue = activationLdapLockedValue;
        this.objectClasses = objectClasses;

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

    private String getUserDnTemplate() {
        return userDnTemplate;
    }

    private String getActivationLdapActiveValue() {
        return activationLdapActiveValue;
    }

    private String[] getObjectClasses() {
        return objectClasses;
    }

    private String getActivationLdapAttr() {
        return activationLdapAttr;
    }

    private String getActivationLdapLockedValue() {
        return activationLdapLockedValue;
    }

    private String getGroupDnTemplate() {
        return groupDnTemplate;
    }
}
