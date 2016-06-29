package org.ventiv.docker.manager.event

import org.ventiv.docker.manager.model.ApplicationDetails

import java.util.concurrent.ScheduledFuture

/**
 * Created by jcrygier on 6/20/16.
 */
class DeploymentScheduledEvent extends AbstractApplicationEvent {

    ApplicationDetails applicationDetails;
    String branch;
    Map<String, String> serviceVersions;
    String requestedVersion;
    Date requestedDeploymentDate;
    ScheduledFuture future;

    /**
     * Create a new ApplicationEvent.
     * @param source the component that published the event (never {@code null})
     */
    DeploymentScheduledEvent(ApplicationDetails applicationDetails, String branch, Map<String, String> serviceVersions, String requestedVersion, Date requestedDeploymentDate, ScheduledFuture future) {
        super(applicationDetails.getTierName(), applicationDetails.getEnvironmentName(), applicationDetails.getId())

        this.applicationDetails = applicationDetails
        this.branch = branch;
        this.serviceVersions = serviceVersions;
        this.requestedVersion = requestedVersion;
        this.requestedDeploymentDate = requestedDeploymentDate;
        this.future = future;
    }

}
