package framework.utils;

import java.util.function.Predicate;

/**
 * An abstract PostQueryFilter that implements a Predicate
 *
 * Created by Guillaume Petit<guillaume.petit@sword-group.com> on 07.02.2019.
 */
public abstract class PostQueryFilter<K> implements Predicate<K> {

    protected Object filterValue;
}
