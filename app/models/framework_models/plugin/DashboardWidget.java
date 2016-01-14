package models.framework_models.plugin;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import com.avaje.ebean.Model;

import models.framework_models.parent.IModelConstants;

/**
 * An object which is storing the configuration for a dashboard widget.
 * <ul>
 * <li>identifier : the unique identifier for the widget application</li>
 * <li>title : the title of the widget defined by the end user</li>
 * <li>color : a color type to be used for the widgets (see
 * {@link DashboardWidgetColor})</li>
 * <li>config : the configuration for the widget (serialized)</li>
 * <li>dashboardPage : the dashboard page to which the plugin is attached</li>
 * <li>pluginConfiguration : the plugin configuration to which the widget
 * instance is attached</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class DashboardWidget extends Model {
    /**
     * Default finder for the entity class
     */
    public static Finder<Long, DashboardWidget> find = new Finder<Long, DashboardWidget>(DashboardWidget.class);

    /**
     * Default constructor.
     */
    public DashboardWidget() {
    }

    @Id
    public Long id;

    @Column(length = IModelConstants.MEDIUM_STRING, nullable = false)
    public String identifier;

    @Column(length = IModelConstants.MEDIUM_STRING, nullable = false)
    public String title;

    @Column(length = IModelConstants.SMALL_STRING, nullable = false)
    public String color;

    @Lob
    public byte[] config;

    @ManyToOne(optional = false)
    public DashboardPage dashboardPage;

    @ManyToOne(optional = false)
    public PluginConfiguration pluginConfiguration;
}
