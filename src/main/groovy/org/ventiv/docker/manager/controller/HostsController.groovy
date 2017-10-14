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

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.LogContainerCmd
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.exception.NotModifiedException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.configuration.ApplicationConfiguration
import org.ventiv.docker.manager.model.configuration.PropertiesConfiguration
import org.ventiv.docker.manager.model.configuration.ServerConfiguration
import org.ventiv.docker.manager.model.configuration.ServiceConfiguration
import org.ventiv.docker.manager.model.configuration.ServiceInstanceConfiguration
import org.ventiv.docker.manager.security.DockerManagerPermission
import org.ventiv.docker.manager.security.SecurityUtil
import org.ventiv.docker.manager.service.DockerService
import org.ventiv.docker.manager.service.EnvironmentConfigurationService
import org.ventiv.docker.manager.service.ServiceInstanceService
import org.ventiv.docker.manager.utils.LogContainerToStreamCallback

import javax.annotation.Resource
import javax.servlet.http.HttpServletResponse
import java.util.zip.GZIPOutputStream

/**
 * Created by jcrygier on 3/2/15.
 */
@Slf4j
@RequestMapping("/api/hosts")
@RestController
@CompileStatic
class HostsController {

    @Resource DockerManagerConfiguration props;
    @Resource EnvironmentController environmentController;
    @Resource DockerService dockerService;
    @Resource ApplicationEventPublisher eventPublisher;
    @Resource EnvironmentConfigurationService environmentConfigurationService;
    @Resource ServiceInstanceService serviceInstanceService;
    @Resource DockerServiceConfiguration dockerServiceConfiguration;
    @Resource PropertiesController propertiesController;

    @RequestMapping
    public def getHostDetails() {
        List<LinkedHashMap<String, Object>> hostDetails = serviceInstanceService.getAllHosts().collect { ServerConfiguration serverConfiguration ->
            return [
                    id: serverConfiguration.getId(),
                    description: serverConfiguration.getDescription(),
                    hostname: serverConfiguration.getHostname(),
                    serviceInstances: SecurityUtil.filter(serviceInstanceService.getServiceInstances(serverConfiguration).sort { it.getContainerStatusTime() }.reverse(), DockerManagerPermission.READ),
                    availableServices: serviceInstanceService.getAvailableServiceInstances(serverConfiguration)
            ]
        }

        // Determine the missing services
        List<ApplicationConfiguration> allApplications = (List<ApplicationConfiguration>) environmentConfigurationService.getActiveEnvironments()*.getApplications().flatten();
        List<ServiceInstanceConfiguration> allServiceInstances = (List<ServiceInstanceConfiguration>) allApplications*.getServiceInstances().flatten().findAll { it != null };
        List<String> allRequiredServices = (List<String>) allServiceInstances.collect { [it?.getType()] * it?.getCount() }?.flatten()
        List<String> missingServices = new ArrayList(allRequiredServices)
        List<ServiceInstance> createdInstances = hostDetails*.get('serviceInstances').flatten() as List<ServiceInstance>
        createdInstances.each {
            missingServices.remove(it.getName())
        }

        return [
                hostDetails: hostDetails,
                missingServices: missingServices
        ];
    }

    @PreAuthorize("hasPermission(#containerId, 'LOGS')")
    @RequestMapping("/{hostName}/{containerId}/stdout")
    public void getStdOutLog(@PathVariable String hostName, @PathVariable String containerId, @RequestParam(defaultValue = "0") Integer tail, HttpServletResponse response) {
        LogContainerCmd cmd = dockerService.getDockerClient(hostName).logContainerCmd(containerId).withStdOut(true)
        if (tail)
            cmd.withTail(tail);

        LogContainerToStreamCallback callback = new LogContainerToStreamCallback(response.getOutputStream());
        cmd.exec(callback);

        callback.awaitCompletion();
    }

    @PreAuthorize("hasPermission(#containerId, 'LOGS')")
    @RequestMapping("/{hostName}/{containerId}/stderr")
    public void getStdErrLog(@PathVariable String hostName, @PathVariable String containerId, @RequestParam(defaultValue = "0") Integer tail, HttpServletResponse response) {
        LogContainerCmd cmd = dockerService.getDockerClient(hostName).logContainerCmd(containerId).withStdErr(true)
        if (tail)
            cmd.withTail(tail);

        LogContainerToStreamCallback callback = new LogContainerToStreamCallback(response.getOutputStream());
        cmd.exec(callback);

        callback.awaitCompletion();
    }

    @PreAuthorize("hasPermission(#containerId, 'LOGS')")
    @RequestMapping("/{hostName}/{containerId}/logs")
    public void getLogs(@PathVariable String hostName, @PathVariable String containerId, @RequestParam(defaultValue = "0") Integer tail, HttpServletResponse response) {
        LogContainerCmd cmd = dockerService.getDockerClient(hostName).logContainerCmd(containerId).withStdOut(true).withStdErr(true)
        if (tail)
            cmd.withTail(tail);

        LogContainerToStreamCallback callback = new LogContainerToStreamCallback(response.getOutputStream());
        cmd.exec(callback);

        callback.awaitCompletion();
    }

