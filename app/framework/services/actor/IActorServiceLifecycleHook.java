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
 * An interface to be implemented by the services which are implemented using
 * actors.<br/>
 * This interface is to be called by the {@link ActorSystemPluginImpl} in order
 * to start or stop the actor(s) of these services.<br/>
 * 
 * @author Pierre-Yves Cloux
 */
public interface IActorServiceLifecycleHook {
    /**
     * Creates the actor(s) which is used by this service
     */
    public void createActors(ActorSystem actorSystem);
}
