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
package org.ventiv.docker.manager.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.ContainerPort
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Ports
import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.model.configuration.ApplicationConfiguration
import org.ventiv.docker.manager.model.configuration.EligibleServiceConfiguration
import org.ventiv.docker.manager.model.configuration.EnvironmentConfiguration
import org.ventiv.docker.manager.model.configuration.ServiceConfiguration
import org.ventiv.docker.manager.service.EnvironmentConfigurationService
import org.ventiv.docker.manager.utils.DockerUtils

import javax.annotation.Nullable

/**
 * An instantiated instance of a service, that has been assigned to a host / list of ports.  In order to keep track
 * and not have to persist this in a database, we will use the Docker Name in a special format.  This format is as follows:
 *
 * <tierName>.<environmentName>.<applicationId>.<service>.<instanceNumber>
 */
@ToString
class ServiceInstance {

    public static final def DOCKER_NAME_PATTERN = /([a-zA-Z0-9][a-zA-Z0-9_-]*)\.([a-zA-Z0-9][a-zA-Z0-9_-]*)\.([a-zA-Z0-9][a-zA-Z0-9_-]*)\.([a-zA-Z0-9][a-zA-Z0-9_-]*)\.([0-9]*)/

    @JsonIgnore private EnvironmentConfigurationService environmentConfigurationService;
    @JsonIgnore private DockerServiceConfiguration dockerServiceConfiguration;
    public ServiceInstance(EnvironmentConfigurationService environmentConfigurationService, DockerServiceConfiguration dockerServiceConfiguration) {
        this.dockerServiceConfiguration = dockerServiceConfiguration;
        this.environmentConfigurationService = environmentConfigurationService;
    }

    public ServiceInstance(Map<String, ?> values) {
        values.each { k, v ->
            this."$k" = v;
        }
    }

    String tierName;
    String environmentName;
    String environmentDescription;
    String applicationId;
    String applicationDescription;
    String name;
    String serviceDescription;
    String serverName;

    /**
     * Derived URL for this service instance, if the ServiceConfiguration has a 'url' defined.
     */
    String url;
    Integer instanceNumber
    Status status;
    Date containerStatusTime;
    String containerId;
    DockerTag containerImage;
    Date containerCreatedDate;
    Collection<String> availableVersions;
    boolean buildPossible = false;
    boolean newBuildPossible = false;

    @Nullable   // Is null if information is received from 'docker ps' (list containers)
    String containerImageId;

    List<PortDefinition> portDefinitions;

    Map<String, String> resolvedEnvironmentVariables;

    Map<String, Object> additionalMetrics;

    @JsonIgnore
    ServiceInstanceThumbnail serviceInstanceThumbnail;

    public ServiceInstance setDockerName(String dockerName) {
        def matcher = dockerName =~ DOCKER_NAME_PATTERN;
        if (matcher) {
            tierName = matcher[0][1];
            environmentName = matcher[0][2];
            applicationId = matcher[0][3];
            name = matcher[0][4];
            instanceNumber = Integer.parseInt(matcher[0][5]);

            // Populate the Application Description
            EnvironmentConfiguration environmentConfiguration = environmentConfigurationService?.getEnvironment(tierName, environmentName);
            ApplicationConfiguration applicationConfiguration = environmentConfiguration?.getApplications()?.find { it.getId() == applicationId }

            environmentDescription = environmentConfiguration?.getDescription();
            applicationDescription = applicationConfiguration?.getDescription();

            if (!portDefinitions)
                setPortsFromConfiguration();
        } else {
            name = dockerName.substring(1);
        }

        return this;
    }

    public ServiceInstance withDockerContainer(Container dockerContainer) {
        String dockerContainerName = dockerContainer.getNames().sort { it.count("/") }.first()      // Get the name w/ the least number of slashes (to coutneract aliases into linked containers)
        this.setDockerName(dockerContainerName)
        this.status = dockerContainer.getStatus().startsWith("Up") ? Status.Running : Status.Stopped;
        //this.containerStatus = dockerContainer.getStatus();
        this.containerStatusTime = DockerUtils.convertPsStatusToDate(dockerContainer.getStatus());
        this.containerId = dockerContainer.getId();
        this.containerImage = new DockerTag(dockerContainer.getImage());
        this.containerCreatedDate = new Date(dockerContainer.getCreated() * 1000);

        // Get the service configuration
        ServiceConfiguration serviceConfig = dockerServiceConfiguration.getServiceConfiguration(name)
        this.serviceDescription = serviceConfig?.description ?: containerImage.getRepository();

        // Determine the Port Definitions
        this.portDefinitions = dockerContainer.getPorts().collect { ContainerPort port ->
            return new PortDefinition([
                    portType: serviceConfig?.containerPorts?.find { it.port == port.getPrivatePort() }?.type,
                    hostPort: port.getPublicPort(),
                    containerPort: port.getPrivatePort()
            ])
        }

        // Determine the URL
        determineUrl(serviceConfig);

        return this;
    }

