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

import java.beans.XMLEncoder;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import play.Logger;
import akka.actor.DeadLetter;
import akka.actor.UntypedActor;
import akka.japi.Creator;

/**
 * The agent which manages the deadletters for the MAF agent systems.<br/>
 * It is created by the {@link ActorSystemPluginImpl}.<br/>
 * The dead letters are wrote as files into a dedicated folder.<br/>
 * The structure of the file is:
 * <ul>
 * <li>The path of the recipient actor on the first line</li>
 * <li>The message serialized as XML (using the standard Java XML encoder</li>
 * </ul>
 * At startup the plugin will check of some "dead letters" file are found in the
 * dead letters folder.<br/>
 * If yes an error is logged but the plugin do not stop.<br/>
 * Any found dead letter file is move to the "re-processing" folder for a latter
 * manual handling.
 * 
 * @author Pierre-Yves Cloux
 */
public class DeadLetterManager extends UntypedActor {
    private static Logger.ALogger log = Logger.of(DeadLetterManager.class);
    public static final String OUTPUT_FILE_NAME_TEMPLATE = "%s_%2$tF_%2$tT_%s";
    private File deadLetterFileSystem;
    private File reprocessedDeadLetters;

    /**
     * The creator for this agent.<br/>
     * A bit boiler plate but mandatory to be able to pass some parameters
     * 
     * @author Pierre-Yves Cloux
     */
    public static class DeadLetterCreator implements Creator<DeadLetterManager> {
        private static final long serialVersionUID = 8414813453080656854L;
        private File deadLetterFileSystem;
        private File reprocessedDeadLetters;

        public DeadLetterCreator(File deadLetterFileSystem, File reprocessedDeadLetters) {
            this.deadLetterFileSystem = deadLetterFileSystem;
            this.reprocessedDeadLetters = reprocessedDeadLetters;
        }

        @Override
        public DeadLetterManager create() throws Exception {
            return new DeadLetterManager(deadLetterFileSystem, reprocessedDeadLetters);
        }
    }

    /**
     * Creates a new dead letter manager
     * 
     * @param deadLetterFileSystem
     *            the file system which will contains the dead letters
     * @param reprocessedDeadLetters
     *            the file system which will contains the re-processed dead
     *            letters
     * @throws ActorSystemPluginException
     */
    public DeadLetterManager(File deadLetterFileSystem, File reprocessedDeadLetters) throws ActorSystemPluginException {
        this.deadLetterFileSystem = deadLetterFileSystem;
        this.reprocessedDeadLetters = reprocessedDeadLetters;
        checkFileSystems(deadLetterFileSystem, reprocessedDeadLetters);
        checkDeadLetters();
    }

    /**
     * Check if the dead letter file systems are valid
     * 
     * @param deadLetterFileSystem
     * @param reprocessedDeadLetters
     * @throws ActorSystemPluginException
     */
    private void checkFileSystems(File deadLetterFileSystem, File reprocessedDeadLetters) throws ActorSystemPluginException {
        // Check if the deadLetterFileSystem and reprocessedDeadLetters exists
        if (deadLetterFileSystem == null || !deadLetterFileSystem.exists() || !deadLetterFileSystem.isDirectory()) {
            throw new ActorSystemPluginException(String.format("Invalid DeadLetters file system %s",
                    deadLetterFileSystem != null ? deadLetterFileSystem.toString() : "NULL"));
        }
        if (reprocessedDeadLetters == null || !reprocessedDeadLetters.exists() || !reprocessedDeadLetters.isDirectory()) {
            throw new ActorSystemPluginException(String.format("Invalid re-processed DeadLetters file system %s",
                    reprocessedDeadLetters != null ? reprocessedDeadLetters.toString() : "NULL"));
        }
    }

    /**
     * Attempt to re-process the deadletters found in the file system.<br/>
     * <b>WARNING</b>: This does nothing today except transferring the files to
     * another directory.<br/>
     * This is a one-shot operation.<br/>
     * The reprocessed dead letters are put into another file system
     */
    private void reprocessingDeadLetters() {
        for (File file : getDeadLetterFileSystem().listFiles()) {
            try {
                FileUtils.moveFileToDirectory(file, getReprocessedDeadLetters(), false);
                log.info(String.format("Moved dead letter file to reprocessing area %s", file.getName()));
            } catch (IOException e) {
                log.error(String.format("Unable to move the dead letter file to the reprocessing area %s", file.getAbsolutePath()));
            }
        }
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof DeadLetter) {
            DeadLetter deadLetter = (DeadLetter) message;
            writeDeadLetterToFile(deadLetter);
        } else {
            String className = message != null ? message.getClass().getName() : "NULL";
            String value = message != null ? message.toString() : "NULL";
            log.error(String.format("Message sent to DeadLetter but it is not one (Class: %s, Value: %s)", className, value));
        }
    }

    /**
     * Write the dead letters to the file system
     * 
     * @param deadLetter
     *            a dead letter
     */
    private void writeDeadLetterToFile(DeadLetter deadLetter) {
        String fileName = String.format(OUTPUT_FILE_NAME_TEMPLATE, deadLetter.recipient().path().name(), new Date(), ".log");
        BufferedWriter bWriter = null;
        try {
            bWriter = new BufferedWriter(new FileWriter(new File(getDeadLetterFileSystem(), fileName)));
            // Serialize the message as XML
            ByteArrayOutputStream baOut = new ByteArrayOutputStream();
            XMLEncoder encoder = new XMLEncoder(baOut);
            if (deadLetter.message() != null) {
                encoder.writeObject(deadLetter.message());
            }
            encoder.flush();
            encoder.close();
            // Write it to a file
            // First line is actor path
            bWriter.write(deadLetter.recipient().path().toString());
            bWriter.newLine();
            // Then write the message
            bWriter.write(baOut.toString());
            bWriter.flush();
        } catch (Exception e) {
            log.error("Error while writing the dead letter info the dead letters folder", e);
        } finally {
            IOUtils.closeQuietly(bWriter);
        }
    }

    /**
     * Check if the Dead letters folder contains some dead letters
     */
    private void checkDeadLetters() {
        if (getDeadLetterFileSystem().listFiles().length != 0) {
            // The deadletter folder is not empty, attempt to re-process the
            // undelivered messages
            log.error(String.format("The DeadLetter file system is not empty (%d files), re-processing may be required",
                    this.deadLetterFileSystem.listFiles().length));
            reprocessingDeadLetters();
        }
    }

    private File getDeadLetterFileSystem() {
        return deadLetterFileSystem;
    }

    private File getReprocessedDeadLetters() {
        return reprocessedDeadLetters;
    }
}
