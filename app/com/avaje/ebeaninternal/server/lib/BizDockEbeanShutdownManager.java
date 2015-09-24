package com.avaje.ebeaninternal.server.lib;

import play.Logger;

/**
 * This class is to remove the Ebean shutdown hook which prevent a complete
 * management of Ebean according to dependency injection principles.
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class BizDockEbeanShutdownManager {
    private static Logger.ALogger log = Logger.of(BizDockEbeanShutdownManager.class);

    public BizDockEbeanShutdownManager() {
    }

    /**
     * Remove the Ebean shutdown hook
     */
    public static void killThisBloodyShutdownHook() {
        log.info("Removing the Ebean shutdown hook");
        if (Runtime.getRuntime().removeShutdownHook(ShutdownManager.shutdownHook)) {
            log.info("Ebean shutdown hook successfully removed");
        }
    }
}
