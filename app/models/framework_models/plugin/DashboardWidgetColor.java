package models.framework_models.plugin;

/**
 * The color codes to be used by the widgets
 * @author Pierre-Yves Cloux
 */
public enum DashboardWidgetColor {
    DEFAULT("default"),
    PRIMARY("primary"),
    SUCCESS("success"),
    INFO("info"),
    WARNING("warning"),
    DANGER("danger");
    
    private String color;
    
    private DashboardWidgetColor(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
}
