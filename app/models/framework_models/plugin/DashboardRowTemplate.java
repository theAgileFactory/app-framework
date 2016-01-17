package models.framework_models.plugin;

import org.apache.commons.lang.StringUtils;

/**
 * An enumeration which is listing all the possible row templates.<br/>
 * A page is made of the combination of multiple rows.<br/>
 * Each row template is associated with a layout which consist in a comma
 * separated list of integers. Each integer is a number matching the Bootstrap
 * grid system.
 * 
 * @author Pierre-Yves Cloux
 */
public enum DashboardRowTemplate {
    TPL12COL_1("12"), TPL66COL_2("6,6"), TPL444COL_3("4,4,4"), TPL48_OL_2("4,8"), TPL84COL_2("8,4");

    private String layout;

    /**
     * Get the layout.
     */
    public String getLayout() {
        return layout;
    }

    /**
     * List of the columns sizes as an array
     */
    public String[] getColumnsSizes() {
        return StringUtils.split(this.getLayout(), ',');
    }

    /**
     * Construct a row template by specifying the layout.
     * 
     * @param layout
     *            the layout of the row template
     */
    private DashboardRowTemplate(String layout) {
        this.layout = layout;
    }
}