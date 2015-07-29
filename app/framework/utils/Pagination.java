/*! LICENSE
 *
 * Copyright (c) 2015, The Agile Factory SA and/or its affiliates. All rights
 * reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package framework.utils;

import java.util.List;

import play.Play;

import com.avaje.ebean.ExpressionList;

import framework.commons.IFrameworkConstants;
import framework.services.ServiceManager;
import framework.services.account.IPreferenceManagerPlugin;

/**
 * Utility class which deals with pagination management.<br/>
 * This class is to be used with the {@link views.html.parts.pagination} scala
 * template.<br/>
 * The pagination can work with a query {@link ExpressionList} or with a row
 * count provided to the constructor. In such case, the management of the
 * selection of objects to be provided to the Table (for listing) is under the
 * responsibility of the developer.
 * <ul>
 * <li>The default "page size" is taken from the configuration property
 * "maf.list_page_size"</li>
 * <li>The default number of links in the pagination navigation bar is taken
 * from the configuration property "maf.number_page_links"</li>
 * </ul>
 * <br/>
 * By default the query name for the current page attribute is simply "page"
 * (i.e. ?page=X). If necessary (for example if a render displays 2 tables),
 * it's possible to custom this attribute's name. To do this use the parameter
 * "pageQueryName".
 * 
 * @param <T>
 *            the object from the {@link Table} which is paginated
 * @author Pierre-Yves Cloux
 */
public class Pagination<T> {
    private ExpressionList<T> expressionList;
    private Integer currentPage;
    private int pageSize;
    private int numberOfPages;
    private int rowCount;
    private int numberOfLinksInNavigationBar;
    private int lowerBound;
    private int upperBound;
    private boolean hasLess = true;
    private boolean hasMore = true;
    private String pageQueryName = "page";

    /**
     * Creates a Pagination object using the specified ExpressionList.<br/>
     * 
     * @param expressionList
     *            an Expression list
     */
    public Pagination(ExpressionList<T> expressionList) {
        this(expressionList, ServiceManager.getService(IPreferenceManagerPlugin.NAME, IPreferenceManagerPlugin.class).getPreferenceValueAsInteger(
                IFrameworkConstants.DISPLAY_LIST_PAGE_SIZE_PREFERENCE), Play.application().configuration().getInt("maf.number_page_links"));
    }

    /**
     * Creates a Pagination object using the specified ExpressionList but by
     * specifying the number of rows
     * 
     * @param rowCount
     *            the total number of records
     * @param expressionList
     *            an Expression list
     */
    public Pagination(int rowCount, ExpressionList<T> expressionList) {
        this(rowCount, ServiceManager.getService(IPreferenceManagerPlugin.NAME, IPreferenceManagerPlugin.class).getPreferenceValueAsInteger(
                IFrameworkConstants.DISPLAY_LIST_PAGE_SIZE_PREFERENCE), Play.application().configuration().getInt("maf.number_page_links"));
        this.expressionList = expressionList;
    }

    /**
     * Creates a Pagination object using the specified number of row (rowCount)
     * 
     * @param rowCount
     *            the total number of records
     */
    public Pagination(int rowCount) {
        this(rowCount, ServiceManager.getService(IPreferenceManagerPlugin.NAME, IPreferenceManagerPlugin.class).getPreferenceValueAsInteger(
                IFrameworkConstants.DISPLAY_LIST_PAGE_SIZE_PREFERENCE), Play.application().configuration().getInt("maf.number_page_links"));
    }

    /**
     * Creates a Pagination object using the specified {@link ExpressionList}
     * and a specific page size.
     * 
     * @param expressionList
     * @param pageSize
     *            the number of records in one page
     * @param numberOfLinksInNavigationBar
     *            the number of links (possible pages to navigate to) displayed
     *            in the navigation bar
     */
    public Pagination(ExpressionList<T> expressionList, int pageSize, int numberOfLinksInNavigationBar) {
        this.pageSize = pageSize;
        this.expressionList = expressionList;
        this.numberOfLinksInNavigationBar = numberOfLinksInNavigationBar;
        this.rowCount = expressionList.findRowCount();
        computeNumberOfPages(pageSize);
    }

