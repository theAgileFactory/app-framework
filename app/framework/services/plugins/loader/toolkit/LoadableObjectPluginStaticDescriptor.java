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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import framework.commons.DataType;
import framework.commons.IFrameworkConstants;
import framework.services.plugins.api.IPluginActionDescriptor;
import framework.services.plugins.api.IPluginConfigurationBlockDescriptor;
import framework.services.plugins.api.IStaticPluginRunnerDescriptor;
import framework.services.plugins.api.LinkedProperties;
import framework.utils.DynamicFormDescriptor;

/**
 * An abstract static descriptor for a plugin which loads some data.<br/>
 * Here are the parameters which can be defined in the main configuration:
 * <ul>
 * <li>INPUT_FILE_CHARSET_PARAMETER = "input.file.charset" : The java charset to
 * be used to load the file (UTF-8, ISO8856-1)</li>
 * <li>INPUT_FILE_PATH_PARAMETER = "input.file.path" : the path to the input
 * file (relative to the sFTP inputs folder)</li>
 * <li>REPORT_FILE_PATH_PARAMETER = "report.file.path" : the path in which to
 * set the load report</li>
 * <li>UNACTIVATE_NOT_FOUND_PARAMETER = "unactivate.not.found" : if "true" all
 * the objects of the database which are not found in the load are automatically
 * set "inactive"</li>
 * <li>UNACTIVATION_SELECTION_CLAUSE_PARAMETER = "unactivate.selection.clause" :
 * a "selection clause" to restrict the objects which are to be automatically
 * inactivated.<br/>
 * the clause can be set on mail, ref_id, erp_ref_id, org_unit_ref_id and
 * actor_type_ref_id.<br/>
 * Example : mail like '%{@literal @}company.com' or
 * actor_type_ref_id="REGULAR".<br/>
 * Only basic comparisons criteria are supported.</li>
 * <li>IGNORE_INVALID_ROWS_PARAMETER = "ignore.invalid.rows" : if yes, the
 * invalid rows (not matching the validation criteria) will be updated in the
 * database. The invalid rows will be listed in the report.</li>
 * <li>CSV_FORMAT_PARAMETER = "csv.format" : the type of CSV format to be used
 * (here are the supported formats : EXCEL,MYSQL,RFC4180</li>
 * <li>AUTOMATIC_LOAD_BY_SCHEDULER_PARAMETER = "manual.trigger" : if true, the
 * plugin does not trigger a scheduled load, you must trigger it manually
 * through the plugin admin interface</li>
 * <li>LOAD_START_TIME = "load.start.time" : the time when the load should start
 * (Expressed as a 24h based format : "hh:mm")</li>
 * <li>LOAD_FREQUENCY_IN_MINUTES_PARAMETER = "load.frequency.in.minutes" : the
 * frequency to be used after the load is started in minutes (1440 minutes means
 * that the load will be triggered once a day)</li>
 * <li>REPORT_MAIL_PARAMETER="report.mail" : if an e-mail is provided the report
 * is sent to this address</li>
 * <li>TEST_MODE_PARAMETER="test.mode" : if true the input file is parsed but
 * the database is not updated</li>
 * </ul>
 * 
 * <p>
 * The CSV_MAPPING_CONFIGURATION actually stores a javascript which allow to map
 * the content of the input file (parsed) with a database object. The javascript
 * mapping can also be used to "exclude" some objects from the load.
 * </p>
 * 
 * @author Pierre-Yves Cloux
 * 
 */
public abstract class LoadableObjectPluginStaticDescriptor implements IStaticPluginRunnerDescriptor {

    /**
     * List of actions.
     * 
     * @author Pierre-Yves Cloux
     * 
     */
    public static enum ActionMessage {
        TRIGGER_LOAD, CHECK_LOADING;
    }

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

    private Map<String, IPluginConfigurationBlockDescriptor> pluginConfigurationBlocks;

