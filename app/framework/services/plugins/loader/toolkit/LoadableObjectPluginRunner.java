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
package framework.services.plugins.loader.toolkit;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import akka.actor.Cancellable;
import framework.commons.DataType;
import framework.commons.message.EventMessage;
import framework.commons.message.EventMessage.MessageType;
import framework.services.ServiceStaticAccessor;
import framework.services.plugins.api.AbstractCustomConfiguratorController;
import framework.services.plugins.api.AbstractRegistrationConfiguratorController;
import framework.services.plugins.api.IPluginActionDescriptor;
import framework.services.plugins.api.IPluginContext;
import framework.services.plugins.api.IPluginContext.LogLevel;
import framework.services.plugins.api.IPluginMenuDescriptor;
import framework.services.plugins.api.IPluginRunner;
import framework.services.plugins.api.IPluginRunnerConfigurator;
import framework.services.plugins.api.PluginException;
import framework.services.plugins.loader.toolkit.GenericFileLoader.AllowedCharSet;
import framework.utils.DynamicFormDescriptor;
import framework.utils.EmailUtils;
import play.Logger;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Abstract class for a plugin which loads some objects from a CSV file.<br/>
 * It expected a descriptor inherited from
 * {@link LoadableObjectPluginStaticDescriptor}.
 * 
 * @param <K>
 *            the loadable object
 * 
 * @author Pierre-Yves Cloux
 */
public abstract class LoadableObjectPluginRunner<K extends ILoadableObject> implements IPluginRunner {
    private static Logger.ALogger log = Logger.of(LoadableObjectPluginRunner.class);

    /**
     * The minimal frequency for the scheduler.
     */
    private static final int MINIMAL_FREQUENCY = 5;

    public static final String MAIN_CONFIGURATION_IDENTIFIER = "main";
    public static final String CSV_MAPPING_CONFIGURATION_IDENTIFIER = "csv_mapping";

    public static final String INPUT_FILE_CHARSET_PARAMETER = "input.file.charset";
    public static final String INPUT_FILE_PATH_PARAMETER = "input.file.path";
    public static final String REPORT_FILE_PATH_PARAMETER = "report.file.path";
    public static final String UNACTIVATE_NOT_FOUND_PARAMETER = "unactivate.not.found";
    public static final String UNACTIVATION_SELECTION_CLAUSE_PARAMETER = "unactivate.selection.clause";
    public static final String IGNORE_INVALID_ROWS_PARAMETER = "ignore.invalid.rows";
    public static final String CSV_FORMAT_PARAMETER = "csv.format";
    public static final String AUTOMATIC_LOAD_BY_SCHEDULER_PARAMETER = "automatic.load.by.scheduler";
    public static final String LOAD_FREQUENCY_IN_MINUTES_PARAMETER = "load.frequency.in.minutes";
    public static final String LOAD_START_TIME = "load.start.time";
    public static final String REPORT_MAIL_PARAMETER = "report.mail";
    public static final String TEST_MODE_PARAMETER = "test.mode";

    /**
     * List of actions.
     * 
     * @author Pierre-Yves Cloux
     * 
     */
    public static enum ActionMessage {
        TRIGGER_LOAD, CHECK_LOADING;
    }

    /**
     * The actions implemented by the plugin see {@link ActionMessage}
     */
    static Map<String, IPluginActionDescriptor> pluginActions = Collections.synchronizedMap(new HashMap<String, IPluginActionDescriptor>() {
        private static final long serialVersionUID = 1L;

        {
            this.put(ActionMessage.TRIGGER_LOAD.name(), new IPluginActionDescriptor() {

                @Override
                public Object getPayLoad(Long id) {
                    return ActionMessage.TRIGGER_LOAD;
                }

                @Override
                public String getLabel() {
                    return "Trigger load";
                }

                @Override
                public String getIdentifier() {
                    return ActionMessage.TRIGGER_LOAD.name();
                }

                @Override
                public DataType getDataType() {
                    return null;
                }

                @Override
                public DynamicFormDescriptor getFormDescriptor() {
                    return null;
                }

                @Override
                public Object getPayLoad(Long arg0, Map<String, Object> arg1) {
                    throw new UnsupportedOperationException();
                }
            });
            this.put(ActionMessage.CHECK_LOADING.name(), new IPluginActionDescriptor() {

                @Override
                public Object getPayLoad(Long id) {
                    return ActionMessage.CHECK_LOADING;
                }

                @Override
                public String getLabel() {
                    return "Check if loading";
                }

                @Override
                public String getIdentifier() {
                    return ActionMessage.CHECK_LOADING.name();
                }

                @Override
                public DataType getDataType() {
                    return null;
                }

                @Override
                public DynamicFormDescriptor getFormDescriptor() {
                    return null;
                }

                @Override
                public Object getPayLoad(Long arg0, Map<String, Object> arg1) {
                    throw new UnsupportedOperationException();
                }
            });
        }
    });

