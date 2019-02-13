package framework.utils;

import com.avaje.ebean.Expression;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.OrderBy;
import play.Logger;
import play.mvc.Http;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created by Guillaume Petit on 13.02.2019.
 */
public class PostQueryFilterConfig<T, U> extends FilterConfig<T> {

    private static Logger.ALogger log = Logger.of(PostQueryFilterConfig.class);

    private List<Predicate<U>> postQueryFilters = new ArrayList<>();

    private Comparator postQueryOrderBy;

    public List<Predicate<U>> getPostQueryFilters() {
        return postQueryFilters;
    }

    public Comparator getPostQueryOrderBy() {
        return postQueryOrderBy;
    }

    @Override
    public synchronized <K> ExpressionList<K> updateWithSearchExpression(ExpressionList<K> expression) {
        for (String columnId : getUserColumnConfigurations().keySet()) {
            UserColumnConfiguration userColumnConfiguration = getUserColumnConfigurations().get(columnId);
            SelectableColumn selectableColumn = getSelectableColumns().get(columnId);
            if (userColumnConfiguration.isFiltered()) {
                String fieldName = selectableColumn.getFieldName();
                IFilterComponent filterComponent = selectableColumn.getFilterComponent();
                Object filterValue = userColumnConfiguration.getFilterValue();
                if (filterComponent instanceof PostQueryFilterComponent) {
                    Predicate<U> predicate = ((PostQueryFilterComponent<U>) filterComponent).getPredicate(filterValue);
                    this.postQueryFilters.add(predicate);
                } else {
                    Expression temp = filterComponent.getEBeanSearchExpression(filterValue, fieldName);
                    if (temp != null) {
                        expression.add(temp);
                    }
                }
            }
        }
        return expression;

    }

    @Override
    public synchronized <K> void updateWithSortExpression(ExpressionList<K> expression) {
        OrderBy<K> orderby = expression.orderBy();
        for (String columnId : getUserColumnConfigurations().keySet()) {
            UserColumnConfiguration userColumnConfiguration = getUserColumnConfigurations().get(columnId);
            SelectableColumn selectableColumn = getSelectableColumns().get(columnId);
            String fieldName = selectableColumn.getFieldName();
            IFilterComponent filterComponent = selectableColumn.getFilterComponent();
            if (filterComponent instanceof PostQueryFilterComponent) {
                this.postQueryOrderBy = ((PostQueryFilterComponent) filterComponent).getComparator(userColumnConfiguration.getSortStatusType());
            } else {
                filterComponent.addEBeanSortExpression(orderby, userColumnConfiguration.getSortStatusType(), fieldName);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Orderby clause : " + orderby.toString());
        }

    }

    @Override
    public synchronized PostQueryFilterConfig<T, U> getCurrent(String principalUid, Http.Request request) {
        return (PostQueryFilterConfig<T, U>) super.getCurrent(principalUid, request);
    }

    @Override
    public synchronized PostQueryFilterConfig<T, U> persistCurrentInDefault(String principalUid, Http.Request request) {
        return (PostQueryFilterConfig<T, U>) super.persistCurrentInDefault(principalUid, request);
    }

    public static class PostQueryFilterComponent<K> extends NoDbFilterComponentWrapper {

        private PostQueryFilter<K> filter;
        private Comparator<K> comparator;

        public PostQueryFilterComponent(IFilterComponent filterComponent, PostQueryFilter<K> filter, Comparator<K> comparator) {
            super(filterComponent);
            this.filter = filter;
            this.comparator = comparator;
        }

        public Predicate<K> getPredicate(Object filterValue) {
            this.filter.filterValue = filterValue;
            return filter;
        }

        public Comparator<K> getComparator(SortStatusType sortStatusType) {
            switch (sortStatusType) {
                case ASC:
                    return comparator;
                case DESC:
                    return comparator.reversed();
                case NONE:
                case UNSORTED:
                default:
                    return null;
            }
        }
    }
}