    /**
     * Creates a Pagination object using the specified number of row (rowCount)
     * and a specific page size.
     * 
     * @param rowCount
     *            the total number of records
     * @param pageSize
     *            the number of records in one page
     * @param numberOfLinksInNavigationBar
     *            the number of links (possible pages to navigate to) displayed
     *            in the navigation bar
     */
    public Pagination(int rowCount, int pageSize, int numberOfLinksInNavigationBar) {
        this.pageSize = pageSize;
        this.rowCount = rowCount;
        this.numberOfLinksInNavigationBar = numberOfLinksInNavigationBar;
        computeNumberOfPages(pageSize);
    }

    /**
     * Compute the number of pages for the pagination
     * 
     * @param pageSize
     *            the page size
     */
    private void computeNumberOfPages(int pageSize) {
        this.numberOfPages = this.rowCount / pageSize;
        if ((this.rowCount % pageSize) != 0) {
            this.numberOfPages = this.numberOfPages + 1;
        }
        /*
         * if (this.numberOfPages > 0 && (this.rowCount % pageSize) == 0) {
         * this.numberOfPages = this.numberOfPages - 1; }
         */
    }

    /**
     * Set the current page for this navigation
     * 
     * @param currentPage
     *            the current page
     */
    public void setCurrentPage(Integer currentPage) {
        if (currentPage < 0)
            currentPage = 0;
        if (currentPage >= getNumberOfPages()) {
            currentPage = 0;
        }
        int alowerBound = currentPage - (getNumberOfLinksInNavigationBar() / 2);
        int aupperBound = currentPage + (getNumberOfLinksInNavigationBar() / 2);

        // Compute lower bound
        if (aupperBound >= getNumberOfPages()) {
            this.lowerBound = alowerBound - (aupperBound - getNumberOfPages());
        } else {
            this.lowerBound = alowerBound;
        }
        if (this.lowerBound <= 0) {
            this.lowerBound = 0;
            this.hasLess = false;
        }

        // Compute upper bound
        if (alowerBound <= 0) {
            this.upperBound = aupperBound - alowerBound;
        } else {
            this.upperBound = aupperBound;
        }
        if (this.upperBound >= getNumberOfPages()) {
            this.upperBound = getNumberOfPages() - 1;
            this.hasMore = false;
        }

        this.currentPage = currentPage;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    /**
     * If the Pagination object is based on an {@link ExpressionList} returns
     * the list of objects matching the current page.<br/>
     * Throws a {@link RuntimeException} otherwise.
     * 
     * @return a list of objects
     */
    public List<T> getListOfObjects() {
        if (getExpressionList() == null || getCurrentPage() == null) {
            throw new IllegalStateException("Invalid use of Pagination, no ExpressionList provided in the constructor or no current page specified");
        }
        if (getNumberOfPages() == 0) {
            return getExpressionList().findList();
        }
        return getExpressionList().setFirstRow(getCurrentPage() * getPageSize()).setMaxRows(getPageSize()).findList();
    }

    /**
     * if the Pagination object is based on a list of entries then we return the
     * sub list for the current page
     */
    public List<T> getEntriesForCurrentPage(List<T> entries) {
        int lower = getCurrentPage() * getPageSize();
        int upper = (lower + getPageSize()) > rowCount ? rowCount : (lower + getPageSize());
        if (lower <= upper) {
            return entries.subList(lower, upper);
        }
        return entries;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getNumberOfLinksInNavigationBar() {
        return numberOfLinksInNavigationBar;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    public boolean hasLess() {
        return hasLess;
    }

    public boolean hasMore() {
        return hasMore;
    }

    private ExpressionList<T> getExpressionList() {
        return expressionList;
    }

    @Override
    public String toString() {
        return "Pagination [expressionList=" + expressionList + ", currentPage=" + currentPage + ", pageSize=" + pageSize + ", numberOfPages="
                + numberOfPages + ", rowCount=" + rowCount + ", numberOfLinksInNavigationBar=" + numberOfLinksInNavigationBar + ", lowerBound=" + lowerBound
                + ", upperBound=" + upperBound + ", hasLess=" + hasLess + ", hasMore=" + hasMore + "]";
    }

    /**
     * get the current page attribute query name
     */
    public String getPageQueryName() {
        return pageQueryName;
    }

    /**
     * set the current page attribute query name
     */
    public void setPageQueryName(String pageQueryName) {
        this.pageQueryName = pageQueryName;
    }
}