    private IPluginContext pluginContext;

    private Cancellable currentScheduler;
    private String inputFilePath;
    private String reportFilePath;
    private String reportEmail;
    private String loadStartTime;
    private FiniteDuration loadFrequency;
    private boolean unactivateNotFoundObjects;
    private String unactivationSelectionClause;
    private boolean isAutomaticLoadByScheduler;
    private LoadingStatusHolder loadingStatusHolder;
    private GenericFileLoader<K> genericFileLoader;

    /**
     * A class which holds the status of the loader.
     * 
     * @author Pierre-Yves Cloux
     */
    private static class LoadingStatusHolder {
        private boolean isLoading;

        /**
         * Return true if the object loading is currently running.
         */
        public synchronized boolean isLoading() {
            return isLoading;
        }

        /**
         * Set isLoading.
         * 
         * @param isLoading
         *            set to true if the object loading is currently running.
         */
        public synchronized void setLoading(boolean isLoading) {
            this.isLoading = isLoading;
        }
    }

    /**
     * Default constructor.
     */
    public LoadableObjectPluginRunner() {
        this.loadingStatusHolder = new LoadingStatusHolder();
    }

    @Override
    public void handleInProvisioningMessage(EventMessage eventMessage) throws PluginException {
    }

    @Override
    public void handleOutProvisioningMessage(EventMessage eventMessage) throws PluginException {
        if (eventMessage.getMessageType().equals(MessageType.CUSTOM) && eventMessage.getPayload() != null
                && eventMessage.getPayload() instanceof ActionMessage) {
            switch ((ActionMessage) eventMessage.getPayload()) {
            case CHECK_LOADING:
                getPluginContext().reportOnEventHandling(eventMessage.getTransactionId(), false, eventMessage,
                        "Loading status is : " + getLoadingStatusHolder().isLoading());
                break;
            case TRIGGER_LOAD:
                runLoad(eventMessage, false);
                break;
            }

        }
    }

    @Override
    public void init(IPluginContext pluginContext) throws PluginException {
        this.pluginContext = pluginContext;
    }

    /**
     * Return the fields which are allowed as part of the unactivation where
     * clause.
     * 
     * @return
     */
    public abstract List<String> getAllowedFieldsForUnactivationWhereClause();

    /**
     * Return the mapper to be used with the file loader.
     * 
     * @param javaScriptMappingScript
     *            a javascript mapping script
     * @return
     */
    public abstract IGenericFileLoaderMapper<K> createGenericFileLoaderMapper(String javaScriptMappingScript);

