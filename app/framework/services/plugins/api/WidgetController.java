package framework.services.plugins.api;

import framework.services.configuration.II18nMessagesPlugin;
import framework.services.ext.ILinkGenerationService;
import framework.services.ext.api.AbstractExtensionController;
import framework.services.ext.api.WebCommandPath;
import framework.services.ext.api.WebParameter;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * The widget controller.
 * 
 * @author Pierre-Yves Cloux
 *
 */
public abstract class WidgetController extends AbstractExtensionController {
    private II18nMessagesPlugin i18nMessagePlugin;

    /**
     * Default constructor.
     * 
     * @param linkGenerationService
     *            the link generation service.
     */
    public WidgetController(ILinkGenerationService linkGenerationService, II18nMessagesPlugin i18nMessagePlugin) {
        super(linkGenerationService);
        this.i18nMessagePlugin = i18nMessagePlugin;
    }

    /**
     * The display action.
     * 
     * @param template
     *            the template
     * @param widgetId
     *            the widget id
     */
    @WebCommandPath(id = WebCommandPath.DEFAULT_COMMAND_ID, path = WebCommandPath.DEFAULT_COMMAND_PATH + "/:id")
    public Promise<Result> displayCommand(@WebParameter(name = "id") Long widgetId) {
        return display(widgetId);
    }

    /**
     * This method is called when the widget is in DISPLAY mode. <b>No need to
     * mark it with</b>:
     * 
     * <pre>
     * {@code
     * &#64;WebCommandPath
     * }
     * 
     * </pre>
     * 
     * @param widgetId
     *            the widget id which is referencing the configuration to be
     *            used by the controller
     * @return a result
     */
    public abstract Promise<Result> display(Long widgetId);

    /**
     * Display a widget error
     * 
     * @param widgetId
     * @param title
     *            the error widget title
     * @param message
     *            a message to be displayed in the widget
     * @return
     */
    public Result displayErrorWidget(Long widgetId, String title, String message) {
        return ok(views.html.framework_views.dashboard.error_widget.render(widgetId, title, message));
    }

    /**
     * Display a default widget error
     * 
     * @param widgetId
     * @return
     */
    public Result displayErrorWidget(Long widgetId) {
        return ok(views.html.framework_views.dashboard.error_widget.render(widgetId, getI18nMessagePlugin().get("unexpected.error.title"),
                getI18nMessagePlugin().get("unexpected.error.message")));
    }

    protected II18nMessagesPlugin getI18nMessagePlugin() {
        return i18nMessagePlugin;
    }
}
