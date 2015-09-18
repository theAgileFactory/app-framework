package framework.services.database;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebeaninternal.server.lib.ShutdownManager;
import com.mysql.jdbc.AbandonedConnectionCleanupThread;

import play.Configuration;
import play.Environment;
import play.Logger;
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
public abstract class DatabaseDependencyServiceImpl implements IDatabaseDependencyService {
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
     */
    @Inject
    public DatabaseDependencyServiceImpl(ApplicationLifecycle lifecycle, Environment environment, Configuration configuration, EbeanConfig ebeanConfig) {
        log.info("SERVICE>>> AbstractDatabaseDependencyServiceImpl starting...");
        // Register the Ebean server as the default one
        Ebean.register(EbeanServerFactory.create(ebeanConfig.serverConfigs().get(EBEAN_SERVER_DEFAULT_NAME)), true);
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> AbstractDatabaseDependencyServiceImpl stopping...");
            if (environment.isDev()) {
                shutdownDbResources();
            }
            log.info("SERVICE>>> AbstractDatabaseDependencyServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> AbstractDatabaseDependencyServiceImpl started");
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

    /**
     * Perform a patching of the database if required
     * 
     * @param log
     */
    public abstract void patch(Logger.ALogger log);
}