    @Override
    public void start() throws PluginException {
        try {
            // Load the configuration
            PropertiesConfiguration properties = getPluginContext()
                    .getPropertiesConfigurationFromByteArray(getPluginContext().getConfigurationAndMergeWithDefault(
                            getPluginContext().getPluginDescriptor().getConfigurationBlockDescriptors().get(MAIN_CONFIGURATION_IDENTIFIER)));

            properties.setThrowExceptionOnMissing(true);
            setInputFilePath(properties.getString(INPUT_FILE_PATH_PARAMETER));
            setLoadStartTime(properties.getString(LOAD_START_TIME));
            if (getLoadStartTime() == null || !getLoadStartTime().matches("^([01]?[0-9]|2[0-3])h[0-5][0-9]$")) {
                throw new IllegalArgumentException("Invalid time format for the " + LOAD_START_TIME + " parameter");
            }
            setLoadFrequency(FiniteDuration.create(properties.getLong(LOAD_FREQUENCY_IN_MINUTES_PARAMETER), TimeUnit.MINUTES));
            if (properties.getLong(LOAD_FREQUENCY_IN_MINUTES_PARAMETER) < MINIMAL_FREQUENCY) {
                throw new IllegalArgumentException("Invalid frequency " + LOAD_FREQUENCY_IN_MINUTES_PARAMETER + " must be more than 5 minutes");
            }
            setUnactivateNotFoundObjects(properties.getBoolean(UNACTIVATE_NOT_FOUND_PARAMETER));
            setUnactivationSelectionClause(properties.getString(UNACTIVATION_SELECTION_CLAUSE_PARAMETER));
            if (!StringUtils.isBlank(getUnactivationSelectionClause())) {
                Pattern pattern = Pattern.compile("\\s?\\(?(\\S*)\\s?(=|like)\\s*'");
                Matcher matcher = pattern.matcher(getUnactivationSelectionClause());
                while (matcher.find()) {
                    System.out.println(matcher.group(1));
                    if (!getAllowedFieldsForUnactivationWhereClause().contains(matcher.group(1))) {
                        throw new IllegalArgumentException("Field not allowed in " + UNACTIVATION_SELECTION_CLAUSE_PARAMETER + " please use only "
                                + getAllowedFieldsForUnactivationWhereClause());
                    }
                }
            }
            setReportFilePath(properties.getString(REPORT_FILE_PATH_PARAMETER));
            setAutomaticLoadByScheduler(properties.getBoolean(AUTOMATIC_LOAD_BY_SCHEDULER_PARAMETER));
            setReportEmail(properties.getString(REPORT_MAIL_PARAMETER));

            // Load the javaScript mapping script
            Pair<Boolean, byte[]> javascriptMappingConfiguration = getPluginContext().getConfiguration(
                    getPluginContext().getPluginDescriptor().getConfigurationBlockDescriptors().get(CSV_MAPPING_CONFIGURATION_IDENTIFIER), true);
            if (javascriptMappingConfiguration.getLeft()) {
                throw new PluginException("WARNING: the javascript configuration may be outdated and the plugin might crash, please check it againt the current"
                        + " documentation and save it before attempting to start the plugin");
            }

            // Creates the generic file loader
            this.genericFileLoader = new GenericFileLoader<K>(createGenericFileLoaderMapper(new String(javascriptMappingConfiguration.getRight())),
                    GenericFileLoader.CSVFormatType.valueOf(properties.getString(CSV_FORMAT_PARAMETER)),
                    AllowedCharSet.getFromCharset(properties.getString(INPUT_FILE_CHARSET_PARAMETER)), log, properties.getBoolean(TEST_MODE_PARAMETER),
                    properties.getBoolean(IGNORE_INVALID_ROWS_PARAMETER));

            // If scheduled, start the scheduler
            // Find the right FiniteDuration before starting the plugin
            if (isAutomaticLoadByScheduler()) {
                long howMuchMinutesUntilStartTime = howMuchMinutesUntilStartTime();

                setCurrentScheduler(ServiceStaticAccessor.getSysAdminUtils().scheduleRecurring(true,
                        getPluginContext().getPluginDescriptor().getName() + " plugin " + getPluginContext().getPluginConfigurationName(),
                        Duration.create(howMuchMinutesUntilStartTime, TimeUnit.MINUTES), getLoadFrequency(), new Runnable() {
                            @Override
                            public void run() {
                                EventMessage eventMessage = new EventMessage();
                                runLoad(eventMessage, true);
                            }
                        }));

                String startTimeMessage = String.format("Scheduler programmed to run in %d minutes", howMuchMinutesUntilStartTime);
                getPluginContext().log(LogLevel.INFO, startTimeMessage);
                getPluginContext().reportOnStartup(false, startTimeMessage);
            }

            getPluginContext().log(LogLevel.INFO, "Object loader started");
        } catch (Exception e) {
            if (e instanceof PluginException) {
                throw (PluginException) e;
            }
            throw new PluginException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (getCurrentScheduler() != null) {
                getCurrentScheduler().cancel();
                getPluginContext().log(LogLevel.INFO, "Scheduler stopped");
            }
        } catch (Exception e) {
        }
        getPluginContext().log(LogLevel.INFO, getPluginContext().getPluginConfigurationName() + " plugin stopped");
    }

