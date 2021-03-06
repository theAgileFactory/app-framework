package framework.services.api.doc;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.wordnik.swagger.converter.ModelConverters;

import framework.commons.IFrameworkConstants;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.api.ClassSchemaDocumentationConverter;
import framework.services.api.commons.ApiSignatureException;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;

@Singleton
public class ApiDocumentationServiceImpl implements IApiDocumentationService {
    private static Logger.ALogger log = Logger.of(ApiDocumentationServiceImpl.class);

    /**
     * Create a new ApiSignatureServiceImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param preferenceManagerPlugin
     *            the preference manager plugin
     * @throws ApiSignatureException
     */
    @Inject
    public ApiDocumentationServiceImpl(ApplicationLifecycle lifecycle, Configuration configuration, IPreferenceManagerPlugin preferenceManagerPlugin) {
        log.info("SERVICE>>> ApiDocumentationServiceImpl starting...");
        final String swaggerBasePath = preferenceManagerPlugin.getPreferenceElseConfigurationValue(IFrameworkConstants.SWAGGER_API_BASEPATH_PREFERENCE,
                "swagger.api.basepath");
        init(swaggerBasePath);
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> ApiDocumentationServiceImpl stopping...");
            log.info("SERVICE>>> ApiDocumentationServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> ApiDocumentationServiceImpl started");
    }

    private void init(String swaggerBasePath) {
        // Register the model converter for Swagger
        if (log.isDebugEnabled()) {
            log.debug("Registering the ModelConverter with swagger");
        }
        log.info("Setting the swagger base path to : " + swaggerBasePath);
        ModelConverters.addConverter(new ClassSchemaDocumentationConverter(), true);
    }
}
