package framework.utils;

import com.avaje.ebean.Expression;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.OrderBy;
import com.fasterxml.jackson.databind.JsonNode;
import models.framework_models.common.FilterConfiguration;
import play.Logger;
import play.mvc.Http;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by Guillaume Petit on 13.02.2019.
 */
public class PostQueryFilterConfig<T, U> extends FilterConfig<T> {

    private static Logger.ALogger log = Logger.of(PostQueryFilterConfig.class);

    private List<Predicate<U>> postQueryFilters = new ArrayList<>();

    private Comparator postQueryOrderBy;

    public PostQueryFilterConfig() {
        super();
    }

    public PostQueryFilterConfig(PostQueryFilterConfig<T, U> template, FilterConfiguration selectedFilterConfiguration, boolean deepCopy) {
        super(template, selectedFilterConfiguration, deepCopy);
    }

    public List<Predicate<U>> getPostQueryFilters() {
        return postQueryFilters;
    }

    public Comparator getPostQueryOrderBy() {
        return postQueryOrderBy;
    }

    public int getSize(ExpressionList<U> expressionList) {
        List<U> list = expressionList.findList();
        int size;
        if (!this.getPostQueryFilters().isEmpty()) {
            Stream<U> stream = list.stream();
            for (Predicate<U> filter : this.getPostQueryFilters()) {
                stream = stream.filter(filter);
            }
            size = (int) stream.count();
        } else {
            size = list.size();
        }
        return size;
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
                Comparator comparator = ((PostQueryFilterComponent) filterComponent).getComparator(userColumnConfiguration.getSortStatusType());
                if (comparator != null) {
                    this.postQueryOrderBy = comparator;
                }
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

    @Override
    protected synchronized PostQueryFilterConfig<T, U> parseResponse(JsonNode json, FilterConfiguration selectedFilterConfiguration) throws FilterConfigException {
        PostQueryFilterConfig<T, U> temp = new PostQueryFilterConfig<>(this, selectedFilterConfiguration, false);
        temp.unmarshall(json);
        return temp;

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
