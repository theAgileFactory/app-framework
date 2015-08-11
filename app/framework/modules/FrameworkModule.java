package framework.modules;

import java.util.Date;

import play.Logger;
import play.data.format.Formatters;

import com.google.inject.AbstractModule;
import com.wordnik.swagger.converter.ModelConverters;

import framework.commons.DataType;
import framework.commons.IFrameworkConstants;
import framework.security.SecurityUtils;
import framework.services.ServiceStaticAccessor;
import framework.services.api.ClassSchemaDocumentationConverter;
import framework.services.ext.ExtensionUtils;
import framework.services.plugins.api.PluginUtils;
import framework.taftree.TafTreeHelper;
import framework.utils.FileAttachmentHelper;
import framework.utils.Msg;
import framework.utils.formats.AnnotationDateTypeFormatter;

/**
 * The module which holds the framework specific injections. This one can be
 * inherited by the application specific modules.
 * 
 * <b>IMPORTANT</b>: With play framework, Guice is used as a dependency
 * injection framework. However some classes must still be statically injected.
 * This is done using the class.<br/>
 * This is a transitory solution.
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class FrameworkModule extends AbstractModule {
    private static Logger.ALogger log = Logger.of(FrameworkModule.class);

    public FrameworkModule() {
    }

    /**
     * The initializations which are supposed to take place before anythingelse.
     */
    protected void beforeInjection() {
        // Register the data types
        DataType.add(IFrameworkConstants.User, "framework.services.account.IUserAccount", false, false);
        DataType.add(IFrameworkConstants.SystemLevelRoleType, "models.framework_models.account.SystemLevelRoleType", false, false);
        // Register the play framework date formatter
        Formatters.register(Date.class, new AnnotationDateTypeFormatter());
        // Register the model converter for Swagger
        ModelConverters.addConverter(new ClassSchemaDocumentationConverter(), true);
    }

    @Override
    protected void configure() {
        log.info(">>> Static dependency injected start...");
        requestStaticInjection(ServiceStaticAccessor.class);
        requestStaticInjection(Msg.class);
        requestStaticInjection(SecurityUtils.class);
        requestStaticInjection(ExtensionUtils.class);
        requestStaticInjection(PluginUtils.class);
        requestStaticInjection(TafTreeHelper.class);
        requestStaticInjection(FileAttachmentHelper.class);
        log.info("...Static dependency injected end");
    }
}
