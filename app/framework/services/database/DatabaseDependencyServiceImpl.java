package framework.services.database;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import javax.inject.Inject;
import javax.inject.Singleton;

import play.Configuration;
import play.Logger;
import play.db.ebean.EbeanConfig;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebeaninternal.server.lib.ShutdownManager;
import com.mysql.jdbc.AbandonedConnectionCleanupThread;

/**
 * This service is to ensure that Ebean is loaded before a service depending on
 * the database requires an access to some data
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class DatabaseDependencyServiceImpl implements IDatabaseDependencyService {
    public static final String EBEAN_SERVER_DEFAULT_NAME = "default";
    private static Logger.ALogger log = Logger.of(DatabaseDependencyServiceImpl.class);

    /**
     * Create a new DatabaseDependencyServiceImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param ebeanConfig
     *            the Ebean configuration as loaded by the play plugin
     */
    @Inject
    public DatabaseDependencyServiceImpl(ApplicationLifecycle lifecycle, Configuration configuration, EbeanConfig ebeanConfig) {
        log.info("SERVICE>>> DatabaseDependencyServiceImpl starting...");
        // Register the Ebean server as the default one
        Ebean.register(EbeanServerFactory.create(ebeanConfig.serverConfigs().get(EBEAN_SERVER_DEFAULT_NAME)), true);
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> DatabaseDependencyServiceImpl stopping...");
            shutdownDbResources();
            log.info("SERVICE>>> DatabaseDependencyServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> DatabaseDependencyServiceImpl started");
    }

    /**
     * Shutdown database connections.
     */
    @SuppressWarnings("deprecation")
    private void shutdownDbResources() {
        log.info(">>>>>>>>>>>>>>>> Shutting down the database resources...");
        try {
            ShutdownManager.shutdown();
            // Unregister the JDBC drivers
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                DriverManager.deregisterDriver(driver);
            }
            // Kill the JDBC cleanup thread
            AbandonedConnectionCleanupThread.shutdown();
            // Kill the remaining Timer threads
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getName().startsWith("Timer-")) {
                    t.stop();
                }
            }
        } catch (Exception e) {
            log.debug("Exception while shutting down the database connections", e);
        }
        log.info(">>>>>>>>>>>>>>>> database resources closed");
    }
}
