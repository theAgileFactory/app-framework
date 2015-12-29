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
package framework.services.job;

import framework.commons.IFrameworkConstants;
import framework.services.configuration.II18nMessagesPlugin;
import framework.utils.Table;
import framework.utils.formats.ObjectFormatter;
import framework.utils.formats.StringFormatFormatter;

/**
 * Job descriptor (for a scheduler).
 * 
 * @author Johann Kohler
 * 
 */
public interface IJobDescriptor {

    /**
     * Get the unique id of the job.
     */
    public String getId();

    /**
     * Get the job name.
     * 
     * @param languageCode
     *            a language code (example: en,fr,de)
     */
    public String getName(String languageCode);

    /**
     * Get the job description.
     * 
     * @param languageCode
     *            a language code (example: en,fr,de)
     */
    public String getDescription(String languageCode);

    /**
     * Get the frequency of execution.
     */
    public Frequency getFrequency();

    /**
     * Get the start hour in the day.
     */
    public int getStartHour();

    /**
     * Get the start minute in the day.
     */
    public int getStartMinute();

    /**
     * Trigger the job.
     */
    public void trigger();

    /**
     * The url (route) of the trigger.
     */
    public String getTriggerUrl();

    /**
     * The possible frequency.
     * 
     * @author Johann Kohler
     */
    public static enum Frequency {
        ONE_TIME, HOURLY, DAILY;
    }

    /**
     * Define a job descriptor row.
     * 
     * @author Johann Kohler
     */
    public static class JobDescriptorRow {

        public static Table<JobDescriptorRow> templateTable = getTable();

        /**
         * Get the table.
         */
        public static Table<JobDescriptorRow> getTable() {
            return new Table<JobDescriptorRow>() {
                {
                    setIdFieldName("id");

                    addColumn("name", "name", "object.job_descriptor.name.label", Table.ColumnDef.SorterType.NONE);
                    setJavaColumnFormatter("name", new ObjectFormatter<JobDescriptorRow>());

                    addColumn("description", "description", "object.job_descriptor.description.label", Table.ColumnDef.SorterType.NONE);
                    setJavaColumnFormatter("description", new ObjectFormatter<JobDescriptorRow>());

                    addColumn("executionTime", "executionTime", "object.job_descriptor.execution_time.label", Table.ColumnDef.SorterType.NONE);
                    setJavaColumnFormatter("executionTime", new ObjectFormatter<JobDescriptorRow>());

                    addColumn("frequency", "frequency", "object.job_descriptor.frequency.label", Table.ColumnDef.SorterType.NONE);
                    setJavaColumnFormatter("frequency", new ObjectFormatter<JobDescriptorRow>());

                    addColumn("triggerActionLink", "id", "", Table.ColumnDef.SorterType.NONE);
                    setJavaColumnFormatter("triggerActionLink", new StringFormatFormatter<JobDescriptorRow>(
                            "<a href=\"%s\"><span class=\"fa fa-play-circle-o\"></span></a>", new StringFormatFormatter.Hook<JobDescriptorRow>() {
                        @Override
                        public String convert(JobDescriptorRow jobDescriptorRow) {
                            return jobDescriptorRow.triggerUrl;
                        }
                    }));
                    setColumnCssClass("triggerActionLink", IFrameworkConstants.BOOTSTRAP_COLUMN_1);
                    setColumnValueCssClass("triggerActionLink", IFrameworkConstants.BOOTSTRAP_TEXT_ALIGN_RIGHT);

                }
            };
        }

        public String id;
        public String name;
        public String description;
        public String executionTime;
        public Frequency frequency;
        public String triggerUrl;

        /**
         * Construct a row with a JobDescriptor.
         * 
         * @param jobDescriptor
         *            the job descriptor
         * @param i18nMessagePlugin
         *            the i18 messages manager
         */
        public JobDescriptorRow(IJobDescriptor jobDescriptor, II18nMessagesPlugin i18nMessagePlugin) {
            this.id = jobDescriptor.getId();
            this.name = jobDescriptor.getName(i18nMessagePlugin.getCurrentLanguage().getCode());
            this.description = jobDescriptor.getDescription(i18nMessagePlugin.getCurrentLanguage().getCode());
            this.executionTime = String.format("%02d", jobDescriptor.getStartHour()) + "h" + String.format("%02d", jobDescriptor.getStartMinute());
            this.frequency = jobDescriptor.getFrequency();
            this.triggerUrl = jobDescriptor.getTriggerUrl();

        }
    }

}