    /**
     * Default constructor.
     * 
     * @param fileNamePart
     *            the file name part (used for the file name of the CSV and the
     *            report)
     * @param defaultMappingScript
     *            the default mapping script (for the
     *            CSV_MAPPING_CONFIGURATION_IDENTIFIER configuration)
     */
    public LoadableObjectPluginStaticDescriptor(final String fileNamePart, final String defaultMappingScript) {

        pluginConfigurationBlocks = Collections.synchronizedMap(new HashMap<String, IPluginConfigurationBlockDescriptor>() {
            private static final long serialVersionUID = 1L;

            {

                this.put(MAIN_CONFIGURATION_IDENTIFIER, new IPluginConfigurationBlockDescriptor() {

                    @Override
                    public byte[] getDefaultValue() {

                        Properties properties = new LinkedProperties();
                        properties.put(TEST_MODE_PARAMETER, "true");
                        properties.put(CSV_FORMAT_PARAMETER, GenericFileLoader.CSVFormatType.EXCEL.name());
                        properties.put(INPUT_FILE_CHARSET_PARAMETER, "ISO8859-1");
                        properties.put(INPUT_FILE_PATH_PARAMETER, "/" + IFrameworkConstants.INPUT_FOLDER_NAME + "/input-" + fileNamePart + ".csv");
                        properties.put(AUTOMATIC_LOAD_BY_SCHEDULER_PARAMETER, "false");
                        properties.put(LOAD_START_TIME, "00h00");
                        properties.put(LOAD_FREQUENCY_IN_MINUTES_PARAMETER, "1440");
                        properties.put(IGNORE_INVALID_ROWS_PARAMETER, "false");
                        properties.put(UNACTIVATE_NOT_FOUND_PARAMETER, "false");
                        properties.put(UNACTIVATION_SELECTION_CLAUSE_PARAMETER, "");
                        properties.put(REPORT_MAIL_PARAMETER, "");
                        properties.put(REPORT_FILE_PATH_PARAMETER, "/" + IFrameworkConstants.OUTPUT_FOLDER_NAME + "/report-" + fileNamePart + ".log");

                        StringWriter sw = new StringWriter();
                        try {
                            properties.store(sw, "Default configuration (WARNING: debug mode is activated by default)");
                        } catch (IOException e) {
                        }
                        return sw.toString().getBytes();
                    }

                    @Override
                    public String getDescription() {
                        return "Various configuration options for the plugin";
                    }

                    @Override
                    public ConfigurationBlockEditionType getEditionType() {
                        return ConfigurationBlockEditionType.PROPERTIES;
                    }

                    @Override
                    public String getIdentifier() {
                        return MAIN_CONFIGURATION_IDENTIFIER;
                    }

                    @Override
                    public String getName() {
                        return "Parameters";
                    }

                    @Override
                    public int getVersion() {
                        return 2;
                    }
                });

                this.put(CSV_MAPPING_CONFIGURATION_IDENTIFIER, new IPluginConfigurationBlockDescriptor() {
                    @Override
                    public byte[] getDefaultValue() {
                        return defaultMappingScript.getBytes();
                    }

                    @Override
                    public String getDescription() {
                        return "The configuration of the mapping between the CSV file columns and the Object attributes."
                                + "This configuration is implemented as JavaScript.";
                    }

                    @Override
                    public ConfigurationBlockEditionType getEditionType() {
                        return ConfigurationBlockEditionType.JAVASCRIPT;
                    }

                    @Override
                    public String getIdentifier() {
                        return CSV_MAPPING_CONFIGURATION_IDENTIFIER;
                    }

                    @Override
                    public String getName() {
                        return "CSV file mapping";
                    }

                    @Override
                    public int getVersion() {
                        return 0;
                    }
                });
            }
        });
    }

    @Override
    public Map<String, IPluginConfigurationBlockDescriptor> getConfigurationBlockDescriptors() {
        return pluginConfigurationBlocks;
    }

    @Override
    public boolean multiInstanceAllowed() {
        return true;
    }

    @Override
    public Map<String, IPluginActionDescriptor> getActionDescriptors() {
        return pluginActions;
    }
}
