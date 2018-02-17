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

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.command.InspectImageResponse
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.Event
import com.github.dockerjava.api.model.EventType
import com.github.dockerjava.core.async.ResultCallbackTemplate
import com.github.dockerjava.core.command.EventsResultCallback
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.api.DockerRegistry
import org.ventiv.docker.manager.api.DockerRegistryV1
import org.ventiv.docker.manager.api.DockerRegistryV2
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.event.ContainerRemovedEvent
import org.ventiv.docker.manager.event.ContainerStartedEvent
import org.ventiv.docker.manager.event.ContainerStoppedEvent
import org.ventiv.docker.manager.event.CreateContainerEvent
import org.ventiv.docker.manager.model.ApplicationThumbnail
import org.ventiv.docker.manager.model.DockerTag
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.ServiceInstanceThumbnail
import org.ventiv.docker.manager.model.configuration.EligibleServiceConfiguration
import org.ventiv.docker.manager.model.configuration.EnvironmentConfiguration
import org.ventiv.docker.manager.model.configuration.ServerConfiguration
import org.ventiv.docker.manager.repository.ServiceInstanceThumbnailRepository

import javax.annotation.PostConstruct
import javax.annotation.Resource
import java.lang.reflect.Field
import java.util.concurrent.ScheduledFuture

/**
 * Service to manage all of the service instances and their state, in memory.  This is much more efficient than
 * querying docker all the time.
 */
@Slf4j
@CompileStatic
@Service
class ServiceInstanceService implements Runnable {

    @Resource EnvironmentConfigurationService environmentConfigurationService;
    @Resource DockerService dockerService;
    @Resource ApplicationEventPublisher eventPublisher;
    @Resource TaskScheduler taskScheduler;
    @Resource DockerManagerConfiguration props;
    @Resource private DockerServiceConfiguration dockerServiceConfiguration;
    @Resource ServiceInstanceThumbnailRepository serviceInstanceThumbnailRepository;
    @Resource DockerRegistryApiService registryApiService;

    private Map<String, List<ServiceInstance>> allServiceInstances = [:]
    private final Map<String, DockerEventCallback> eventCallbacks = [:];
    private ScheduledFuture scheduledTask;

    @PostConstruct
    public void initialize() {
        if (eventCallbacks)
            eventCallbacks.each { k, v -> v.close() }

        // Kick off the refresh of the environment
        this.run();

        // Now, tell environemntConfigurationService that we're interested in any environment changes
        environmentConfigurationService.getEnvironmentChangeCallbacks() << { EnvironmentConfiguration environmentConfiguration ->
            environmentConfiguration.getServers()?.each(this.&initializeServerConfiguration)
        }

        if (!scheduledTask && props.dockerServerReconnectDelay > 0)
            scheduledTask = taskScheduler.scheduleAtFixedRate(this, props.dockerServerReconnectDelay);
    }

    @CompileDynamic
    public List<ServerConfiguration> getAllHosts() {
        return (List<ServerConfiguration>) environmentConfigurationService.getActiveEnvironments()*.getServers().flatten().findAll { it }.unique { it.getHostname() }
    }

    public List<ServiceInstance> getServiceInstances(ServerConfiguration serverConfiguration) {
        synchronized (allServiceInstances) {
            return allServiceInstances[getServerConfigurationKey(serverConfiguration)];
        }
    }

    public Collection<ServiceInstance> getServiceInstances() {
        synchronized (allServiceInstances) {
            return allServiceInstances.values().flatten();
        }
    }

    public ServiceInstance getServiceInstance(String containerId) {
        getServiceInstances().find { it.getContainerId().startsWith(containerId) }
    }

    public Collection<EligibleServiceConfiguration> getAvailableServiceInstances(ServerConfiguration serverConfiguration) {
        Collection<EligibleServiceConfiguration> availableServices = new ArrayList(serverConfiguration.getEligibleServices());
        getServiceInstances(serverConfiguration).each { ServiceInstance createdInstance ->
            availableServices.remove(availableServices.find { EligibleServiceConfiguration eligibleServiceConfiguration ->
                eligibleServiceConfiguration.getType() == createdInstance.getName() && eligibleServiceConfiguration.getInstanceNumber() == createdInstance.getInstanceNumber()
            });
        }

        return availableServices;
    }

