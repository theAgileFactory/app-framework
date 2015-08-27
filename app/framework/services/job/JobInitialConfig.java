package framework.services.job;

import java.util.List;

/**
 * A value holder for the initialization of the service {@link IJobsService}.
 * <br/>
 * This one is injected at startup.
 * 
 * @author Pierre-Yves Cloux
 */
public class JobInitialConfig {
    private List<IJobDescriptor> jobs;

    public JobInitialConfig(List<IJobDescriptor> jobs) {
        super();
        this.jobs = jobs;
    }

    public List<IJobDescriptor> getJobs() {
        return jobs;
    }

}