    @Override
    public IPluginRunnerConfigurator getConfigurator() {
        return new IPluginRunnerConfigurator() {
            @Override
            public Map<DataType, AbstractRegistrationConfiguratorController> getDataTypesWithRegistration() {
                return null;
            }

            @Override
            public AbstractCustomConfiguratorController getCustomConfigurator() {
                return null;
            }

            @Override
            public Map<String, IPluginActionDescriptor> getActionDescriptors() {
                return pluginActions;
            }

            @Override
            public IPluginMenuDescriptor getMenuDescriptor() {
                return null;
            }
        };
    }

    /**
     * Return true is the not found objects should be disabled.
     */
    protected boolean isUnactivateNotFoundObjects() {
        return unactivateNotFoundObjects;
    }

    /**
     * Get the select clause for the deactivation of the objects.
     */
    protected String getUnactivationSelectionClause() {
        return unactivationSelectionClause;
    }

    /**
     * Get the plugin context.
     */
    protected IPluginContext getPluginContext() {
        return pluginContext;
    }

    /**
     * Method called by the scheduler.
     * 
     * @param eventMessage
     *            the event message
     * @param scheduled
     *            set to true if scheduled
     */
    private void runLoad(EventMessage eventMessage, boolean scheduled) {
        if (!getLoadingStatusHolder().isLoading()) {
            getPluginContext().reportOnEventHandling(eventMessage.getTransactionId(), false, eventMessage, "Object load started");
            try {
                getLoadingStatusHolder().setLoading(true);
                loadFile();
                getPluginContext().reportOnEventHandling(eventMessage.getTransactionId(), false, eventMessage, "Object load completed");
            } catch (Exception e) {
                getPluginContext().reportOnEventHandling(eventMessage.getTransactionId(), true, eventMessage, "Object load failed", e);
            } finally {
                getLoadingStatusHolder().setLoading(false);
            }
        } else {
            if (!scheduled) {
                getPluginContext().reportOnEventHandling(eventMessage.getTransactionId(), false, eventMessage,
                        "A file is being processed, please wait for the completion of the current load");
            } else {
                getPluginContext().reportOnEventHandling(eventMessage.getTransactionId(), true, eventMessage,
                        "WARNING: the scheduled load was blocked because another load was already running."
                                + "If you need to be executed, please proceed via the manual interface.");
            }
        }
    }

    /**
     * Load the object file.<br/>
     * This method is blocking. Only one load attempt could be received at a
     * time.
     * 
     * @throws PluginException
     */
    private synchronized void loadFile() throws PluginException {
        try {
            String reportAsString = getGenericFileLoader().performLoad(getPluginContext().getFileFromSharedStorage(getInputFilePath()));
            // If a file is defined, write the report to the file system
            if (!StringUtils.isBlank(getReportFilePath())) {
                BufferedWriter bWriter = null;
                try {
                    bWriter = new BufferedWriter(
                            new OutputStreamWriter(getPluginContext().writeFileInSharedStorage(String.format(getReportFilePath(), new Date()), true)));
                    bWriter.append(reportAsString);
                } catch (Exception e) {
                    throw new PluginException("Error while writing the load report", e);
                } finally {
                    IOUtils.closeQuietly(bWriter);
                }
            }

            // If a mail is defined, send the report to the specified mail
            if (!StringUtils.isBlank(getReportEmail())) {
                EmailUtils.sendEmail(this.getPluginContext().getPluginConfigurationName() + " report", play.Configuration.root().getString("maf.email.from"),
                        "<pre>" + reportAsString + "</pre>", getReportEmail());
            }
        } catch (Exception e) {
            log.error("Error while running the load job", e);
            throw new PluginException(e);
        }
    }

