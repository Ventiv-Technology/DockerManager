/*
 * Copyright (c) 2014 - 2015 Ventiv Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.ventiv.docker.manager.service

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.jdeferred.Deferred
import org.jdeferred.DoneCallback
import org.jdeferred.FailCallback
import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationListener
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.controller.EnvironmentController
import org.ventiv.docker.manager.controller.HostsController
import org.ventiv.docker.manager.event.DeploymentFinishedEvent
import org.ventiv.docker.manager.event.DeploymentStartedEvent
import org.ventiv.docker.manager.exception.ApplicationException
import org.ventiv.docker.manager.model.ApplicationDetails
import org.ventiv.docker.manager.model.DockerTag
import org.ventiv.docker.manager.model.MissingService
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.configuration.ServiceConfiguration
import org.ventiv.docker.manager.model.configuration.ServiceInstanceConfiguration
import org.ventiv.docker.manager.service.selection.ServiceSelectionAlgorithm
import org.ventiv.docker.manager.utils.TimingUtils

import javax.annotation.Resource

/**
 * Service to handle Application Deployments
 */
@Slf4j
@CompileStatic
@Service
class ApplicationDeploymentService implements ApplicationListener<DeploymentStartedEvent> {

    @Resource EnvironmentController environmentController;
    @Resource HostsController hostsController;
    @Resource ApplicationEventPublisher eventPublisher;
    @Resource DockerRegistryApiService registryApiService;
    @Resource ServiceInstanceService serviceInstanceService;
    @Resource DockerServiceConfiguration dockerServiceConfiguration;

    private Map<String, Promise<ApplicationDetails, ApplicationException, String>> runningDeployments = [:]

    @Override
    void onApplicationEvent(DeploymentStartedEvent event) {
        String key = getKey(event.getTierName(), event.getEnvironmentName(), event.getApplicationId());
        if (isRunning(event.getTierName(), event.getEnvironmentName(), event.getApplicationId())) {
            event.setStatus(DeploymentStartedEvent.DeploymentStatus.AlreadyRunning);
        } else {
            runningDeployments[key] = startDeployment(event.getApplicationDetails(), event.getBranch(), event.getServiceVersions());
            event.setStatus(DeploymentStartedEvent.DeploymentStatus.Started);

            runningDeployments[key].done(this.&onDeploymentFinished as DoneCallback<ApplicationDetails>)
            runningDeployments[key].fail(this.&onDeploymentRejected as FailCallback<ApplicationException>)
        }
    }

