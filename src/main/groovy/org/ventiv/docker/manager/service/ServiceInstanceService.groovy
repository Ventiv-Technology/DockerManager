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

import com.github.dockerjava.api.command.EventCallback
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.Event
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.event.ContainerRemovedEvent
import org.ventiv.docker.manager.event.ContainerStartedEvent
import org.ventiv.docker.manager.event.ContainerStoppedEvent
import org.ventiv.docker.manager.event.CreateContainerEvent
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.configuration.EligibleServiceConfiguration
import org.ventiv.docker.manager.model.configuration.ServerConfiguration

import javax.annotation.PostConstruct
import javax.annotation.Resource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
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

    private ConcurrentHashMap<String, List<ServiceInstance>> allServiceInstances = new ConcurrentHashMap<>();
    private Map<String, ExecutorService> eventExecutors = [:];
    private Map<String, DockerEventCallback> eventCallbacks = [:];
    private ScheduledFuture scheduledTask;

    @PostConstruct
    public void initialize() {
        if (eventExecutors)
            eventExecutors.each { k, v -> v.shutdownNow() }

        getAllHosts().each(this.&initializeServerConfiguration);

        if (props.dockerServerReconnectDelay > 0)
            scheduledTask = taskScheduler.scheduleAtFixedRate(this, props.dockerServerReconnectDelay);
    }

    @CompileDynamic
    public List<ServerConfiguration> getAllHosts() {
        return (List<ServerConfiguration>) environmentConfigurationService.getActiveEnvironments()*.getServers().flatten().findAll { it }.unique { it.getHostname() }
    }

    public List<ServiceInstance> getServiceInstances(ServerConfiguration serverConfiguration) {
        return allServiceInstances[getServerConfigurationKey(serverConfiguration)];
    }

    public Collection<ServiceInstance> getServiceInstances() {
        return allServiceInstances.values().flatten();
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
        String serverConfigurationKey = getServerConfigurationKey(serverConfiguration);

        if (eventCallbacks.containsKey(serverConfigurationKey)) {
            eventCallbacks[serverConfigurationKey].stop();
            eventCallbacks.remove(serverConfigurationKey);
        }

        if (eventExecutors.containsKey(serverConfigurationKey)) {
            eventExecutors[serverConfigurationKey].shutdownNow();
            eventExecutors.remove(serverConfigurationKey)
        }

        if (allServiceInstances.containsKey(serverConfigurationKey))
            allServiceInstances.remove(serverConfigurationKey);

        // First, lets query for all containers that exist on this host
        List<Container> hostContainers = dockerService.getDockerClient(serverConfiguration.getHostname()).listContainersCmd().withShowAll(true).exec()
        allServiceInstances.put(serverConfigurationKey, hostContainers.collect { createServiceInstance(serverName: serverConfiguration.getHostname()).withDockerContainer(it) })

        // Now, lets hook up to the Docker Events API
        DockerEventCallback callback = new DockerEventCallback(serverConfiguration, this)
        eventExecutors.put(serverConfigurationKey, dockerService.getDockerClient(serverConfiguration.getHostname()).eventsCmd(new DockerEventCallback(serverConfiguration, this)).exec());
        eventCallbacks.put(serverConfigurationKey, callback);
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

    @Override
    void run() {
        getAllHosts().each(this.&initializeServerConfiguration);
    }

    public static final class DockerEventCallback implements EventCallback {

        boolean running = true;
        ServerConfiguration serverConfiguration;
        ServiceInstanceService serviceInstanceService;

        public DockerEventCallback(ServerConfiguration serverConfiguration, ServiceInstanceService serviceInstanceService) {
            this.serverConfiguration = serverConfiguration;
            this.serviceInstanceService = serviceInstanceService;
        }

        @Override
        void onEvent(Event event) {
            log.debug("Received Docker Event: $event");

            // We don't care about Image Events
            if (["untag", "delete", "pull"].contains(event.getStatus()))
                return;

            // And we don't care about the following Container events
            if (["export", "kill", "pause", "restart", "unpause", "stop"].contains(event.getStatus()))
                return;

            String serverConfigurationKey = getServerConfigurationKey(serverConfiguration);
            ServiceInstance serviceInstance = null;  // Will be cretaed

            // Remove the service instance from the list, if it existed
            List<ServiceInstance> allServiceInstances = serviceInstanceService.allServiceInstances.get(serverConfigurationKey);
            ServiceInstance previousServiceInstance = allServiceInstances.find { it.getContainerId() == event.getId() }
            allServiceInstances.remove(previousServiceInstance);

            // Add the new one back
            if (event.getStatus() != "destroy") {
                InspectContainerResponse inspectContainerResponse = serviceInstanceService.dockerService.getDockerClient(serverConfiguration.getHostname()).inspectContainerCmd(event.getId()).exec()
                serviceInstance = serviceInstanceService.createServiceInstance(serverName: serverConfiguration.getHostname()).withDockerContainer(inspectContainerResponse);

                allServiceInstances << serviceInstance;
            }

            // Publish the event
            ApplicationEvent eventToPublish = null;
            if (event.getStatus() == "create")
                eventToPublish = new CreateContainerEvent(serviceInstance, serviceInstance.getResolvedEnvironmentVariables())
            else if (event.getStatus() == "destroy")
                eventToPublish = new ContainerRemovedEvent(previousServiceInstance);
            else if (event.getStatus() == "die")
                eventToPublish = new ContainerStoppedEvent(serviceInstance);
            else if (event.getStatus() == "start")
                eventToPublish = new ContainerStartedEvent(serviceInstance);

            if (eventToPublish)
                serviceInstanceService.eventPublisher.publishEvent(eventToPublish);
        }

        @Override
        void onException(Throwable throwable) {
            log.error("Error from Docker Event Listener, attempting reconnect", throwable)
            serviceInstanceService.initializeServerConfiguration(serverConfiguration);
        }

        @Override
        void onCompletion(int numEvents) {
            log.debug("Finished listening for events.  Received $numEvents events");
        }

        @Override
        boolean isReceiving() {
            return running
        }

        void stop() {
            running = false;
        }
    }


}
