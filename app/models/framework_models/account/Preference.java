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
package models.framework_models.account;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Version;

import models.framework_models.common.CustomAttributeDefinition;
import models.framework_models.common.ICustomAttributeValue;
import models.framework_models.parent.IModel;
import models.framework_models.parent.IModelConstants;
import play.Logger;
import play.cache.Cache;
import play.mvc.Controller;

import com.avaje.ebean.Model;

import framework.commons.IFrameworkConstants;
import framework.services.ServiceManager;
import framework.services.account.AccountManagementException;
import framework.services.account.IAccountManagerPlugin;
import framework.services.account.IUserAccount;
import framework.services.session.IUserSessionManagerPlugin;

/**
 * A preference describes a parameter which defines some specific configurable
 * behavior of the system.<br/>
 * <p>
 * The gategory of a preference is obtained from the "prefix" of the preference
 * uuid.<br/>
 * For instance:<br/>
 * GOVERNANCE_EVENT_NOTIFICATION_PREFERENCE is a preference which category is
 * GOVERNANCE.<br/>
 * A i18n name of the category can be found using a key with the following
 * pattern:<br/>
 * maf.preference.category.[category lowercase].name
 * </p>
 * The preference uuid is matching the uuid of a
 * {@link CustomAttributeDefinition}.
 * <p>
 * There are two types of preferences:
 * <ul>
 * <li>system : which is unique for the whole system and thus attached to
 * {@link Object} class</li>
 * <li>user : which is attached to any user (and thus normally modifiable by
 * each user) and attached to {@link Principal}</li>
 * <li></li>
 * </ul>
 * </p>
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class Preference extends Model implements IModel {
    private static final long serialVersionUID = 7613063223004631793L;

    private static Logger.ALogger log = Logger.of(Preference.class);

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, Preference> find = new Finder<Long, Preference>(Preference.class);

    public boolean deleted = false;
    @Version
    public Timestamp lastUpdate;

    @Id
    public Long id;

    public boolean systemPreference;

    @Column(length = IModelConstants.LARGE_STRING)
    public String uuid;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "preference_has_system_permission")
    public List<SystemPermission> systemPermissions;

    /**
     * Returns the category of the preference
     * 
     * @return
     */
    public String getCategory() {
        if (uuid.indexOf('_') != -1 && uuid.indexOf('_') < uuid.length()) {
            return uuid.substring(uuid.indexOf('_') + 1);
        }
        return uuid;
    }

    public Preference() {
    }

    @Override
    public String audit() {
        return "Preference [deleted=" + deleted + ", lastUpdate=" + lastUpdate + ", id=" + id + ", uuid=" + uuid + "]";
    }

    @Override
    public void defaults() {
    }

    @Override
    public void doDelete() {
        this.deleted = true;
        save();
    }

    @Override
    public String toString() {
        return String.valueOf(this.id);
    }

    /**
     * Check if the permissions stored in the database match the one defined in
     * the class (as a static final variable).<br/>
     * This is required in order to avoid inconsistencies between the code and
     * the database content.
     * 
     * @return a boolean
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static boolean checkPreferences(Class<?> preferenceClass) throws IllegalArgumentException, IllegalAccessException {
        List<Preference> preferences = find.where().eq("deleted", false).findList();
        List<String> possibleValuesForPreference = new ArrayList<String>();
        for (Field field : preferenceClass.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) && field.getName().endsWith("_PREFERENCE")) {
                possibleValuesForPreference.add(String.valueOf(field.get(Preference.class)));
            }
        }
        // Add the standard framework preferences
        for (Field field : IFrameworkConstants.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) && field.getName().endsWith("_PREFERENCE")) {
                possibleValuesForPreference.add(String.valueOf(field.get(Preference.class)));
            }
        }
        if (preferences != null) {
            for (Preference preference : preferences) {
                try {
                    if (possibleValuesForPreference.contains(preference.uuid)) {
                        possibleValuesForPreference.remove(preference.uuid);
                    }
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        }
        return possibleValuesForPreference.size() == 0;
    }

    /**
     * Invalidate the system cache for the specified preference id.<br/>
     * Warning: this is critical for system preferences which are systematically
     * cached.
     * 
     * @param uuid
     *            a preference uuid
     */
    public static void savePreferenceValue(ICustomAttributeValue customAttributeValue) {
        String uuid = customAttributeValue.getDefinition().uuid;
        if (log.isDebugEnabled()) {
            log.debug("Saving preference with uuid " + uuid);
        }
        Preference preference = find.where().eq("uuid", uuid).eq("deleted", false).findUnique();
        if (preference == null) {
            log.error("Attempt to save a preference which is not a preference " + uuid);
            return;
        }
        if (preference.systemPreference) {
            if (log.isDebugEnabled()) {
                log.debug("Preference with uuid " + uuid + " is a system preference, flushing the cache");
            }
            Cache.remove(IFrameworkConstants.SYSTEM_PREFERENCE_CACHE_PREFIX + uuid);
        }
        customAttributeValue.performSave();
    }

    public static Preference getPreferenceFromUuid(String uuid) {
        return find.where().eq("uuid", uuid).eq("deleted", false).findUnique();
    }

    /**
     * Return the custom attribute value associated with this preference.<br/>
     * If the preference is a system preference, its value is taken from the
     * system {@link Cache}
     * 
     * @param uuid
     *            a preference uuid
     * @return a custom attribute value
     */
    public static ICustomAttributeValue getPreferenceValueFromUuid(String uuid) {
        if (log.isDebugEnabled()) {
            log.debug("Getting preference with uuid " + uuid);
        }
        Preference preference = find.where().eq("uuid", uuid).eq("deleted", false).findUnique();
        if (preference == null) {
            if (log.isDebugEnabled()) {
                log.debug("Preference with uuid " + uuid + " not found");
            }
            return null;
        }
        if (preference.systemPreference) {
            if (log.isDebugEnabled()) {
                log.debug("Preference with uuid " + uuid + " is a system preference");
            }
            ICustomAttributeValue attributeValue = (ICustomAttributeValue) Cache.get(IFrameworkConstants.SYSTEM_PREFERENCE_CACHE_PREFIX + uuid);
            if (attributeValue != null) {
                if (log.isDebugEnabled()) {
                    CustomAttributeDefinition customAttributeDefinition = attributeValue.getDefinition();
                    String message = (customAttributeDefinition != null ? customAttributeDefinition.name : attributeValue.getClass().getName());
                    log.debug("Preference with uuid " + uuid + " found in cache " + message + ", returning : " + attributeValue);
                }
                attributeValue.resetError();
                return attributeValue;
            }
            // The preference is attached to the system and unique
            attributeValue = CustomAttributeDefinition.getCustomAttributeValue(preference.uuid, Object.class, 1L);
            if (attributeValue.isNotReadFromDb()) {
                if (log.isDebugEnabled()) {
                    log.debug("Preference with uuid " + uuid + " NOT found in cache and not in DB, using default value");
                }
                attributeValue.defaults();
            }
            Cache.set(IFrameworkConstants.SYSTEM_PREFERENCE_CACHE_PREFIX + uuid, attributeValue);
            if (log.isDebugEnabled()) {
                log.debug("Preference with uuid " + uuid + " set in cache returning : " + attributeValue);
            }
            return attributeValue;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Preference with uuid " + uuid + " is a user preference");
            }
            try {
                // The preference is attached to a user and specific
                IUserSessionManagerPlugin userSessionManager = ServiceManager.getService(IUserSessionManagerPlugin.NAME, IUserSessionManagerPlugin.class);
                String userSessionUid = userSessionManager.getUserSessionId(Controller.ctx());
                IAccountManagerPlugin accountManagerPlugin = ServiceManager.getService(IAccountManagerPlugin.NAME, IAccountManagerPlugin.class);
                IUserAccount userAccount = accountManagerPlugin.getUserAccountFromUid(userSessionUid);
                ICustomAttributeValue attributeValue = CustomAttributeDefinition.getCustomAttributeValue(preference.uuid, Principal.class,
                        userAccount.getMafUid());
                if (log.isDebugEnabled()) {
                    log.debug("Preference with uuid " + uuid + " looking for value for user " + userAccount.getMafUid());
                }
                if (attributeValue.isNotReadFromDb()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Preference with uuid " + uuid + " no value in DB, using default");
                    }
                    attributeValue.defaults();
                }
                if (log.isDebugEnabled()) {
                    log.debug("Preference with uuid " + uuid + " returning : " + attributeValue);
                }
                return attributeValue;
            } catch (AccountManagementException e) {
                log.error("Error while finding the preference " + uuid, e);
            }
            return null;
        }
    }
}
