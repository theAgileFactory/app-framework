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

import java.util.List;

/**
 * The interface for the jobs service.
 * 
 * @author Johann Kohler
 */
public interface IJobsService {

    public static final String NAME = "jobsService";

    /**
     * Start the jobs, including the schedulers.
     * 
     * @param the
     *            list of jobs
     */
    public void start(List<IJobDescriptor> jobs);

    /**
     * Cancell all schedulers.
     */
    public void cancel();

    /**
     * Get the jobs.
     */
    public List<IJobDescriptor> getJobs();

    /**
     * Trigger a job.
     * 
     * @param id
     *            the job id
     */
    public void trigger(String id);

}
