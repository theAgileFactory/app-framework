package framework.handlers;

import framework.services.account.IAccountManagerPlugin;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.audit.IAuditLoggerService;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.configuration.ITopMenuBarService;
import framework.services.custom_attribute.ICustomAttributeManagerService;
import framework.services.kpi.IKpiService;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import play.Configuration;
import play.cache.CacheApi;
import play.mvc.Http.Context;

/**
 * This utility class is used to "inject" some standard services into the
 * Context.args map.<br/>
 * This feature is used by the {@link AbstractRequestHandler} and the
 * {@link AbstractErrorHandler} in their injectCommonServicesIncontext method.
 * <br/>
 * At a later stage the ViewInjector scala trait is used to "inject" the
 * services in the views.
 */
abstract class ContextArgsInjector {
    /**
     * This method injects the commonly used services in the {@link Context}.
     * These common services can then be access from the "args" variable of the
     * current context in the scala templates.
     *
     * @param context
     * @param configuration
     * @param messagesPlugin
     * @param userSessionManagerPlugin
     * @param attachmentManagerPlugin
     * @param accountManagerPlugin
     * @param cacheApi
     * @param kpiService
     * @param implementationDefinedObjectService
     * @param topMenuBarService
     * @param auditLoggerService
     */
    public static void injectCommonServicesIncontext(Context context,
                                                     Configuration configuration,
                                                     II18nMessagesPlugin messagesPlugin,
                                                     IUserSessionManagerPlugin userSessionManagerPlugin,
                                                     IAttachmentManagerPlugin attachmentManagerPlugin,
                                                     IAccountManagerPlugin accountManagerPlugin,
                                                     CacheApi cacheApi,
                                                     IKpiService kpiService,
                                                     IImplementationDefinedObjectService implementationDefinedObjectService,
                                                     ITopMenuBarService topMenuBarService,
                                                     IPreferenceManagerPlugin preferenceManagerPlugin,
                                                     ICustomAttributeManagerService customAttributeManagerService,
                                                     IAuditLoggerService auditLoggerService) {
        if (context == null) {
            return;
        }
        if (configuration != null) {
            context.args.put(Configuration.class.getName(), configuration);
        }
        if (messagesPlugin != null) {
            context.args.put(II18nMessagesPlugin.class.getName(), messagesPlugin);
        }
        if (userSessionManagerPlugin != null) {
            context.args.put(IUserSessionManagerPlugin.class.getName(), userSessionManagerPlugin);
        }
        if (attachmentManagerPlugin != null) {
            context.args.put(IAttachmentManagerPlugin.class.getName(), attachmentManagerPlugin);
        }
        if (accountManagerPlugin != null) {
            context.args.put(IAccountManagerPlugin.class.getName(), accountManagerPlugin);
        }
        if (cacheApi != null) {
            context.args.put(CacheApi.class.getName(), cacheApi);
        }
        if (kpiService != null) {
            context.args.put(IKpiService.class.getName(), kpiService);
        }
        if (implementationDefinedObjectService != null) {
            context.args.put(IImplementationDefinedObjectService.class.getName(), implementationDefinedObjectService);
        }
        if (topMenuBarService != null) {
            context.args.put(ITopMenuBarService.class.getName(), topMenuBarService);
        }
        if (preferenceManagerPlugin != null) {
            context.args.put(IPreferenceManagerPlugin.class.getName(), preferenceManagerPlugin);
        }
        if (customAttributeManagerService != null) {
            context.args.put(ICustomAttributeManagerService.class.getName(), customAttributeManagerService);
        }
        if (auditLoggerService != null) {
            context.args.put(IAuditLoggerService.class.getName(), auditLoggerService);
        }
    }
}
