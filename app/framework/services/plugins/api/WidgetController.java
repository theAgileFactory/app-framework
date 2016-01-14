package framework.services.plugins.api;

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
    /**
     * Default command for Edit mode
     */
    public static final String EDIT_COMMAND_ID="_edit";
    /**
     * Default path for the command EDIT_COMMAND_ID
     */
    public static final String EDIT_COMMAND_PATH="/edit";
    
    /**
     * Default constructor.
     * 
     * @param linkGenerationService
     *            the link generation service.
     */
    public WidgetController(ILinkGenerationService linkGenerationService) {
        super(linkGenerationService);
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
     * The display action.
     * 
     * @param template
     *            the template
     * @param widgetId
     *            the widget id
     */
    @WebCommandPath(id = WidgetController.EDIT_COMMAND_ID, path = WidgetController.EDIT_COMMAND_PATH + "/:id")
    public Promise<Result> editCommand(@WebParameter(name = "id") Long widgetId) {
        return edit(widgetId);
    }

    /**
     * This method is called when the widget is in DISPLAY mode.
     * <b>No need to mark it with</b>:
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
     * This method is called when the widget is in EDIT mode.
     * <b>No need to mark it with</b>:
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
    public abstract Promise<Result> edit(Long widgetId);
}
