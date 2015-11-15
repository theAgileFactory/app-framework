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
package framework.services.database;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.avaje.ebean.event.BeanPersistController;
import com.avaje.ebean.event.BeanPersistRequest;

/**
 * Class which acts as a listener of the Ebean server.<br/>
 * Its methods are called when an object is modified (updated, deleted or
 * created).
 * 
 * @author Pierre-Yves Cloux
 */
public class CustomBeanPersistController implements BeanPersistController {
    private Map<IDatabaseChangeListener, ExecutorService> listeners;

    /**
     * Default constructor.
     */
    public CustomBeanPersistController(Map<IDatabaseChangeListener, ExecutorService> listeners) {
        this.listeners = listeners;
    }

    @Override
    public int getExecutionOrder() {
        return 0;
    }

    @Override
    public boolean isRegisterFor(Class<?> clazz) {
        return true;
    }

    @Override
    public void postLoad(Object bean, Set<String> parameters) {
    }

    @Override
    public void postInsert(final BeanPersistRequest<?> beanPersistRequest) {
        Set<IDatabaseChangeListener> listenersSet = new HashSet<IDatabaseChangeListener>(getListeners().keySet());
        listenersSet.forEach(new Consumer<IDatabaseChangeListener>() {
            @Override
            public void accept(final IDatabaseChangeListener listener) {
                ExecutorService executorService = getListeners().get(listener);
                if (executorService != null) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            listener.postInsert(beanPersistRequest.getBean());
                        }
                    });
                }
            }
        });
    }

    @Override
    public void postDelete(BeanPersistRequest<?> beanPersistRequest) {
        Set<IDatabaseChangeListener> listenersSet = new HashSet<IDatabaseChangeListener>(getListeners().keySet());
        listenersSet.forEach(new Consumer<IDatabaseChangeListener>() {
            @Override
            public void accept(final IDatabaseChangeListener listener) {
                ExecutorService executorService = getListeners().get(listener);
                if (executorService != null) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            listener.postDelete(beanPersistRequest.getBean());
                        }
                    });
                }
            }
        });
    }

    @Override
    public void postUpdate(BeanPersistRequest<?> beanPersistRequest) {
        Set<IDatabaseChangeListener> listenersSet = new HashSet<IDatabaseChangeListener>(getListeners().keySet());
        listenersSet.forEach(new Consumer<IDatabaseChangeListener>() {
            @Override
            public void accept(final IDatabaseChangeListener listener) {
                ExecutorService executorService = getListeners().get(listener);
                if (executorService != null) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            listener.postUpdate(beanPersistRequest.getBean());
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean preUpdate(BeanPersistRequest<?> beanPersistRequest) {
        return true;
    }

    @Override
    public boolean preInsert(BeanPersistRequest<?> beanPersistRequest) {
        return true;
    }

    @Override
    public boolean preDelete(BeanPersistRequest<?> beanPersistRequest) {
        return true;
    }

    private Map<IDatabaseChangeListener, ExecutorService> getListeners() {
        return listeners;
    }
}
