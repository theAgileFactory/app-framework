package models.framework_models.plugin;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.avaje.ebean.Model;

import models.framework_models.account.Principal;
import models.framework_models.parent.IModelConstants;

/**
 * An object which is storing the configuration for a dashboard page.<br/>
 * A dashboard page has:
 * <ul>
 * <li>isHome is the dashboard page which is used as the user "home page" (it is
 * the default page displayed)</li>
 * <li>a name which is displayed to the end user (as the title of the page)</li>
 * <li>a layout configuration (serialized JSON String) which describes how the
 * dashboard page is made</li>
 * </ul>
 * Here is a sample configuration for a page:
 * 
 * <pre>
 * [{layout: "TPL12COL_1", widgets: [1]},{layout: "TPL66COL_2", widgets: [2,-1]},{layout: "TPL444COL_3", widgets: [3,-1,-1]}]
 * </pre>
 * <ul>
 * <li>layout : this is a {@link DashboardRowTemplate} which indicates how the
 * widget areas are distributed on the page.</li>
 * <li>widgets : -1 is used to identify an empty widget cell, otherwise the
 * value provided is the id of the widget.</li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class DashboardPage extends Model {
    /**
     * Default finder for the entity class
     */
    public static Finder<Long, DashboardPage> find = new Finder<Long, DashboardPage>(DashboardPage.class);

    @Id
    public Long id;

    public boolean isHome;

    @Column(length = IModelConstants.SMALL_STRING, nullable = false)
    public String name;

    @Lob
    public byte[] layout;

    @ManyToOne(cascade = CascadeType.ALL, optional = false)
    public Principal principal;

    @OneToMany(mappedBy = "dashboardPage", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<DashboardWidget> dashboardWidgets;

    /**
     * Default constructor.
     */
    public DashboardPage() {
    }
}