    /**
     * Stops a container that is already running.
     *
     * @param hostName
     * @param containerId
     */
    @PreAuthorize("hasPermission(#containerId, 'STOP')")
    @RequestMapping(value = "/{hostName}/{containerId}/stop", method = RequestMethod.POST)
    public void stopContainer(@PathVariable String hostName, @PathVariable String containerId) {
        log.info("Stopping container: ${containerId} on ${hostName}")
        dockerService.getDockerClient(hostName).stopContainerCmd(containerId).exec();
        //eventPublisher.publishEvent(new ContainerStoppedEvent(getServiceInstance(hostName, containerId)))
    }

    /**
     * Starts a container that has already been created.  NOTE: This container should have been started already with
     * the correct binding, since this command will not rebind volumes or ports.
     *
     * @param hostName
     * @param containerId
     */
    @PreAuthorize("hasPermission(#containerId, 'START')")
    @RequestMapping(value = "/{hostName}/{containerId}/start", method = RequestMethod.POST)
    public void startContainer(@PathVariable String hostName, @PathVariable String containerId) {
        pushPropertiesFilesToServiceInstance(getServiceInstance(hostName, containerId));

        dockerService.getDockerClient(hostName).startContainerCmd(containerId).exec();
        //eventPublisher.publishEvent(new ContainerStartedEvent(getServiceInstance(hostName, containerId)))
    }

    /**
     * Re-Starts a container that has already been created.  NOTE: This container should have been started already with
     * the correct binding, since this command will not rebind volumes or ports.
     *
     * @param hostName
     * @param containerId
     */
    @PreAuthorize("hasPermission(#containerId, 'RESTART')")
    @RequestMapping(value = "/{hostName}/{containerId}/restart", method = RequestMethod.POST)
    public void restartContainer(@PathVariable String hostName, @PathVariable String containerId) {
        pushPropertiesFilesToServiceInstance(getServiceInstance(hostName, containerId));

        dockerService.getDockerClient(hostName).restartContainerCmd(containerId).exec();
        //eventPublisher.publishEvent(new ContainerStartedEvent(getServiceInstance(hostName, containerId)))
    }

    /**
     * Stops and removes a container.
     *
     * @param hostName
     * @param containerId
     */
    @PreAuthorize("hasPermission(#containerId, 'REMOVE')")
    @RequestMapping(value = "/{hostName}/{containerId}/remove", method = RequestMethod.POST)
    public void removeContainer(@PathVariable String hostName, @PathVariable String containerId) {
        try {
            stopContainer(hostName, containerId);
        } catch (NotModifiedException ignored) {}       // This happens if the container is already stopped

        log.info("Removing container: ${containerId} on ${hostName}")
        dockerService.getDockerClient(hostName).removeContainerCmd(containerId).withForce(true).exec();
    }

    public void pushPropertiesFilesToServiceInstance(ServiceInstance serviceInstance) {
        ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(serviceInstance.getName());
        DockerClient docker = dockerService.getDockerClient(serviceInstance.getServerName());

        if (serviceConfiguration.getProperties()) {
            serviceConfiguration.getProperties()
                    .findAll { it.getMethod() == PropertiesConfiguration.PropertiesMethod.File }
                    .each { propertyConfig ->
                String propertyFileContents = SecurityUtil.doWithSuperUser {
                    propertiesController.getEnvironmentPropertiesText(serviceInstance.getTierName(), serviceInstance.getEnvironmentName(), serviceInstance.getApplicationId(), serviceConfiguration.getName(), propertyConfig.getSetId())
                }

                TarArchiveEntry propFile = new TarArchiveEntry(new File(propertyConfig.getLocation()).getName());
                propFile.setSize(propertyFileContents.size());

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                TarArchiveOutputStream gzout = new TarArchiveOutputStream(new GZIPOutputStream(baos));
                gzout.putArchiveEntry(propFile);
                gzout.write(propertyFileContents.getBytes("UTF-8"));
                gzout.closeArchiveEntry();
                gzout.flush();
                gzout.close();

                docker.copyArchiveToContainerCmd(serviceInstance.getContainerId())
                        .withRemotePath(new File(propertyConfig.getLocation()).getParentFile().getAbsolutePath())
                        .withTarInputStream(new ByteArrayInputStream(baos.toByteArray()))
                        .exec();
            }
        }
    }

    public ServiceInstance getServiceInstance(String hostName, String containerId) {
        try {
            ServiceInstance answer = serviceInstanceService.createServiceInstance().withDockerContainer(dockerService.getDockerClient(hostName).inspectContainerCmd(containerId).exec());
            answer.setServerName(hostName);

            return answer;
        } catch (NotFoundException nfe) {
            return null;
        }
    }
}
