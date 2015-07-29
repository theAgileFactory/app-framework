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
package framework.services.audit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.framework_models.parent.IModel;

import org.apache.commons.io.FileUtils;

import play.Logger;
import play.Play;
import play.mvc.Http;
import framework.services.session.IUserSessionManagerPlugin;
import framework.utils.Utilities;

/**
 * This utility class loads the audit configuration from the {@link Auditable}
 * entity. It is implemented as a singleton. The method "init" must be called
 * with the userSession manager once this one is available. Before this is done,
 * the logging feature is not activated.<br/>
 * 
 * The logs contains the login of the currently logged user.<br/>
 * If no user is logged (example: scheduled processes, the user login is SYSTEM)
 * 
 * @author Pierre-Yves Cloux
 */
public class AuditLoggerUtilities {
    /**
     * User login to be used when no user is logged
     */
    private static final String SYSTEM = "_SYSTEM";

    private static AuditLoggerUtilities instance;
    private IUserSessionManagerPlugin userSessionManager;
    private static Logger.ALogger log = Logger.of(AuditLoggerUtilities.class);
    private Map<String, Boolean> auditableEntities = Collections.synchronizedMap(new HashMap<String, Boolean>());

    public enum AuditedAction {
        CREATE, UPDATE, DELETE;
    }

    /**
     * Return the instance of the singleton
     * 
     * @return
     */
    public static AuditLoggerUtilities getInstance() {
        if (instance == null) {
            instance = new AuditLoggerUtilities();
        }
        return instance;
    }

    /**
     * Set the {@link IUserSessionManagerPlugin}.<br/>
     * Before this is done, the audit logging is not activated.
     * 
     * @param userSessionManager
     *            the user session manager.
     */
    public static void init(IUserSessionManagerPlugin userSessionManager) {
        getInstance().userSessionManager = userSessionManager;
        getInstance().reload();
    }

    /**
     * Creates a new instance.
     * 
     * @param userSessionManager
     *            the user session manager (will be used to enrich the log with
     *            the id of the user)
     * @param auditableEntitiesFilePath
     *            path to the file which contains the auditable entities
     */
    private AuditLoggerUtilities() {
    }

    /**
     * Log the creation of an entity
     * 
     * @param entity
     */
    public void logCreate(Object entity) {
        log(AuditedAction.CREATE, entity);
    }

    /**
     * Log the update of an entity
     * 
     * @param entity
     */
    public void logUpdate(Object entity) {
        log(AuditedAction.UPDATE, entity);
    }

    /**
     * Log the deletion of an entity
     * 
     * @param entity
     */
    public void logDelete(Object entity) {
        log(AuditedAction.DELETE, entity);
    }

    /**
     * Log a message to the audit log
     * 
     * @param action
     *            an auditable action
     * @param message
     *            a message
     */
    private void log(AuditedAction action, Object entity) {
        if (entity != null && IModel.class.isAssignableFrom(entity.getClass()) && getUserSessionManager() != null) {
            Boolean flag = auditableEntities.get(entity.getClass().getName());
            if (flag != null && flag) {
                log.info(String.format("%s/%s/%s", action.name(), getCurrentUserLogin(), ((IModel) entity).audit()));
            }
        }
    }

    /**
     * Return the currently logged user or SYSTEM is an exception is thrown.
     * Such exception may happens if no user are logged (example scheduled
     * actions).
     * 
     * @return
     */
    private String getCurrentUserLogin() {
        try {
            return getUserSessionManager().getUserSessionId(Http.Context.current());
        } catch (Exception e) {
            return SYSTEM;
        }
    }

    @SuppressWarnings("unchecked")
    /**
     * Reload the audit configuration
     */
    public synchronized void reload() {
        synchronized (this.auditableEntities) {
            this.auditableEntities.clear();
            try {
                String auditableEntitiesFilePath = Play.application().configuration().getString("maf.auditable.entities.file");
                File auditableEntitiesFile = new File(auditableEntitiesFilePath);
                if (!auditableEntitiesFile.exists() || !auditableEntitiesFile.isFile()) {
                    log.error("Auditable entities file " + auditableEntitiesFilePath + " not found, will be created next time an item is edited");
                    return;
                }
                byte[] data = FileUtils.readFileToByteArray(auditableEntitiesFile);
                Map<String, Boolean> temp = (Map<String, Boolean>) Utilities.unmarshallObject(data);
                this.auditableEntities.putAll(temp);
            } catch (Exception e) {
                log.error("Exception while reading the auditable entities file", e);
            }
        }
    }

    /**
     * Return the Auditable associated with the specified objectClass
     * 
     * @param objectClass
     *            an Auditable objectClass
     * @return a Auditable instance
     */
    public Auditable getAuditableFromObjectClass(String objectClass) {
        return new Auditable(objectClass, getAuditableEntities().get(objectClass));
    }

    /**
     * Return all the {@link Auditable} objects which are not deleted
     * 
     * @return
     */
    public List<Auditable> getAllActiveAuditable() {
        List<Auditable> auditables = new ArrayList<Auditable>();
        for (String objectClass : getAuditableEntities().keySet()) {
            auditables.add(new Auditable(objectClass, getAuditableEntities().get(objectClass)));
        }
        return auditables;
    }

    /**
     * Save an auditable object to the system (either update or new)
     * 
     * @param auditable
     */
    public void saveAuditable(Auditable auditable) {
        synchronized (auditableEntities) {
            this.auditableEntities.put(auditable.objectClass, auditable.isAuditable);
            try {
                String auditableEntitiesFilePath = Play.application().configuration().getString("maf.auditable.entities.file");
                File auditableEntitiesFile = new File(auditableEntitiesFilePath);
                FileUtils.writeByteArrayToFile(auditableEntitiesFile, Utilities.marshallObject(auditableEntities));
            } catch (Exception e) {
                log.error("Exception while writing the auditable entities file", e);
            }
        }
    }

    /**
     * Delete an auditable object to the system
     * 
     * @param objectClass
     *            an Auditable objectClass
     */
    public void deleteAuditable(String objectClass) {
        synchronized (auditableEntities) {
            this.auditableEntities.remove(objectClass);
            try {
                String auditableEntitiesFilePath = Play.application().configuration().getString("maf.auditable.entities.file");
                File auditableEntitiesFile = new File(auditableEntitiesFilePath);
                FileUtils.writeByteArrayToFile(auditableEntitiesFile, Utilities.marshallObject(auditableEntities));
            } catch (Exception e) {
                log.error("Exception while writing the auditable entities file", e);
            }
        }
    }

    private IUserSessionManagerPlugin getUserSessionManager() {
        return userSessionManager;
    }

    private Map<String, Boolean> getAuditableEntities() {
        return auditableEntities;
    }
}
