package framework.services.action_log;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.configuration.PropertiesConfiguration;

import framework.commons.DataType;
import framework.services.session.IUserSessionManagerPlugin;
import models.framework_models.account.Principal;
import models.framework_models.common.ActionLog;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;
import play.mvc.Http;

/**
 * The action log service.
 * 
 * @author Johann Kohler
 *
 */
@Singleton
public class ActionLogServiceImpl implements IActionLogService {

    private static Logger.ALogger log = Logger.of(ActionLogServiceImpl.class);

    private IUserSessionManagerPlugin userSessionManagerPlugin;

    /**
     * Construct the service.
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param userSessionManagerPlugin
     *            the user session manager service
     */
    @Inject
    public ActionLogServiceImpl(ApplicationLifecycle lifecycle, Configuration configuration, IUserSessionManagerPlugin userSessionManagerPlugin) {
        log.info("SERVICE>>> ActionLogServiceImpl starting...");
        this.userSessionManagerPlugin = userSessionManagerPlugin;
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> ActionLogServiceImpl stopping...");
            log.info("SERVICE>>> ActionLogServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> ActionLogServiceImpl started...");
    }

    @Override
    public void log(DataType dataType, Long objectId, String action, PropertiesConfiguration parameters) {
        String uid = this.getUserSessionManagerPlugin().getUserSessionId(Http.Context.current());
        ActionLog actionLog = new ActionLog();
        actionLog.principal = Principal.getPrincipalFromUid(uid);
        actionLog.action = action;
        actionLog.objectId = objectId;
        actionLog.objectType = dataType.getDataTypeClassName();
        actionLog.parameters = parameters.toString();
    }

    @Override
    public List<ActionLog> getLogs(DataType dataType, Long objectId, String action) {
        return ActionLog.getActionLogAsListByObjectAndAction(dataType.getDataTypeClassName(), objectId, action);
    }

    @Override
    public List<ActionLog> getLogs(DataType dataType, Long objectId, String action, Date fromDate) {
        return ActionLog.getActionLogAsListByObjectAndAction(dataType.getDataTypeClassName(), objectId, action, fromDate);
    }

    /**
     * Get the user session manager service.
     */
    private IUserSessionManagerPlugin getUserSessionManagerPlugin() {
        return this.userSessionManagerPlugin;
    }

}
