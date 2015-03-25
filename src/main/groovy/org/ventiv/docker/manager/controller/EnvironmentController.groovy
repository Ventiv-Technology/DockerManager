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
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.StartContainerCmd
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.api.model.Volume
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.event.ContainerStartedEvent
import org.ventiv.docker.manager.event.CreateContainerEvent
import org.ventiv.docker.manager.event.DeploymentStartedEvent
import org.ventiv.docker.manager.event.PullImageEvent
import org.ventiv.docker.manager.model.ApplicationConfiguration
import org.ventiv.docker.manager.model.ApplicationDetails
import org.ventiv.docker.manager.model.BuildApplicationInfo
import org.ventiv.docker.manager.model.DeployApplicationRequest
import org.ventiv.docker.manager.model.DockerTag
import org.ventiv.docker.manager.model.EligibleServiceConfiguration
import org.ventiv.docker.manager.model.EnvironmentConfiguration
import org.ventiv.docker.manager.model.MissingService
import org.ventiv.docker.manager.model.PortDefinition
import org.ventiv.docker.manager.model.ServerConfiguration
import org.ventiv.docker.manager.model.ServiceBuildInfo
import org.ventiv.docker.manager.model.ServiceConfiguration
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.ServiceInstanceConfiguration
import org.ventiv.docker.manager.service.ApplicationDeploymentService
import org.ventiv.docker.manager.service.DockerService
import org.ventiv.docker.manager.service.EnvironmentConfigurationService
import org.ventiv.docker.manager.service.PluginService

import javax.annotation.Resource

/**
 * Created by jcrygier on 2/27/15.
 */
@Slf4j
@RequestMapping("/api/environment")
@RestController
class EnvironmentController {

    @Resource DockerManagerConfiguration props;
    @Resource DockerService dockerService;
    @Resource DockerServiceConfiguration dockerServiceConfiguration;
    @Resource DockerServiceController dockerServiceController;
    @Resource HostsController hostsController;
    @Resource ApplicationEventPublisher eventPublisher;
    @Resource ApplicationDeploymentService deploymentService;
    @Resource PluginService pluginService;
    @Resource EnvironmentConfigurationService environmentConfigurationService;

    Map<String, BuildApplicationInfo> buildingApplications = [:]

    @RequestMapping
    public Map<String, List<EnvironmentConfiguration>> getTiers() {
        List<String> activeTiers = props.activeTiers;
        getAllEnvironments().findAll { k, v -> activeTiers == null || activeTiers.size() == 0 || activeTiers.contains(k) };
    }

    @RequestMapping("/{tierName}")
    public Collection<String> getEnvironments(@PathVariable("tierName") String tierName) {
        getAllEnvironments()[tierName];
    }

    @CompileStatic
    @RequestMapping("/{tierName}/{environmentName}")
    public List<ApplicationDetails> getEnvironmentDetails(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName) {
        EnvironmentConfiguration envConfiguration = environmentConfigurationService.getEnvironment(tierName, environmentName);
        List<ServiceInstance> serviceInstances = getServiceInstances(tierName, environmentName);

        return envConfiguration.getApplications().collect { ApplicationConfiguration applicationConfiguration ->
            Collection<ServiceInstance> applicationInstances = serviceInstances.findAll { it.getApplicationId() == applicationConfiguration.getId() };

            // Now make ServiceInstance objects for each one defined
            List<String> requiredServices = (List<String>) applicationConfiguration.getServiceInstances().collect { [it.getType()] * it.getCount() }.flatten()
            List<String> missingServices = new ArrayList<String>(requiredServices).reverse()        // Reverse is so we will deploy in 'bottom-up' fashion
            applicationInstances.each { missingServices.remove(it.getName())  }  // Remove any that actually exist

            // Derive the URL
            String url = applicationConfiguration.getUrl();
            if (applicationConfiguration.getServiceInstanceUrl()) {
                url = applicationInstances.find { it.getName() == applicationConfiguration.getServiceInstanceUrl() }?.getUrl()
            }

            // Version derivation
            Collection<ServiceInstance> nonPinnedVersions = applicationInstances.findAll { ServiceInstance serviceInstance ->
                ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(serviceInstance.getName());
                return serviceConfiguration.getPinnedVersion() == null;
            }
            Collection<String> versionsDeployed = nonPinnedVersions*.getContainerImage()*.getTag().unique();

            return new ApplicationDetails([
                    tierName: tierName,
                    environmentName: environmentName,
                    url: url,
                    serviceInstances: applicationInstances,
                    missingServiceInstances: missingServices.collect { new MissingService([serviceName: it, serviceDescription: dockerServiceConfiguration.getServiceConfiguration(it).getDescription()]) },
                    buildStatus: buildingApplications.get("$tierName.$environmentName.${applicationConfiguration.getId()}")?.getBuildStatus(),
                    deploymentInProgress: deploymentService.isRunning(tierName, environmentName, applicationConfiguration.getId()),
                    version: versionsDeployed.join(", ")
            ]).withApplicationConfiguration(applicationConfiguration)
        }
    }

