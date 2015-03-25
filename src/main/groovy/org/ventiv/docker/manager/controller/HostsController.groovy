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
package org.ventiv.docker.manager.controller

import com.github.dockerjava.api.NotFoundException
import com.github.dockerjava.api.NotModifiedException
import com.github.dockerjava.api.command.LogContainerCmd
import com.github.dockerjava.api.model.Container
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.io.IOUtils
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.event.ContainerRemovedEvent
import org.ventiv.docker.manager.event.ContainerStartedEvent
import org.ventiv.docker.manager.event.ContainerStoppedEvent
import org.ventiv.docker.manager.model.ServerConfiguration
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.service.DockerService
import org.ventiv.docker.manager.service.EnvironmentConfigurationService

import javax.annotation.Resource
import javax.servlet.http.HttpServletResponse

/**
 * Created by jcrygier on 3/2/15.
 */
@Slf4j
@RequestMapping("/api/hosts")
@RestController
@CompileStatic
class HostsController {

    @Resource EnvironmentController environmentController;
    @Resource DockerService dockerService;
    @Resource ApplicationEventPublisher eventPublisher;
    @Resource EnvironmentConfigurationService environmentConfigurationService;

    @RequestMapping
    public def getHostDetails() {
        return getAllHosts().collect { ServerConfiguration serverConfiguration ->
            List<Container> hostContainers = null;
            String status = "Online";

            try {
                hostContainers = dockerService.getDockerClient(serverConfiguration.getHostname()).listContainersCmd().withShowAll(true).exec()
            } catch (Exception ex) {
                log.error("Unable to fetch containers from '${serverConfiguration.getHostname()}'", ex)
                status = "Disconnected"
            }

            return [
                    id: serverConfiguration.getId(),
                    description: serverConfiguration.getDescription(),
                    hostname: serverConfiguration.getHostname(),
                    status: status,
                    serviceInstances: hostContainers?.collect {
                        ServiceInstance serviceInstance = new ServiceInstance().withDockerContainer(it)
                        serviceInstance.setServerName(serverConfiguration.getHostname())
                        return serviceInstance;
                    }
            ]
        }
    }

    @RequestMapping("/{hostName}/{containerId}/stdout")
    public void getStdOutLog(@PathVariable String hostName, @PathVariable String containerId, @RequestParam(defaultValue = "0") Integer tail, HttpServletResponse response) {
        LogContainerCmd cmd = dockerService.getDockerClient(hostName).logContainerCmd(containerId).withStdOut()
        if (tail)
            cmd.withTail(tail);

        IOUtils.copy(cmd.exec(), response.getOutputStream());
    }

    @RequestMapping("/{hostName}/{containerId}/stderr")
    public void getStdErrLog(@PathVariable String hostName, @PathVariable String containerId, @RequestParam(defaultValue = "0") Integer tail, HttpServletResponse response) {
        LogContainerCmd cmd = dockerService.getDockerClient(hostName).logContainerCmd(containerId).withStdErr()
        if (tail)
            cmd.withTail(tail);

        IOUtils.copy(cmd.exec(), response.getOutputStream());
    }

    /**
     * Stops a container that is already running.
     *
     * @param hostName
     * @param containerId
     */
    @RequestMapping(value = "/{hostName}/{containerId}/stop", method = RequestMethod.POST)
    public void stopContainer(@PathVariable String hostName, @PathVariable String containerId) {
        dockerService.getDockerClient(hostName).stopContainerCmd(containerId).exec();
        eventPublisher.publishEvent(new ContainerStoppedEvent(getServiceInstance(hostName, containerId)))
    }

    /**
     * Starts a container that has already been created.  NOTE: This container should have been started already with
     * the correct binding, since this command will not rebind volumes or ports.
     *
     * @param hostName
     * @param containerId
     */
    @RequestMapping(value = "/{hostName}/{containerId}/start", method = RequestMethod.POST)
    public void startContainer(@PathVariable String hostName, @PathVariable String containerId) {
        dockerService.getDockerClient(hostName).startContainerCmd(containerId).exec();
        eventPublisher.publishEvent(new ContainerStartedEvent(getServiceInstance(hostName, containerId)))
    }

    /**
     * Re-Starts a container that has already been created.  NOTE: This container should have been started already with
     * the correct binding, since this command will not rebind volumes or ports.
     *
     * @param hostName
     * @param containerId
     */
    @RequestMapping(value = "/{hostName}/{containerId}/restart", method = RequestMethod.POST)
    public void restartContainer(@PathVariable String hostName, @PathVariable String containerId) {
        dockerService.getDockerClient(hostName).restartContainerCmd(containerId).exec();
        eventPublisher.publishEvent(new ContainerStartedEvent(getServiceInstance(hostName, containerId)))
    }

    /**
     * Stops and removes a container.
     *
     * @param hostName
     * @param containerId
     */
    @RequestMapping(value = "/{hostName}/{containerId}/remove", method = RequestMethod.POST)
    public void removeContainer(@PathVariable String hostName, @PathVariable String containerId) {
        try {
            stopContainer(hostName, containerId);
        } catch (NotModifiedException ignored) {}       // This happens if the container is already stopped

        // Need to get the service instance BEFORE we destroy it - after it's stopped....so we event with the last known state
        ServiceInstance serviceInstance = getServiceInstance(hostName, containerId);

        dockerService.getDockerClient(hostName).removeContainerCmd(containerId).withForce().exec();

        eventPublisher.publishEvent(new ContainerRemovedEvent(serviceInstance))
    }

    public ServiceInstance getServiceInstance(String hostName, String containerId) {
        try {
            ServiceInstance answer = new ServiceInstance().withDockerContainer(dockerService.getDockerClient(hostName).inspectContainerCmd(containerId).exec());
            answer.setServerName(hostName);

            return answer;
        } catch (NotFoundException nfe) {
            return null;
        }
    }

    @CompileDynamic
    private List<ServerConfiguration> getAllHosts() {
        return (List<ServerConfiguration>) environmentConfigurationService.getAllEnvironments()*.getServers().flatten().findAll { it }.unique { it.getHostname() }
    }

}