    /**
     * Return the number of minutes until the next "start time".
     */
    private long howMuchMinutesUntilStartTime() {
        String time = getLoadStartTime();
        Date today = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time.substring(0, 2)));
        calendar.set(Calendar.MINUTE, Integer.valueOf(time.substring(3, 5)));
        if (calendar.getTime().before(today)) {
            calendar.add(Calendar.DATE, 1);
        }
        long diff = calendar.getTime().getTime() - today.getTime();
        return diff / (60 * 1000);
    }

    /**
     * Get the path of the input file.
     */
    private String getInputFilePath() {
        return inputFilePath;
    }

    /**
     * Set the path of the input file.
     * 
     * @param filePath
     *            the path of the input file
     */
    private void setInputFilePath(String filePath) {
        this.inputFilePath = filePath;
    }

    /**
     * Get the load frequency.
     */
    private FiniteDuration getLoadFrequency() {
        return loadFrequency;
    }

    /**
     * Set the load frequency.
     * 
     * @param loadFrequency
     *            the load frequency
     */
    private void setLoadFrequency(FiniteDuration loadFrequency) {
        this.loadFrequency = loadFrequency;
    }

    /**
     * Set unactivateNotFoundObjects.
     * 
     * @param unactivateNotFoundObjects
     *            set to true if the not found objects should be disabled.
     */
    private void setUnactivateNotFoundObjects(boolean unactivateNotFoundObjects) {
        this.unactivateNotFoundObjects = unactivateNotFoundObjects;
    }

    /**
     * Get the report file path.
     */
    private String getReportFilePath() {
        return reportFilePath;
    }

    /**
     * Set the report file path.
     * 
     * @param reportFilePath
     *            the report file path
     */
    private void setReportFilePath(String reportFilePath) {
        this.reportFilePath = reportFilePath;
    }

    /**
     * Get the loading status holder.
     */
    private LoadingStatusHolder getLoadingStatusHolder() {
        return loadingStatusHolder;
    }

    /**
     * Return true if the objects loading is triggered by a scheduler.
     */
    private boolean isAutomaticLoadByScheduler() {
        return isAutomaticLoadByScheduler;
    }

    /**
     * Set isAutomaticLoadByScheduler.
     * 
     * @param isAutomaticLoadByScheduler
     *            set to true if the objects loading is triggered by a
     *            scheduler.
     */
    private void setAutomaticLoadByScheduler(boolean isAutomaticLoadByScheduler) {
        this.isAutomaticLoadByScheduler = isAutomaticLoadByScheduler;
    }

    /**
     * Get the scheduler.
     */
    private Cancellable getCurrentScheduler() {
        return currentScheduler;
    }

    /**
     * Set the scheduler.
     * 
     * @param currentScheduler
     *            the scheduler
     */
    private void setCurrentScheduler(Cancellable currentScheduler) {
        this.currentScheduler = currentScheduler;
    }

    /**
     * Get the email address for the report.
     */
    private String getReportEmail() {
        return reportEmail;
    }

    /**
     * Set the report email for the report.
     * 
     * @param reportEmail
     *            the email address
     */
    private void setReportEmail(String reportEmail) {
        this.reportEmail = reportEmail;
    }

    /**
     * Get the loading start time.
     */
    private String getLoadStartTime() {
        return loadStartTime;
    }

    /**
     * Set the loading start time.
     * 
     * @param loadStartTime
     *            the loading start time
     */
    private void setLoadStartTime(String loadStartTime) {
        this.loadStartTime = loadStartTime;
    }

    /**
     * Set the select clause for the deactivation of the objects.
     * 
     * @param unactivationSelectionClause
     *            the select clause
     */
    private void setUnactivationSelectionClause(String unactivationSelectionClause) {
        this.unactivationSelectionClause = unactivationSelectionClause;
    }

    /**
     * Get the generic file loader.
     */
    private GenericFileLoader<K> getGenericFileLoader() {
        return genericFileLoader;
    }
}
