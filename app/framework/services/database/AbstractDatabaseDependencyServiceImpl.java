package framework.services.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.server.lib.BizDockEbeanShutdownManager;

import models.framework_models.patcher.Patch;
import play.Configuration;
import play.Environment;
import play.Logger;
import play.db.DBApi;
import play.db.ebean.EbeanConfig;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;

/**
 * This service is to ensure that Ebean is loaded before a service depending on
 * the database requires an access to some data
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public abstract class AbstractDatabaseDependencyServiceImpl implements IDatabaseDependencyService {
    /**
     * How many threads to manage the cache in Ebean
     */
    public static final int EBEAN_CACHE_THREAD_POOL_SIZE = 5;
    /**
     * How many seconds should Ebean cache wait before stopping
     */
    public static final int EBEAN_CACHE_SHUTDOWN_DELAY = 30;
    public static final String EBEAN_SERVER_DEFAULT_NAME = "default";
    private static Logger.ALogger log = Logger.of(AbstractDatabaseDependencyServiceImpl.class);

    /**
     * Create a new AbstractDatabaseDependencyServiceImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param environment
     *            the environment
     * @param configuration
     *            the play application configuration
     * @param ebeanConfig
     *            the Ebean configuration as loaded by the play plugin
     * @param dbApi
     *            the play database API (this must prevent the database API to
     *            be closed before this service is stopped)
     */
    @Inject
    public AbstractDatabaseDependencyServiceImpl(ApplicationLifecycle lifecycle, Environment environment, Configuration configuration, EbeanConfig ebeanConfig,
            DBApi dbApi) {
        log.info("SERVICE>>> AbstractDatabaseDependencyServiceImpl starting...");
        init(configuration, ebeanConfig);
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> AbstractDatabaseDependencyServiceImpl stopping...");
            Ebean.getServer(EBEAN_SERVER_DEFAULT_NAME).shutdown(false, true);
            log.info("SERVICE>>> AbstractDatabaseDependencyServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> AbstractDatabaseDependencyServiceImpl started");
    }

    /**
     * Initialize the database (by running patches if required)
     */
    private void init(Configuration configuration, EbeanConfig ebeanConfig) {
        // Register the Ebean server as the default one
        ServerConfig serverConfig = ebeanConfig.serverConfigs().get(EBEAN_SERVER_DEFAULT_NAME);
        serverConfig.setBackgroundExecutorShutdownSecs(EBEAN_CACHE_SHUTDOWN_DELAY);
        serverConfig.setBackgroundExecutorCorePoolSize(EBEAN_CACHE_THREAD_POOL_SIZE);
        Ebean.register(EbeanServerFactory.create(serverConfig), true);

        // Unregister ebean from the Runtime (Shutdown hooks are bad !)
        BizDockEbeanShutdownManager.killThisBloodyShutdownHook();

        if (isPatcheable(configuration)) {
            log.info("Running the patch for the release " + getRelease());
            patch(log);
            updateRelease();
        } else {
            log.info("The application is up to date, no patch to run");
        }
    }

    /**
     * Return true if the application can be patched
     * 
     * @return
     */
    private boolean isPatcheable(Configuration configuration) {
        boolean isPatcheable = false;
        Connection connection = null;
        try {
            String driver = configuration.getString("db.default.driver");
            String url = configuration.getString("db.default.url");
            String username = configuration.getString("db.default.username");
            String password = configuration.getString("db.default.password");
            Class.forName(driver);
            connection = DriverManager.getConnection(url, username, password);
            String sql = "select apprelease from patch";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.first()) {
                String recordedRelease = rs.getString("apprelease");
                if (!getRelease().equals(recordedRelease)) {
                    isPatcheable = true;
                }
            } else {
                isPatcheable = true;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to read the patch release from the database", e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception e) {
            }
        }
        return isPatcheable;
    }

    /**
     * Update the current release after the patch has been run
     * 
     * @return
     */
    private void updateRelease() {
        List<Patch> patches = Patch.find.all();
        Patch patch = null;
        if (patches == null || patches.size() == 0) {
            patch = new Patch();
        } else {
            patch = patches.get(0);
        }
        patch.apprelease = getRelease();
        patch.runDate = new Date();
        patch.save();
    }

    /**
     * Return the release number
     * 
     * @return the new release to be updated into the database after the
     *         execution of the patch
     */
    public abstract String getRelease();

    /**
     * Perform a patching of the database
     * 
     * @param log
     */
    public abstract void patch(Logger.ALogger log);

}
