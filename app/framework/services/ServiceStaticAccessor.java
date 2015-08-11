package framework.services;

import javax.inject.Inject;
import javax.inject.Singleton;

import play.Configuration;
import play.cache.CacheApi;
import framework.services.account.IAccountManagerPlugin;
import framework.services.account.IAuthenticationAccountReaderPlugin;
import framework.services.account.IAuthenticationAccountWriterPlugin;
import framework.services.account.IPreferenceManagerPlugin;
import framework.services.audit.IAuditLoggerService;
import framework.services.configuration.II18nMessagesPlugin;
import framework.services.configuration.IImplementationDefinedObjectService;
import framework.services.database.IDatabaseDependencyService;
import framework.services.ext.IExtensionManagerService;
import framework.services.kpi.IKpiService;
import framework.services.notification.INotificationManagerPlugin;
import framework.services.plugins.IPluginManagerService;
import framework.services.remote.IAdPanelManagerService;
import framework.services.router.ICustomRouterNotificationService;
import framework.services.router.ICustomRouterService;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import framework.services.storage.IPersonalStoragePlugin;
import framework.services.storage.ISharedStorageService;
import framework.services.system.ISysAdminUtils;

@Singleton
public class ServiceStaticAccessor {
    @Inject
    private static IUserSessionManagerPlugin userSessionManagerPlugin;
    @Inject
    private static IKpiService kpiService;
    @Inject
    private static IAuthenticationAccountWriterPlugin authenticationAccountWriterPlugin;
    @Inject
    private static IAuthenticationAccountReaderPlugin authenticationAccountReaderPlugin;
    @Inject
    private static IAccountManagerPlugin accountManagerPlugin;
    @Inject
    private static II18nMessagesPlugin messagesPlugin;
    @Inject
    private static IPreferenceManagerPlugin preferenceManagerPlugin;
    @Inject
    private static IPluginManagerService pluginManagerService;
    @Inject
    private static IExtensionManagerService extensionmanagerService;
    @Inject
    private static IPersonalStoragePlugin personalStoragePlugin;
    @Inject
    private static ISharedStorageService sharedStorageService;
    @Inject
    private static INotificationManagerPlugin notificationManagerPlugin;
    @Inject
    private static IAttachmentManagerPlugin attachmentManagerPlugin;
    @Inject
    private static IAdPanelManagerService adPanelManagerService;
    @Inject
    private static ISysAdminUtils sysAdminUtils;
    @Inject
    private static IImplementationDefinedObjectService implementationDefinedObjectService;
    @Inject
    private static ICustomRouterService customRouterService;
    @Inject
    private static ICustomRouterNotificationService customRouterNotificationService;
    @Inject
    private static IAuditLoggerService auditLoggerService;
    @Inject
    private static IDatabaseDependencyService databaseDependencyService;
    @Inject
    private static Configuration configuration;
    @Inject
    private static CacheApi cacheApi;

    public ServiceStaticAccessor() {
    }

    public static IUserSessionManagerPlugin getUserSessionManagerPlugin() {
        if (userSessionManagerPlugin == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return userSessionManagerPlugin;
    }

    public static IKpiService getKpiService() {
        if (kpiService == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return kpiService;
    }

    public static IAccountManagerPlugin getAccountManagerPlugin() {
        if (accountManagerPlugin == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return accountManagerPlugin;
    }

    public static II18nMessagesPlugin getMessagesPlugin() {
        if (messagesPlugin == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return messagesPlugin;
    }

    public static IPreferenceManagerPlugin getPreferenceManagerPlugin() {
        if (preferenceManagerPlugin == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return preferenceManagerPlugin;
    }

    public static IPluginManagerService getPluginManagerService() {
        if (pluginManagerService == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return pluginManagerService;
    }

    public static IExtensionManagerService getExtensionmanagerService() {
        if (extensionmanagerService == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return extensionmanagerService;
    }

    public static IPersonalStoragePlugin getPersonalStoragePlugin() {
        if (personalStoragePlugin == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return personalStoragePlugin;
    }

    public static ISharedStorageService getSharedStorageService() {
        if (sharedStorageService == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return sharedStorageService;
    }

    public static INotificationManagerPlugin getNotificationManagerPlugin() {
        if (notificationManagerPlugin == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return notificationManagerPlugin;
    }

    public static IAttachmentManagerPlugin getAttachmentManagerPlugin() {
        if (attachmentManagerPlugin == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return attachmentManagerPlugin;
    }

    public static IAdPanelManagerService getAdPanelManagerService() {
        if (adPanelManagerService == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return adPanelManagerService;
    }

    public static ISysAdminUtils getSysAdminUtils() {
        if (sysAdminUtils == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return sysAdminUtils;
    }

    public static IImplementationDefinedObjectService getImplementationDefinedObjectService() {
        if (implementationDefinedObjectService == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return implementationDefinedObjectService;
    }

    public static ICustomRouterService getCustomRouterService() {
        if (customRouterService == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return customRouterService;
    }

    public static ICustomRouterNotificationService getCustomRouterNotificationService() {
        if (customRouterNotificationService == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return customRouterNotificationService;
    }

    public static IAuditLoggerService getAuditLoggerService() {
        if (auditLoggerService == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return auditLoggerService;
    }

    public static IDatabaseDependencyService getDatabaseDependencyService() {
        if (databaseDependencyService == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return databaseDependencyService;
    }

    public static Configuration getConfiguration() {
        if (databaseDependencyService == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return configuration;
    }

    public static CacheApi getCacheApi() {
        if (cacheApi == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return cacheApi;
    }

    public static IAuthenticationAccountWriterPlugin getAuthenticationAccountWriterPlugin() {
        if (authenticationAccountWriterPlugin == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return authenticationAccountWriterPlugin;
    }

    public static IAuthenticationAccountReaderPlugin getAuthenticationAccountReaderPlugin() {
        if (authenticationAccountReaderPlugin == null) {
            throw new IllegalArgumentException("Service is not injected yet");
        }
        return authenticationAccountReaderPlugin;
    }
}