    Promise<ApplicationDetails, ApplicationException, String> startDeployment(ApplicationDetails applicationDetails, String branch, Map<String, String> serviceVersions) {
        Authentication deploymentRequestor = SecurityContextHolder.getContext().getAuthentication();
        Deferred<ApplicationDetails, ApplicationException, String> deferred = new DeferredObject<>();

        Thread.start {
            SecurityContextHolder.getContext().setAuthentication(deploymentRequestor);
            TimingUtils.time("Deploy Application ${applicationDetails.getId()}") {
                try {
                    Collection<ServiceInstance> createdServiceInstances = environmentController.getServiceInstances(applicationDetails.getTierName(), applicationDetails.getEnvironmentName());

                    // First, lets sort the missing services, so we can adhere to bottom-up building (along with linking dependencies)
                    applicationDetails.getMissingServiceInstances().sort(true) { MissingService a, MissingService b ->
                        ServiceConfiguration aConfiguration = dockerServiceConfiguration.getServiceConfiguration(a.getServiceName());
                        ServiceConfiguration bConfiguration = dockerServiceConfiguration.getServiceConfiguration(b.getServiceName());

                        if (aConfiguration.getLinks()?.find { it.getContainer() == b.getServiceName() })
                            return 1;
                        else if (bConfiguration.getLinks()?.find { it.getContainer() == a.getServiceName() })
                            return -1;
                        else {
                            int index1 = applicationDetails.getApplicationConfiguration().getServiceInstances().findIndexOf { ServiceInstanceConfiguration sic ->
                                sic.type ==  a.getServiceName()
                            }
                            int index2 = applicationDetails.getApplicationConfiguration().getServiceInstances().findIndexOf { ServiceInstanceConfiguration sic ->
                                sic.type ==  b.getServiceName()
                            }
                            return index1 <=> index2
                        }
                    }

                    // Now, let's find any missing services
                    applicationDetails.getMissingServiceInstances().each { MissingService missingService ->
                        // Find an 'Available' Service Instance
                        ServiceInstance toUse = ServiceSelectionAlgorithm.Util.getAvailableServiceInstance(missingService.getServiceName(), createdServiceInstances, applicationDetails);
                        toUse.setApplicationId(applicationDetails.getId());


                        // Create (and start) the container
                        environmentController.createDockerContainer(applicationDetails, toUse, branch, serviceVersions.get(missingService.getServiceName()));

                        // Mark this service instance as 'Running' so it won't get used again
                        toUse.setStatus(ServiceInstance.Status.Running);
                    }

                    // Verify all running serviceInstances to ensure they're the correct version
                    new ArrayList<ServiceInstance>(applicationDetails.getServiceInstances()).each { ServiceInstance anInstance ->
                        if (anInstance.getStatus() != ServiceInstance.Status.Available && anInstance.getContainerImage() != null) {
                            DockerTag tag = anInstance.getContainerImage();
                            String expectedVersion = serviceVersions[anInstance.getName()];

                            try {
                                // Get the image id's for both expected as well as what's running, since images can be tagged with multiple versions (e.g. mysql:5 = mysql:5.2)
                                String expectedImageId = registryApiService.getRegistry(tag).listRepositoryTags(tag.getNamespace(), tag.getRepository())[expectedVersion];
                                String runningImageId = hostsController.getServiceInstance(anInstance.getServerName(), anInstance.getContainerId()).getContainerImageId()

                                // We have a version mismatch...destroy the container and rebuild it
                                if (expectedImageId != runningImageId) {
                                    log.info("Redeploying container ${anInstance.getContainerId()} from image ${runningImageId} to ${expectedImageId}")

                                    // First, let's destroy the container
                                    hostsController.removeContainer(anInstance.getServerName(), anInstance.getContainerId());
                                    applicationDetails.getServiceInstances().remove(anInstance);

                                    // Now, create a new one
                                    environmentController.createDockerContainer(applicationDetails, anInstance, branch, serviceVersions.get(anInstance.getName()));
                                } else {
                                    log.info("Not modifying container ${anInstance.getContainerId()} as it's already on the proper image: ${expectedImageId}")
                                }
                            } catch (Exception e) {
                                // This is okay, likely means that this image only exists locally
                                log.debug("Exception Redeploying container ${anInstance.getContainerId()} to $tag", e)
                            }
                        }
                    }

                    deferred.resolve(applicationDetails);
                } catch (Exception e) {
                    e.printStackTrace()
                    ApplicationException wrapped = new ApplicationException(applicationDetails)
                    wrapped.initCause(e);

                    deferred.reject(wrapped);
                }
            }
        }

        return deferred.promise();
    }

    private void onDeploymentFinished(ApplicationDetails applicationDetails) {
        eventPublisher.publishEvent(new DeploymentFinishedEvent(applicationDetails));
    }

    private void onDeploymentRejected(ApplicationException e) {
        log.error("Deployment failed for application ${e.getApplication().getId()}: ", e);
        eventPublisher.publishEvent(new DeploymentFinishedEvent(e.getApplication()));
    }

    boolean isRunning(String tierName, String environmentName, String applicationId) {
        String key = getKey(tierName, environmentName, applicationId)
        if (runningDeployments.containsKey(key))
            return runningDeployments[key].isPending();
        else
            return false;
    }

    private static String getKey(String tierName, String environmentName, String applicationId) {
        return "${tierName}.${environmentName}.${applicationId}"
    }

}
