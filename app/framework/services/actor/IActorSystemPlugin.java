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
package framework.services.actor;

import akka.actor.ActorSystem;

/**
 * Interface to be implemented by the service which manages (retreive) the actor
 * system used by MAF
 * 
 * @author Pierre-Yves Cloux
 */
public interface IActorSystemPlugin {
    public static final String NAME = "actorSystemPlugin";

    /**
     * A dispatcher configured with a durable queue (file system)
     */
    public static final String DURABLE_DISPATCHER = "maf-durable-dispatcher";

    /**
     * Start the MAF actors management system
     */
    public void startup() throws ActorSystemPluginException;

    /**
     * Shutdown the MAF actors management system
     */
    public void shutdown();

    /**
     * Returns the Akka actor system
     * 
     * @return an actor system
     */
    public ActorSystem getActorSystem() throws ActorSystemPluginException;
}
