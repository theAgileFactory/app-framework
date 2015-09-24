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

import javax.inject.Inject;
import javax.inject.Singleton;

import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;

/**
 * The default implementation for the {@link ActorSystem}.<br/>
 * This plugin creates the {@link ActorSystem} but also manage the deadletters
 * (it is also an actor).<br/>
 * 
 * @author Pierre-Yves Cloux
 */
@Singleton
public class ActorSystemPluginImpl implements IActorSystemPlugin {
    private static Logger.ALogger log = Logger.of(ActorSystemPluginImpl.class);
    private ActorSystem actorSystem;

    /**
     * The name of the maf actor system
     */
    private String actorSystemName;

    /**
     * The directory which is to be used to "store" the deadletters as XML
     */
    private File deadLetterFileSystem;

    /**
     * The directory which is to contain the "re-processed" deadletters (just in
     * case the re-processing failed)
     */
    private File reprocessedDeadLetters;

    public enum Config {
        ACTOR_SYSTEM_NAME("maf.actor.system"), DEAD_LETTERS_FOLDER("maf.actor.deadletters.folder"), DEAD_LETTERS_REPROCESSING_FOLDER(
                "maf.actor.deadletters.reprocessing.folder");

        private String configurationKey;

        private Config(String configurationKey) {
            this.configurationKey = configurationKey;
        }

        public String getConfigurationKey() {
            return configurationKey;
        }
    }

    /**
     * Creates a new ActorSystemPluginImpl
     * 
     * @param lifecycle
     *            the play application lifecycle listener
     * @param configuration
     *            the play application configuration
     * @param actorSystem
     * @throws ActorSystemPluginException
     */
    @Inject
    public ActorSystemPluginImpl(ApplicationLifecycle lifecycle, Configuration configuration, ActorSystem actorSystem) throws ActorSystemPluginException {
        log.info("SERVICE>>> ActorSystemPluginImpl starting...");
        this.actorSystemName = configuration.getString(Config.ACTOR_SYSTEM_NAME.getConfigurationKey());
        this.deadLetterFileSystem = new File(configuration.getString(Config.DEAD_LETTERS_FOLDER.getConfigurationKey()));
        this.reprocessedDeadLetters = new File(configuration.getString(Config.DEAD_LETTERS_REPROCESSING_FOLDER.getConfigurationKey()));
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> ActorSystemPluginImpl stopping...");
            shutdown();
            log.info("SERVICE>>> ActorSystemPluginImpl stopped");
            return Promise.pure(null);
        });
        startup();
        log.info("SERVICE>>> ActorSystemPluginImpl started");
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

    @Override
    public ActorSystem getActorSystem() {
        if (this.actorSystem != null && !this.actorSystem.isTerminated()) {
            return this.actorSystem;
        }
        throw new IllegalStateException("Actor system is not initialized or terminated");
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
}
