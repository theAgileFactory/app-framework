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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;

import framework.services.session.IUserSessionManagerPlugin;
import framework.utils.Utilities;
import models.framework_models.parent.IModel;
import play.Configuration;
import play.Logger;
import play.Play;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import play.mvc.Http;

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
@Singleton
public class AuditLoggerServiceImpl implements IAuditLoggerService {
    /**
     * User login to be used when no user is logged
     */
    private static final String SYSTEM = "_SYSTEM";
    private String auditableEntitiesFilePath;
    private IUserSessionManagerPlugin userSessionManager;
    private static Logger.ALogger log = Logger.of(AuditLoggerServiceImpl.class);
    private Map<String, Boolean> auditableEntities = Collections.synchronizedMap(new HashMap<String, Boolean>());

    public enum AuditedAction {
        CREATE, UPDATE, DELETE;
    }

    public enum Config {
        AUDITABLE_ENTITIES_FILE("maf.auditable.entities.file");

        private String configurationKey;

        private Config(String configurationKey) {
            this.configurationKey = configurationKey;
        }

        public String getConfigurationKey() {
            return configurationKey;
        }
    }

    /**
     * Creates a new instance.
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param userSessionManager
     *            the user session manager (will be used to enrich the log with
     *            the id of the user)
     */
    @Inject
    public AuditLoggerServiceImpl(ApplicationLifecycle lifecycle, Configuration configuration, IUserSessionManagerPlugin userSessionManager) {
        log.info("SERVICE>>> AuditLoggerServiceImpl starting...");
        this.userSessionManager = userSessionManager;
        this.auditableEntitiesFilePath = configuration.getString(Config.AUDITABLE_ENTITIES_FILE.getConfigurationKey());
        log.info("Activating audit log with audit log file " + this.auditableEntitiesFilePath);
        reload();
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> AuditLoggerServiceImpl stopping...");
            log.info("SERVICE>>> AuditLoggerServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> AuditLoggerServiceImpl started");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * framework.services.audit.IAuditLoggerService#logCreate(java.lang.Object)
     */
    @Override
    public void logCreate(Object entity) {
        log(AuditedAction.CREATE, entity);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * framework.services.audit.IAuditLoggerService#logUpdate(java.lang.Object)
     */
    @Override
    public void logUpdate(Object entity) {
        log(AuditedAction.UPDATE, entity);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * framework.services.audit.IAuditLoggerService#logDelete(java.lang.Object)
     */
    @Override
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

    /*
     * (non-Javadoc)
     * 
     * @see framework.services.audit.IAuditLoggerService#reload()
     */
    @Override
    @SuppressWarnings("unchecked")
    /**
     * Reload the audit configuration
     */
    public synchronized void reload() {
        synchronized (this.auditableEntities) {
            this.auditableEntities.clear();
            try {
                File auditableEntitiesFile = new File(getAuditableEntitiesFilePath());
                if (!auditableEntitiesFile.exists() || !auditableEntitiesFile.isFile()) {
                    log.warn("Auditable entities file " + auditableEntitiesFilePath + " not found, will be created next time an item is edited");
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * framework.services.audit.IAuditLoggerService#getAuditableFromObjectClass
     * (java.lang.String)
     */
    @Override
    public Auditable getAuditableFromObjectClass(String objectClass) {
        return new Auditable(objectClass, getAuditableEntities().get(objectClass));
    }

    /*
     * (non-Javadoc)
     * 
     * @see framework.services.audit.IAuditLoggerService#getAllActiveAuditable()
     */
    @Override
    public List<Auditable> getAllActiveAuditable() {
        List<Auditable> auditables = new ArrayList<Auditable>();
        for (String objectClass : getAuditableEntities().keySet()) {
            auditables.add(new Auditable(objectClass, getAuditableEntities().get(objectClass)));
        }
        return auditables;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * framework.services.audit.IAuditLoggerService#saveAuditable(framework.
     * services.audit.Auditable)
     */
    @Override
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * framework.services.audit.IAuditLoggerService#deleteAuditable(java.lang
     * .String)
     */
    @Override
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

    private String getAuditableEntitiesFilePath() {
        return auditableEntitiesFilePath;
    }
}