    public void initializeServerConfiguration(ServerConfiguration serverConfiguration) {
        synchronized (allServiceInstances) {
            String serverConfigurationKey = getServerConfigurationKey(serverConfiguration);

            if (allServiceInstances.containsKey(serverConfigurationKey))
                allServiceInstances.remove(serverConfigurationKey);

            // First, lets query for all containers that exist on this host
            List<Container> hostContainers = dockerService.getDockerClient(serverConfiguration.getHostname()).listContainersCmd().withShowAll(true).exec()
            allServiceInstances.put(serverConfigurationKey, hostContainers.collect {
                InspectContainerResponse inspectContainerResponse = dockerService.getDockerClient(serverConfiguration.getHostname()).inspectContainerCmd(it.getId()).exec()
                createServiceInstance(serverName: serverConfiguration.getHostname()).withDockerContainer(inspectContainerResponse)
            })

            // Now, lets hook up to the Docker Events API, but only if we don't already have one running
            boolean connected = eventCallbacks.get(serverConfigurationKey)?.isConnected();
            if (!connected) {
                DockerEventCallback callback = new DockerEventCallback(serverConfiguration, this)
                dockerService.getDockerClient(serverConfiguration.getHostname()).eventsCmd().exec(callback);
                eventCallbacks.put(serverConfigurationKey, callback);
            }
        }
    }

    protected static String getServerConfigurationKey(ServerConfiguration serverConfiguration) {
        return serverConfiguration.getHostname();
    }

    @CompileDynamic
    public ServiceInstance createServiceInstance(Map<String, ?> values = [:]) {
        ServiceInstance answer = new ServiceInstance(environmentConfigurationService, dockerServiceConfiguration);
        values.each { k, v ->
            answer."$k" = v;
        }

        return answer;
    }

    public ServiceInstanceThumbnail getServiceInstanceThumbnail(ServiceInstance serviceInstance) {
        ServiceInstanceThumbnail thumbnail = serviceInstance.getServiceInstanceThumbnail();
        if (thumbnail == null)
            thumbnail = serviceInstanceThumbnailRepository.findServiceInstanceThumbnail(serviceInstance.getServerName(), serviceInstance.getTierName(), serviceInstance.getEnvironmentName(), serviceInstance.getApplicationId(), serviceInstance.getName(), serviceInstance.getInstanceNumber())

        if (thumbnail == null) {
            ApplicationThumbnail applicationThumbnail = environmentConfigurationService.getApplicationThumbnail(serviceInstance.getTierName(), serviceInstance.getEnvironmentName(), serviceInstance.getApplicationId());
            thumbnail = new ServiceInstanceThumbnail(application: applicationThumbnail, serverName: serviceInstance.getServerName(), name: serviceInstance.getName(), instanceNumber: serviceInstance.getInstanceNumber())
            serviceInstanceThumbnailRepository.save(thumbnail);
        }

        serviceInstance.setServiceInstanceThumbnail(thumbnail);

        return thumbnail;
    }

    @Override
    void run() {
        getAllHosts().each(this.&initializeServerConfiguration);
    }

