package org.ventiv.docker.manager.event

/**
 * Created by jcrygier on 6/20/16.
 */
class DeploymentCancelledEvent extends AbstractApplicationEvent {

    DeploymentScheduledEvent deploymentScheduledEvent;

    DeploymentCancelledEvent(String tierName, String environmentName, String applicationId, DeploymentScheduledEvent deploymentScheduledEvent) {
        super(tierName, environmentName, applicationId)

        this.deploymentScheduledEvent;
    }

}
