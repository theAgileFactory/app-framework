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

import java.io.File;
import java.util.List;

import play.Logger;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;

import com.typesafe.config.ConfigFactory;

/**
 * The default implementation for the {@link ActorSystem}.<br/>
 * This plugin creates the {@link ActorSystem} but also manage the deadletters
 * (it is also an actor).<br/>
 * 
 * @author Pierre-Yves Cloux
 */
public class ActorSystemPluginImpl implements IActorSystemPlugin {
    private static Logger.ALogger log = Logger.of(ActorSystemPluginImpl.class);
    private ActorSystem actorSystem;
    private String actorSystemName;
    private File deadLetterFileSystem;
    private File reprocessedDeadLetters;
    private List<IActorServiceLifecycleHook> actorLifeCycleHooks;

    /**
     * The name of the actor system
     * 
     * @param actorSystemName
     *            a name
     * @param deadLetterFileSystem
     *            the directory which is to be used to "store" the deadletters
     *            as XML
     * @param reprocessedDeadLetters
     *            the directory which is to contain the "re-processed"
     *            deadletters (just in case the re-processing failed)
     * @throws ActorSystemPluginException
     */
    public ActorSystemPluginImpl(String actorSystemName, File deadLetterFileSystem, File reprocessedDeadLetters,
            List<IActorServiceLifecycleHook> actorLifeCycleHooks) throws ActorSystemPluginException {
        this.actorSystemName = actorSystemName;
        this.deadLetterFileSystem = deadLetterFileSystem;
        this.reprocessedDeadLetters = reprocessedDeadLetters;
        this.actorLifeCycleHooks = actorLifeCycleHooks;
    }

    /**
     * Initialize the dead letter actor (which is actually this class)
     * 
     * @throws ActorSystemPluginException
     */
    private void initializeTheDeadLetterActor(final File deadLetterFileSystem, final File reprocessedDeadLetters) throws ActorSystemPluginException {
        final ActorRef actor = getActorSystem().actorOf(Props.create(new DeadLetterManager.DeadLetterCreator(deadLetterFileSystem, reprocessedDeadLetters)));
        getActorSystem().eventStream().subscribe(actor, DeadLetter.class);
    }

    /**
     * Start the actors registered with this plugin
     * 
     * @param actorLifeCycleHooks
     *            a list of services implemented as actors
     * @throws ActorSystemPluginException
     */
    private void startActors(List<IActorServiceLifecycleHook> actorLifeCycleHooks) throws ActorSystemPluginException {
        for (IActorServiceLifecycleHook hook : actorLifeCycleHooks) {
            hook.createActors(getActorSystem());
        }
    }

    @Override
    public ActorSystem getActorSystem() throws ActorSystemPluginException {
        if (this.actorSystem != null && !this.actorSystem.isTerminated()) {
            return this.actorSystem;
        }
        throw new ActorSystemPluginException("Actor system is not initialized or terminated");
    }

    @Override
    public void startup() throws ActorSystemPluginException {
        // Creation of the agent system (a configuration lock for this actor
        // system must be available in application.conf
        this.actorSystem = ActorSystem.create(getActorSystemName(), ConfigFactory.load().getConfig(getActorSystemName()));

        // Check the deadletters
        try {
            initializeTheDeadLetterActor(getDeadLetterFileSystem(), getReprocessedDeadLetters());
        } catch (Exception e) {
            log.error("Error while starting the deadletter manager", e);
            this.actorSystem.shutdown();
            throw new ActorSystemPluginException(e);
        }

        // Starting the actors
        startActors(getActorLifeCycleHooks());
    }

    @Override
    public void shutdown() {
        try {
            getActorSystem().shutdown();
            log.info("Actor system shutdown requested, awaiting termination...");
            getActorSystem().awaitTermination();
            log.info("...actor system terminated");
        } catch (Exception e) {
            log.error("Error while stopping the actor system", e);
        }
    }

    private String getActorSystemName() {
        return actorSystemName;
    }

    private File getDeadLetterFileSystem() {
        return deadLetterFileSystem;
    }

    private File getReprocessedDeadLetters() {
        return reprocessedDeadLetters;
    }

    private List<IActorServiceLifecycleHook> getActorLifeCycleHooks() {
        return actorLifeCycleHooks;
    }
}
