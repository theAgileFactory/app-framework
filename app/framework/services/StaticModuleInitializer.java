package framework.services;

import play.Logger;

import com.google.inject.AbstractModule;

import framework.security.SecurityUtils;

/**
 * With play framework, Guice is used as a dependency injection framework.
 * However some classes must still be statically injected. This is done using
 * the class.<br/>
 * This is a transitory solution.
 * 
 * @author Pierre-Yves Cloux
 */
public class StaticModuleInitializer extends AbstractModule {
    private static Logger.ALogger log = Logger.of(StaticModuleInitializer.class);

    public StaticModuleInitializer() {
    }

    @Override
    protected void configure() {
        log.info(">>> Static dependency injected start...");
        requestStaticInjection(SecurityUtils.class);
        log.info("...Static dependency injected end");
    }
}