    Map<String, DockerEventCallback> getEventCallbacks() {
        return eventCallbacks
    }
/**
     * Determines if the service instance that is passed in is using the image (by tag) that is currently deployed in
     * the corresponding registry.  This is helpful if an image is overwriten in the registry.  This commonly occurs with
     * 'latest' images.
     *
     * @param serviceInstance
     * @return
     */
    boolean isImageDeployedMatchRegistry(ServiceInstance serviceInstance, String expectedTag) {
        DockerTag tag = serviceInstance.getContainerImage();
        String host = serviceInstance.getServerName();

        serviceInstance = createServiceInstance().withDockerContainer(dockerService.getDockerClient(host).inspectContainerCmd(serviceInstance.getContainerId()).exec());
        serviceInstance.setServerName(host);

        String runningImageId = serviceInstance.getContainerImageId();

        DockerRegistry registry = registryApiService.getRegistry(tag);
        if (registry instanceof DockerRegistryV1) {
            String registryImageId = registry.listRepositoryTags(tag.namespace, tag.repository)[tag.tag];
            return registryImageId == runningImageId;
        } else if (registry instanceof DockerRegistryV2) {
            InspectImageResponse imageDetails = dockerService.getDockerClient(host).inspectImageCmd(runningImageId).exec();

            DockerRegistryV2.ImageManifest registryImageManifest;
            try {
                registryImageManifest = registry.getImageManifest(tag.namespace, tag.repository, expectedTag);
            } catch (def ignored) {
                // Artifactory v2 requires no namespace, but Docker Hub requires namespace.....ugh
                registryImageManifest = registry.getImageManifest(tag.repository, expectedTag);
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> v1History = mapper.readValue(registryImageManifest.history[0].v1Compatibility.toString(), Map);

            return imageDetails.container == v1History.container
        }

        return false
    }

    public static final class DockerEventCallback extends EventsResultCallback {

        boolean running = true;
        ServerConfiguration serverConfiguration;
        ServiceInstanceService serviceInstanceService;
        Field closedField;

        public DockerEventCallback(ServerConfiguration serverConfiguration, ServiceInstanceService serviceInstanceService) {
            this.serverConfiguration = serverConfiguration;
            this.serviceInstanceService = serviceInstanceService;

            closedField = ResultCallbackTemplate.class.getDeclaredField("closed");
            closedField.setAccessible(true);
        }

        @Override
        public RuntimeException getFirstError() {
            return super.getFirstError()
        }

        public boolean isConnected() {
            return running && !closedField.get(this);
        }

        public Throwable getException() {
            try {
                this.getFirstError();
            } catch (Throwable e) {
                return e;
            }
        }

        @Override
        void onNext(Event event) {
            // Okay not to call super, it only logs....
            log.debug("Received Docker Event: $event");

            // We really only care about container type events, not NETWORK, or VOLUME, etc...
            if (event.getType() != EventType.CONTAINER)
                return;

            // We don't care about Image Events
            if (["untag", "delete", "pull"].contains(event.getStatus()))
                return;

            // Don't care about exec events
            if (event.getStatus() && event.getStatus().startsWith("exec"))
                return;

            // And we don't care about the following Container events
            if (["export", "kill", "pause", "restart", "unpause", "stop"].contains(event.getStatus()))
                return;

            synchronized (serviceInstanceService.allServiceInstances) {
                String serverConfigurationKey = getServerConfigurationKey(serverConfiguration);
                ServiceInstance serviceInstance = null;  // Will be cretaed

                // Remove the service instance from the list, if it existed
                List<ServiceInstance> allServiceInstances = serviceInstanceService.allServiceInstances.get(serverConfigurationKey);
                ServiceInstance previousServiceInstance = allServiceInstances.find {
                    it.getContainerId() == event.getId()
                }

                // Add the new one back
                if (event.getStatus() != "destroy" && event.getId()) {
                    try {
                        InspectContainerResponse inspectContainerResponse = serviceInstanceService.dockerService.getDockerClient(serverConfiguration.getHostname()).inspectContainerCmd(event.getId()).exec()
                        serviceInstance = serviceInstanceService.createServiceInstance(serverName: serverConfiguration.getHostname()).withDockerContainer(inspectContainerResponse);

                        allServiceInstances.remove(previousServiceInstance);
                        allServiceInstances << serviceInstance;
                    } catch (NotFoundException ignored) {
                        log.info("Event received from container ${event.getId()}, but it must have been destroyed before we could read the status.  Ignoring the container...")
                        return;
                    }
                } else {
                    allServiceInstances.remove(previousServiceInstance);
                }

                // Publish the event
                ApplicationEvent eventToPublish = null;
                if (event.getStatus() == "create")
                    eventToPublish = new CreateContainerEvent(serviceInstance, serviceInstance.getResolvedEnvironmentVariables())
                else if (event.getStatus() == "destroy" && previousServiceInstance)
                    eventToPublish = new ContainerRemovedEvent(previousServiceInstance);
                else if (event.getStatus() == "die")
                    eventToPublish = new ContainerStoppedEvent(serviceInstance);
                else if (event.getStatus() == "start")
                    eventToPublish = new ContainerStartedEvent(serviceInstance);

                if (eventToPublish)
                    serviceInstanceService.eventPublisher.publishEvent(eventToPublish);
            }
        }

        @Override
        public void onComplete() {
            super.onComplete();
            running = false;
            log.debug("Finished listening for events.");

            // TODO: We now know that we've disconnected from the server.  The dockerServerReconnectDelay setting will reconnect, but should we auto-try before that timeout?
        }
    }


}