    @CompileStatic
    public ServiceInstance withDockerContainer(InspectContainerResponse inspectContainerResponse) {
        this.setDockerName(inspectContainerResponse.getName());
        this.status = inspectContainerResponse.getState().getRunning() ? Status.Running : Status.Stopped
        this.containerStatusTime = this.status == Status.Running ? DockerUtils.convertDockerDate(inspectContainerResponse.getState().getStartedAt()) : DockerUtils.convertDockerDate(inspectContainerResponse.getState().getFinishedAt());
        //this.containerStatus = this.status == Status.Running ? DockerUtils.getStatusTime(inspectContainerResponse.getState().getStartedAt()) : "Exited (${inspectContainerResponse.getState().getExitCode()}) " + DockerUtils.getStatusTime(inspectContainerResponse.getState().getFinishedAt())
        this.containerId = inspectContainerResponse.getId();
        this.containerImage = new DockerTag(inspectContainerResponse.getConfig().getImage());
        this.containerCreatedDate = DockerUtils.convertDockerDate(inspectContainerResponse.getCreated())
        this.containerImageId = inspectContainerResponse.getImageId();

        // Get the service configuration
        ServiceConfiguration serviceConfig = dockerServiceConfiguration.getServiceConfiguration(name)
        this.serviceDescription = serviceConfig?.description ?: containerImage.getRepository();

        // We have environment variables, populate em!
        this.resolvedEnvironmentVariables = inspectContainerResponse.getConfig().getEnv().collectEntries { String parts = it.split('='); return [ parts[0], parts[1] ] }

        // Determine the Port Definitions
        this.portDefinitions = inspectContainerResponse.getHostConfig()?.getPortBindings()?.getBindings()?.collect { ExposedPort containerPort, Ports.Binding[] bindings ->
            return new PortDefinition([
                    portType: serviceConfig?.containerPorts?.find { it.port == containerPort.getPort() }?.type,
                    hostPort: bindings[0].getHostPort(),
                    containerPort: containerPort.getPort()
            ])
        }

        // Determine URL
        determineUrl(serviceConfig);

        return this;
    }

    public void setPortsFromConfiguration() {
        ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(getName());
        EligibleServiceConfiguration eligibleServiceConfiguration = environmentConfigurationService.getEligibleServiceConfiguration(this);
        if (eligibleServiceConfiguration) {
            this.portDefinitions = eligibleServiceConfiguration.portMappings.collect { portMapping ->
                return new PortDefinition([
                        portType: portMapping.type,
                        hostPort: portMapping.port,
                        containerPort: serviceConfiguration.containerPorts.find { it.type == portMapping.type }.port
                ])
            }
        }
    }

    public String getContainerStatus() {
        return DockerUtils.getStatusTime(getContainerStatusTime());
    }

    @CompileStatic
    private void determineUrl(ServiceConfiguration serviceConfig) {
        if (serviceConfig?.getUrl()) {
            if (this.status == Status.Running) {
                Map<String, Integer> ports = getPortDefinitions()?.collectEntries { PortDefinition portDefinition ->
                    [portDefinition.getPortType(), portDefinition.getHostPort()]
                }

                Binding b = new Binding([server: getServerName(), port: ports]);
                GroovyShell sh = new GroovyShell(b);
                setUrl(sh.evaluate('"' + serviceConfig.getUrl() + '"').toString());
            } else if (getStatus() == Status.Stopped)
                setUrl(getServiceDescription() + " is Stopped")
            else
                setUrl(getServiceDescription() + " is Missing")
        }
    }

    @Override
    public String toString() {
        return "${tierName}.${environmentName}.${applicationId}.${name}.${instanceNumber}"
    }

    @JsonIgnore
    public Map<String, Object> getTemplateBindings() {
        return [
                instance: this,
                server: getServerName(),
                port: getPortDefinitions().collectEntries {
                    return [it.getPortType(), it.getHostPort()]
                }
        ]
    }


    public static final enum Status {
        Available, Running, Stopped
    }

}
