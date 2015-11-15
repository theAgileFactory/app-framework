package framework.services.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private int ebeanCacheThreadPoolSize;
    private int ebeanCacheShutdownDelay;
    private String ebeanServerDefaultName;
    private int ebeanListenerQueueSize;
    private EbeanConfig ebeanConfig;
    private Map<IDatabaseChangeListener, ExecutorService> listeners;

    public enum Config {
        EBEAN_CACHE_THREAD_POOL_SIZE("maf.ebean.cache.thread.pool.size"), EBEAN_CACHE_SHUTDOWN_DELAY(
                "maf.ebean.cache.shutdown.delay"), EBEAN_SERVER_DEFAULT_NAME("maf.ebean.default.server.name"), EBEAN_LISTENER_QUEUE_SIZE(
                        "maf.ebean.change.listener.queue.size");

        private String configurationKey;

        private Config(String configurationKey) {
            this.configurationKey = configurationKey;
        }

        public String getConfigurationKey() {
            return configurationKey;
        }
    }

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
        this.ebeanConfig = ebeanConfig;

        this.ebeanCacheThreadPoolSize = configuration.getInt(Config.EBEAN_CACHE_THREAD_POOL_SIZE.getConfigurationKey());
        this.ebeanCacheShutdownDelay = configuration.getInt(Config.EBEAN_CACHE_SHUTDOWN_DELAY.getConfigurationKey());
        this.ebeanServerDefaultName = configuration.getString(Config.EBEAN_SERVER_DEFAULT_NAME.getConfigurationKey());
        this.ebeanListenerQueueSize = configuration.getInt(Config.EBEAN_LISTENER_QUEUE_SIZE.getConfigurationKey());
        if (log.isDebugEnabled()) {
            log.debug(Config.EBEAN_CACHE_THREAD_POOL_SIZE.getConfigurationKey() + "=" + this.ebeanCacheThreadPoolSize);
            log.debug(Config.EBEAN_CACHE_SHUTDOWN_DELAY.getConfigurationKey() + "=" + this.ebeanCacheShutdownDelay);
            log.debug(Config.EBEAN_SERVER_DEFAULT_NAME.getConfigurationKey() + "=" + this.ebeanServerDefaultName);
            log.debug(Config.EBEAN_LISTENER_QUEUE_SIZE.getConfigurationKey() + "=" + this.ebeanListenerQueueSize);
        }

        log.info("SERVICE>>> AbstractDatabaseDependencyServiceImpl starting...");
        init(configuration);
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> AbstractDatabaseDependencyServiceImpl stopping...");
            destroy();
            log.info("SERVICE>>> AbstractDatabaseDependencyServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> AbstractDatabaseDependencyServiceImpl started");
    }

    @Override
    public void addDatabaseChangeListener(IDatabaseChangeListener listener) {
        if (log.isDebugEnabled()) {
            log.debug("Adding a new listener " + listener);
        }
        // If a listener is already registered, do nothing
        if (!getListeners().containsKey(listener)) {
            BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>(getEbeanListenerQueueSize());
            ExecutorService executorService = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, blockingQueue, new ThreadPoolExecutor.DiscardPolicy());
            listeners.put(listener, executorService);
        }
        log.info("Added listener for database events " + listener);
    }

    @Override
    public void removeDatabaseChangeListener(IDatabaseChangeListener listener) {
        if (log.isDebugEnabled()) {
            log.debug("Removing a new listener " + listener);
        }
        ExecutorService executorService = getListeners().remove(listener);
        if (executorService != null) {
            try {
                executorService.shutdown();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Error while shutting down the executor service", e);
                }
            }
        }
        log.info("Removed listener for database events " + listener);
    }

    /**
     * Initialize the database (by running patches if required)
     */
    private void init(Configuration configuration) {
        // Creates the listeners map
        listeners = Collections.synchronizedMap(new HashMap<>());

        // Register the Ebean server as the default one
        ServerConfig serverConfig = getEbeanConfig().serverConfigs().get(getEbeanServerDefaultName());
        serverConfig.setBackgroundExecutorShutdownSecs(getEbeanCacheShutdownDelay());
        serverConfig.setBackgroundExecutorCorePoolSize(getEbeanCacheThreadPoolSize());
        serverConfig.add(new CustomBeanPersistController(listeners));
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
     * Stop the Ebean server
     */
    private void destroy() {
        Ebean.getServer(getEbeanServerDefaultName()).shutdown(false, true);
        listeners.clear();
        this.ebeanConfig = null;
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

    private EbeanConfig getEbeanConfig() {
        return ebeanConfig;
    }

    private int getEbeanCacheThreadPoolSize() {
        return ebeanCacheThreadPoolSize;
    }

    private int getEbeanCacheShutdownDelay() {
        return ebeanCacheShutdownDelay;
    }

    private String getEbeanServerDefaultName() {
        return ebeanServerDefaultName;
    }

    private int getEbeanListenerQueueSize() {
        return ebeanListenerQueueSize;
    }

    private Map<IDatabaseChangeListener, ExecutorService> getListeners() {
        return listeners;
    }

}