    @RequestMapping("/{tierName}/{environmentName}/app/{applicationId}/versions")
    public Collection<Map<String, String>> getApplicationVersions(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName, @PathVariable("applicationId") String applicationId) {
        EnvironmentConfiguration envConfiguration = environmentConfigurationService.getEnvironment(tierName, environmentName);
        ApplicationConfiguration appConfiguration = envConfiguration.getApplications().find { it.getId() == applicationId };
        Collection<ServiceConfiguration> allServiceConfigurations = appConfiguration.getServiceInstances()*.getType().unique().collect { dockerServiceConfiguration.getServiceConfiguration(it); }
        Collection<ServiceConfiguration> nonPinnedServices = allServiceConfigurations.findAll { it.getPinnedVersion() == null };

        Collection<List<String>> versions = nonPinnedServices.collect { it.getPossibleVersions() }.unique()

        // Create an option for the new build - if applicable
        Map<String, String> newBuildOption = nonPinnedServices.any { it.isNewBuildPossible() } ? [id: "BuildNewVersion", text: "New Build"] : [:]

        if (versions.size() == 1) {
            return [newBuildOption] + versions[0].collect {
                return [id: it, text: it]
            }
        }

        return [ newBuildOption ];
    }

    /**
     * Builds out an application.  Ensures the docker environment of two things:
     * 1.) Any missingServiceInstances (see getEnvironmentDetails) will be built out according to the requested versions
     * 2.) Each serviceInstance (see getEnvironmentDetails) is on the proper version.  If not, it will destroy the container and rebuild.
     */
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RequestMapping(value = "/{tierName}/{environmentName}", method = RequestMethod.POST)
    public void deployApplication(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName, @RequestBody DeployApplicationRequest deployRequest) {
        List<ApplicationDetails> environmentDetails = getEnvironmentDetails(tierName, environmentName);
        ApplicationDetails applicationDetails = environmentDetails.find { it.getId() == deployRequest.getName() }

        // First, Build the application
        buildApplication(applicationDetails, deployRequest.getServiceVersions()).onSuccessfulBuild { ApplicationDetails builtApplication ->
            // The following does 2 things: 1.) Sends a message to the UI that a deployment is now going, and 2.) Gets Picked up by ApplicationDeploymentService to do the actual deployment
            eventPublisher.publishEvent(new DeploymentStartedEvent(applicationDetails, applicationDetails.getBuildServiceVersionsTemplate()))
        }
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @RequestMapping(value = "/{tierName}/{environmentName}/app/{applicationId}/{version}", method = RequestMethod.POST)
    public void deployApplication(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName, @PathVariable("applicationId") String applicationId, @PathVariable("version") String version) {
        EnvironmentConfiguration envConfiguration = environmentConfigurationService.getEnvironment(tierName, environmentName);
        ApplicationConfiguration appConfiguration = envConfiguration.getApplications().find { it.getId() == applicationId };
        Collection<ServiceConfiguration> allServiceConfigurations = appConfiguration.getServiceInstances()*.getType().unique().collect { dockerServiceConfiguration.getServiceConfiguration(it); }

        // Create the template with the pinned versions
        Map<String, String> serviceVersions = allServiceConfigurations.findAll { it.getPinnedVersion() != null }.collectEntries { [it.getName(), it.getPinnedVersion()] };

        // Fill in the rest with the passed in version
        allServiceConfigurations.findAll { it.getPinnedVersion() == null }.each {
            if (version == null)
                throw new IllegalArgumentException("Version is required when there are unpinned services");

            serviceVersions.put(it.getName(), version);
        }

        // Finally, call the deploy
        deployApplication(tierName, environmentName, new DeployApplicationRequest(name: applicationId, serviceVersions: serviceVersions));
    }

    @CompileStatic
    @RequestMapping("/{tierName}/{environmentName}/serviceInstances")
    public List<ServiceInstance> getServiceInstances(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName) {
        EnvironmentConfiguration envConfiguration = environmentConfigurationService.getEnvironment(tierName, environmentName);

        List<ServiceInstance> definedServiceInstances = [];
        envConfiguration.getServers().each { ServerConfiguration serverConf ->
            List<Container> containers = dockerService.getDockerClient(serverConf.getHostname()).listContainersCmd().withShowAll(true).exec();

            Map<String, Integer> instanceNumbers = [:]
            serverConf.getEligibleServices().each { EligibleServiceConfiguration serviceConf ->
                String serviceName = serviceConf.getType();
                ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(serviceName);
                instanceNumbers.put(serviceName, (instanceNumbers[serviceName] ?: 0) + 1);      // Increment/populate the service instance number
                Integer instanceNumber = instanceNumbers[serviceName];
                Container dockerContainer = containers.find { it.getNames()[0].startsWith("/${tierName}.${environmentName}.") && it.getNames()[0].endsWith(".${serviceName}.${instanceNumber}") }

                // Create a shell of an instance, just in case there is no docker container to get the info from.
                ServiceInstance serviceInstance = new ServiceInstance([
                        tierName: tierName,
                        environmentName: environmentName,
                        name: serviceName,
                        serverName: serverConf.getHostname(),
                        instanceNumber: instanceNumber,
                        status: ServiceInstance.Status.Available,
                        buildPossible: serviceConfiguration.isBuildPossible(),
                        newBuildPossible: serviceConfiguration.isNewBuildPossible(),
                        portDefinitions: serviceConf.portMappings.collect { portMapping ->
                            return new PortDefinition([
                                    portType: portMapping.type,
                                    hostPort: portMapping.port,
                                    containerPort: serviceConfiguration.containerPorts.find { it.type == portMapping.type }.port
                            ])
                        }
                ])

                // Now, if the docker container exists, overwrite all the info with the real info
                if (dockerContainer) {
                    serviceInstance.withDockerContainer(dockerContainer);
                }

                definedServiceInstances << serviceInstance;
            }
        }

        return definedServiceInstances;
    }

    @CompileStatic
    @RequestMapping(value = "/{tierName}/{environmentName}/app/{applicationId}/stop", method = RequestMethod.POST)
    public ApplicationDetails stopApplication(@PathVariable String tierName, @PathVariable String environmentName, @PathVariable String applicationId) {
        ApplicationDetails application = getEnvironmentDetails(tierName, environmentName).find { it.getId() == applicationId }

        application.getServiceInstances().each { ServiceInstance serviceInstance ->
            if (serviceInstance.getStatus() == ServiceInstance.Status.Running) {
                hostsController.stopContainer(serviceInstance.getServerName(), serviceInstance.getContainerId());
                serviceInstance.setStatus(ServiceInstance.Status.Stopped);
                serviceInstance.setContainerStatus("Exited (???) 0 seconds ago")
            }
        }

        return application;
    }

    @CompileStatic
    @RequestMapping(value = "/{tierName}/{environmentName}/app/{applicationId}/start", method = RequestMethod.POST)
    public ApplicationDetails startApplication(@PathVariable String tierName, @PathVariable String environmentName, @PathVariable String applicationId) {
        ApplicationDetails application = getEnvironmentDetails(tierName, environmentName).find { it.getId() == applicationId }

        application.getServiceInstances().each { ServiceInstance serviceInstance ->
            if (serviceInstance.getStatus() == ServiceInstance.Status.Stopped) {
                hostsController.startContainer(serviceInstance.getServerName(), serviceInstance.getContainerId());
                serviceInstance.setStatus(ServiceInstance.Status.Running);
                serviceInstance.setContainerStatus("Up 0 seconds")
            }
        }

        return application;
    }

    @CompileStatic
    @RequestMapping(value = "/{tierName}/{environmentName}/app/{applicationId}/restart", method = RequestMethod.POST)
    public ApplicationDetails restartApplication(@PathVariable String tierName, @PathVariable String environmentName, @PathVariable String applicationId) {
        ApplicationDetails application = getEnvironmentDetails(tierName, environmentName).find { it.getId() == applicationId }

        application.getServiceInstances().each { ServiceInstance serviceInstance ->
            if (serviceInstance.getStatus() == ServiceInstance.Status.Running) {
                hostsController.restartContainer(serviceInstance.getServerName(), serviceInstance.getContainerId());
                serviceInstance.setStatus(ServiceInstance.Status.Running);
                serviceInstance.setContainerStatus("Up 0 seconds")
            }
        }

        return application;
    }

    public BuildApplicationInfo buildApplication(ApplicationDetails applicationDetails, Map<String, String> versionToBuild) {
        String applicationKey = "${applicationDetails.tierName}.${applicationDetails.environmentName}.${applicationDetails.id}";
        if (!buildingApplications.containsKey(applicationKey) || !buildingApplications[applicationKey].isBuilding()) {
            BuildApplicationInfo buildApplicationInfo = new BuildApplicationInfo(applicationDetails);
            buildingApplications[applicationKey] = buildApplicationInfo;

            buildApplicationInfo.getServiceBuildInfoList().each { ServiceBuildInfo serviceBuildInfo ->
                ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(serviceBuildInfo.getServiceName());

                // Let's make sure that this service can be built - TODO: Ensure that this service isn't being built from another application
                if (serviceConfiguration.getBuild()) {
                    log.debug("Scheduling build for service: ${serviceConfiguration.getName()}");
                    serviceBuildInfo.setPromise(serviceConfiguration.getBuild().execute(serviceConfiguration, versionToBuild[serviceConfiguration.getName()]));
                }
            }
        }

        return buildingApplications[applicationKey];
    }

    public Map<String, List<EnvironmentConfiguration>> getAllEnvironments() {
        return environmentConfigurationService.getAllEnvironments().groupBy { it.getTierName() }
    }

    public String createDockerContainer(ApplicationDetails applicationDetails, ServiceInstance instance, String desiredVersion) {
        EnvironmentConfiguration environmentConfiguration = environmentConfigurationService.getEnvironment(applicationDetails.getTierName(), applicationDetails.getEnvironmentName());
        ServerConfiguration serverConfiguration = environmentConfiguration.getServers().find { it.getHostname() == instance.getServerName() }
        ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(instance.getName());
        ServiceInstanceConfiguration serviceInstanceConfiguration = applicationDetails.getApplicationConfiguration().getServiceInstances().find { it.getType() == instance.getName() };
        DockerClient docker = dockerService.getDockerClient(instance.getServerName())

        // Get the image name, so we can build out a DockerTag with the proper version
        String imageName = serviceConfiguration.image
        DockerTag toDeploy = new DockerTag(imageName)
        toDeploy.setTag(desiredVersion);
        instance.setContainerImage(toDeploy);

        // Build a host config, just in case we need to resolve the host name
        HostConfig hostConfig = new HostConfig();
        if (serverConfiguration.getResolveHostname()) {
            InetAddress address = InetAddress.getByName(instance.getServerName());
            hostConfig.setExtraHosts(["${address.getHostName()}:${address.getHostAddress()}"] as String[]);
        }

        // Get the environment variables
        Map<String, String> env = [:]
        if (serviceConfiguration.getEnvironment())
            env.putAll(serviceConfiguration.getEnvironment())
        if (applicationDetails.getApplicationConfiguration().getServiceInstances().find { it.getType() == instance.getName() }.getEnvironment())
            env.putAll(applicationDetails.getApplicationConfiguration().getServiceInstances().find { it.getType() == instance.getName() }.getEnvironment())

        // Resolve them
        def resolutionVariables = [
                application: applicationDetails,
                instance: instance,
                serviceInstances: applicationDetails.getServiceInstances().collectEntries { ServiceInstance serviceInstance ->
                    return [serviceInstance.getName(), [
                            server: serviceInstance.getServerName(),
                            port: serviceInstance.getPortDefinitions().collectEntries {
                                return [it.getPortType(), it.getHostPort()]
                            }
                    ]]
                }
        ]

        Binding b = new Binding(resolutionVariables);
        GroovyShell sh = new GroovyShell(b);
        env.each { k, v ->
            if (v.contains('$'))
                env.put(k, sh.evaluate('"' + v + '"').toString())
        }

        instance.setResolvedEnvironmentVariables(env);

        try {
            // Do a docker pull, just to ensure we have the image locally
            eventPublisher.publishEvent(new PullImageEvent(instance));
            InputStream pullIn = docker.pullImageCmd(toDeploy.toString()).exec();
            pullIn.eachLine {
                log.debug(it);
            }
        } catch (Exception ignored) {
            log.warn("Pull of image $toDeploy was unsuccessful, is this pointing to a real registry?");
        }

        // Plugin Hook!
        pluginService.getCreateContainerPlugins()?.each { it.doWithServiceInstance(instance) }

        // Create the actual container
        CreateContainerResponse resp = docker.createContainerCmd(toDeploy.toString())
                .withName(instance.toString())
                .withEnv(instance.getResolvedEnvironmentVariables()?.collect {k, v -> "$k=$v"} as String[])
                .withVolumes(serviceConfiguration.getContainerVolumes().collect { new Volume(it.getPath()) } as Volume[])
                .withExposedPorts(serviceConfiguration.getContainerPorts().collect { new ExposedPort(it.getPort()) } as ExposedPort[])
                .withHostConfig(hostConfig)
                .exec();
        eventPublisher.publishEvent(new CreateContainerEvent(hostsController.getServiceInstance(instance.getServerName(), resp.id), env));

        // Now start the container
        log.info("Starting container '${resp.id}' with " +
                "ports: ${instance.getPortDefinitions().collect { '0.0.0.0:' + it.getHostPort() + '->' + it.getContainerPort() } }, " +
                "volumes: ${serviceInstanceConfiguration.getVolumeMappings()?.collect { volumeMapping -> volumeMapping.getPath() + '->' + serviceConfiguration.getContainerVolumes()?.find { it.getType() == volumeMapping.getType() }?.getPath() } }")
        StartContainerCmd startCmd = docker.startContainerCmd(resp.id)
                .withPortBindings(instance.getPortDefinitions().collect { new PortBinding(new Ports.Binding("0.0.0.0", it.getHostPort()), new ExposedPort(it.getContainerPort())) } as PortBinding[])

        if (serviceInstanceConfiguration.getVolumeMappings())
            startCmd.withBinds(serviceInstanceConfiguration.getVolumeMappings()?.collect { volumeMapping -> new Bind(volumeMapping.getPath(), new Volume(serviceConfiguration.getContainerVolumes().find { it.getType() == volumeMapping.getType() }.getPath())) } as Bind[])

        if (hostConfig.getExtraHosts())
            startCmd.withExtraHosts(hostConfig.getExtraHosts());

        startCmd.exec();
        eventPublisher.publishEvent(new ContainerStartedEvent(hostsController.getServiceInstance(instance.getServerName(), resp.id)));

        // Create a ServiceInstance out of this Container
        Container container = docker.listContainersCmd().withShowAll(true).exec().find { it.getId() == resp.id }
        if (container) applicationDetails.getServiceInstances() << instance.withDockerContainer(container);

        return resp.id;
    }

}
