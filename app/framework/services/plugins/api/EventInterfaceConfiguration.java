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
package framework.services.plugins.api;

import scala.concurrent.duration.Duration;
import akka.actor.OneForOneStrategy;

/**
 * The structure to be used to configure the IN or OUT event interface of a
 * plugin.
 * 
 * @author Pierre-Yves Cloux
 */
public class EventInterfaceConfiguration {
    public static final int DEFAULT_POOL_SIZE = 1;
    public static final int DEFAULT_NUMBER_OF_RETRY = 0;
    public static final Duration DEFAULT_RETRY_DURATION = Duration.create(0, "seconds");

    private int poolSize;
    private int numberOfRetry;
    private Duration retryDuration;

    /**
     * Creates an interface with the default configuration, namely:
     * <ul>
     * <li>poolsize : 2</li>
     * <li>numberOfRetry : 0</li>
     * <li>retryDuration : 0 seconds</li>
     * </ul>
     */
    public EventInterfaceConfiguration() {
        this.poolSize = DEFAULT_POOL_SIZE;
        this.numberOfRetry = DEFAULT_NUMBER_OF_RETRY;
        this.retryDuration = DEFAULT_RETRY_DURATION;
    }

    public EventInterfaceConfiguration(int poolSize, int numberOfRetry, Duration retryDuration) {
        super();
        if (poolSize <= 0 || numberOfRetry < 0 || !retryDuration.gteq(Duration.Zero())) {
            throw new IllegalArgumentException("Invalid configuration for the interface");
        }
        this.poolSize = poolSize;
        this.numberOfRetry = numberOfRetry;
        this.retryDuration = retryDuration;
    }

    /**
     * Return the size of the actors pool for OUT messages which deal with the
     * provisioning command asynchronously
     */
    public int getPoolSize() {
        return this.poolSize;
    }

    /**
     * Return the number of retry to be attempted in case of failure of a agent
     * managing the OUT messages<br/>
     * See {@link OneForOneStrategy}
     */
    public int getNumberOfRetry() {
        return this.numberOfRetry;
    }

    /**
     * Return the time slot during which a retry can be attempted See
     * {@link OneForOneStrategy}
     */
    public Duration getRetryDuration() {
        return this.retryDuration;
    }

}
