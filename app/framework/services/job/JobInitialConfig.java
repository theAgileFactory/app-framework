package framework.services.job;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

/**
 * A value holder for the initialization of the service {@link IJobsService}.<br/>
 * This one is injected at startup.
 * 
 * @author Pierre-Yves Cloux
 */
public class JobInitialConfig {
    private List<Pair<IJobDescriptor, Boolean>> jobs;

    public JobInitialConfig(List<Pair<IJobDescriptor, Boolean>> jobs) {
        super();
        this.jobs = jobs;
    }

    public List<Pair<IJobDescriptor, Boolean>> getJobs() {
        return jobs;
    }

}
