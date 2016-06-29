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
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.StartContainerCmd
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Link
import com.github.dockerjava.api.model.Links
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.command.PullImageResultCallback
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.io.IOUtils
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.scheduling.TaskScheduler
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.event.DeploymentCancelledEvent
import org.ventiv.docker.manager.event.DeploymentScheduledEvent
import org.ventiv.docker.manager.event.DeploymentStartedEvent
import org.ventiv.docker.manager.event.PullImageEvent
import org.ventiv.docker.manager.model.ApplicationDetails
import org.ventiv.docker.manager.model.BuildApplicationInfo
import org.ventiv.docker.manager.model.DeployApplicationRequest
import org.ventiv.docker.manager.model.DockerTag
import org.ventiv.docker.manager.model.MissingService
import org.ventiv.docker.manager.model.PortDefinition
import org.ventiv.docker.manager.model.ServiceBuildInfo
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.configuration.ApplicationConfiguration
import org.ventiv.docker.manager.model.configuration.EligibleServiceConfiguration
import org.ventiv.docker.manager.model.configuration.EnvironmentConfiguration
import org.ventiv.docker.manager.model.configuration.LinkConfiguration
import org.ventiv.docker.manager.model.configuration.ServerConfiguration
import org.ventiv.docker.manager.model.configuration.ServiceConfiguration
import org.ventiv.docker.manager.model.configuration.ServiceInstanceConfiguration
import org.ventiv.docker.manager.security.DockerManagerPermission
import org.ventiv.docker.manager.security.SecurityUtil
import org.ventiv.docker.manager.service.ApplicationDeploymentService
import org.ventiv.docker.manager.service.DockerService
import org.ventiv.docker.manager.service.EnvironmentConfigurationService
import org.ventiv.docker.manager.service.PluginService
import org.ventiv.docker.manager.service.ServiceInstanceService
import org.ventiv.docker.manager.utils.CachingGroovyShell
import org.ventiv.docker.manager.utils.DockerUtils
import org.ventiv.docker.manager.utils.UserAuditFilter

import javax.annotation.Resource
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.ScheduledFuture

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
    @Resource ServiceInstanceService serviceInstanceService;
    @Resource TaskScheduler taskScheduler;

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
    public Collection<ApplicationDetails> getEnvironmentDetails(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName) {
        EnvironmentConfiguration envConfiguration = environmentConfigurationService.getEnvironment(tierName, environmentName);
        Collection<ServiceInstance> serviceInstances = getServiceInstances(tierName, environmentName);

        return SecurityUtil.filter(envConfiguration.getApplications().collect { ApplicationConfiguration applicationConfiguration ->
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

            List<String> branches = applicationConfiguration.getBranches()

            return new ApplicationDetails([
                    tierName: tierName,
                    environmentName: environmentName,
                    environmentDescription: envConfiguration.getDescription(),
                    url: url,
                    branches: branches,
                    serviceInstances: applicationInstances,
                    missingServiceInstances: missingServices.collect { new MissingService([serviceName: it, serviceDescription: dockerServiceConfiguration.getServiceConfiguration(it).getDescription()]) },
                    buildStatus: buildingApplications.get("$tierName.$environmentName.${applicationConfiguration.getId()}")?.getBuildStatus(),
                    deploymentInProgress: deploymentService.isRunning(tierName, environmentName, applicationConfiguration.getId()),
                    version: versionsDeployed.join(", "),
                    scheduledDeployment: deploymentService.getScheduledDeployment(tierName, environmentName, applicationConfiguration.getId())
            ]).withApplicationConfiguration(applicationConfiguration)
        }, DockerManagerPermission.READ)
    }

    @RequestMapping("/{tierName}/{environmentName}/app/{applicationId}/versions")
    public Collection<Map<String, String>> getApplicationVersions(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName, @PathVariable("applicationId") String applicationId,  @RequestParam(value = 'q', required = false) String query) {
        return getApplicationVersions(tierName, environmentName, applicationId, null, query)
    }

    @RequestMapping("/{tierName}/{environmentName}/app/{applicationId}/versions/{branch}")
    public Collection<Map<String, String>> getApplicationVersions(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName, @PathVariable("applicationId") String applicationId, @PathVariable("branch") String branch, @RequestParam(value = 'q', required = false) String query) {
        EnvironmentConfiguration envConfiguration = environmentConfigurationService.getEnvironment(tierName, environmentName);
        ApplicationConfiguration appConfiguration = envConfiguration.getApplications().find { it.getId() == applicationId };
        Collection<ServiceConfiguration> allServiceConfigurations = appConfiguration.getServiceInstances()*.getType().unique().collect { dockerServiceConfiguration.getServiceConfiguration(it); }
        Collection<ServiceConfiguration> nonPinnedServices = allServiceConfigurations.findAll { it.getPinnedVersion() == null };

        Collection<List<String>> versions = nonPinnedServices.collect { it.getPossibleVersions(branch, query) }.unique()

        // Create an option for the new build - if applicable
        Map<String, String> newBuildOption = nonPinnedServices.any { it.isNewBuildPossible() } ? [id: "BuildNewVersion", text: "New Build"] : [:]

        if (versions.size() == 1) {
            return [newBuildOption] + versions[0].collect {
                return [id: it, text: it]
            }
        }

        return [ newBuildOption ];
    }

    @RequestMapping(value = "/{tierName}/{environmentName}/app/{applicationId}/cancelScheduledDeployment", method = RequestMethod.DELETE)
    public DeploymentScheduledEvent cancelScheduledDeployment(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName, @PathVariable("applicationId") String applicationId) {
        DeploymentScheduledEvent deploymentScheduledEvent = deploymentService.removeScheduledDeployment(tierName, environmentName, applicationId, true);
        eventPublisher.publishEvent(new DeploymentCancelledEvent(tierName, environmentName, applicationId, deploymentScheduledEvent));

        return deploymentScheduledEvent;
    }

    /**
     * Builds out an application.  Ensures the docker environment of two things:
     * 1.) Any missingServiceInstances (see getEnvironmentDetails) will be built out according to the requested versions
     * 2.) Each serviceInstance (see getEnvironmentDetails) is on the proper version.  If not, it will destroy the container and rebuild.
     */
    @PreAuthorize("hasPermission(#tierName + '.' + #environmentName + '.' + #deployRequest.name, 'DEPLOY')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RequestMapping(value = "/{tierName}/{environmentName}", method = RequestMethod.POST)
    public void deployApplication(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName, @RequestBody DeployApplicationRequest deployRequest) {
        List<ApplicationDetails> environmentDetails = getEnvironmentDetails(tierName, environmentName);
        ApplicationDetails applicationDetails = environmentDetails.find { it.getId() == deployRequest.getName() }
        applicationDetails.setBuildServiceVersionsTemplate(deployRequest.getServiceVersions());

        // Update our audit object with the version
        UserAuditFilter.getAuditsForPermission(DockerManagerPermission.DEPLOY).each {
            it.setAuditDetails(deployRequest.getServiceVersions());
        }

        // First, Build the application
        buildApplication(applicationDetails, deployRequest.branch, deployRequest.getServiceVersions()).onSuccessfulBuild { ApplicationDetails builtApplication ->
            // The following does 2 things: 1.) Sends a message to the UI that a deployment is now going, and 2.) Gets Picked up by ApplicationDeploymentService to do the actual deployment
            eventPublisher.publishEvent(new DeploymentStartedEvent(applicationDetails, deployRequest.branch, builtApplication.getBuildServiceVersionsTemplate(), deployRequest.requestedVersion));
        }
    }

    @PreAuthorize("hasPermission(#tierName + '.' + #environmentName + '.' + #applicationId, 'DEPLOY')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RequestMapping(value = "/{tierName}/{environmentName}/app/{applicationId}/{version:.*}", method = RequestMethod.POST)
    public void deployApplication(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName, @PathVariable("applicationId") String applicationId,  @PathVariable("version") String version, @RequestBody Map<String, Object> variables) {
        deployApplication(tierName, environmentName, applicationId, null, version, variables)
    }

    @PreAuthorize("hasPermission(#tierName + '.' + #environmentName + '.' + #applicationId, 'DEPLOY')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RequestMapping(value = "/{tierName}/{environmentName}/app/{applicationId}/{branch}/{version:.*}", method = RequestMethod.POST)
    public void deployApplication(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName, @PathVariable("applicationId") String applicationId, @PathVariable("branch") String branch,  @PathVariable("version") String version, @RequestBody Map<String, Object> variables) {
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

        List<ApplicationDetails> environmentDetails = getEnvironmentDetails(tierName, environmentName);
        ApplicationDetails applicationDetails = environmentDetails.find { it.getId() == applicationId }

        // Throw an error if we've already scheduled a deployment for this app
        if (deploymentService.isDeploymentScheduled(applicationDetails))
            throw new IllegalArgumentException("A deployment has already been scheduled for: ${applicationDetails.getDescription()}");


        // Finally, call the deploy
        if (variables?.delay) {
            Calendar delayedDeployTime = Calendar.getInstance();
            delayedDeployTime.setTimeInMillis(System.currentTimeMillis());
            delayedDeployTime.add(Calendar.MINUTE, (Integer) variables.delay);

            // Get some details we'll need for the delayed deployment
            Authentication requestor = SecurityContextHolder.getContext().getAuthentication();

            // Schedule the deployment
            ScheduledFuture scheduledDeploy = taskScheduler.schedule({
                SecurityContextHolder.getContext().setAuthentication(requestor);
                deploymentService.removeScheduledDeployment(applicationDetails);
                deployApplication(tierName, environmentName, new DeployApplicationRequest(tierName: tierName, environmentName: environmentName, name: applicationId, branch: branch, serviceVersions: serviceVersions, requestedVersion: version));
                SecurityContextHolder.getContext().setAuthentication(null);
            } as Runnable, new Date(delayedDeployTime.getTimeInMillis()));

            // Keep track of the deployment event, so we can cancel it later if need be
            DeploymentScheduledEvent deploymentEvent = new DeploymentScheduledEvent(applicationDetails, branch, serviceVersions, version, new Date(delayedDeployTime.getTimeInMillis()), scheduledDeploy)
            deploymentService.scheduleDeployment(applicationDetails, deploymentEvent);

            // Let the world know we've scheduled a deployment
            eventPublisher.publishEvent(deploymentEvent);
        } else {
            deployApplication(tierName, environmentName, new DeployApplicationRequest(tierName: tierName, environmentName: environmentName, name: applicationId, branch: branch, serviceVersions: serviceVersions, requestedVersion: version));
        }

    }

    /**
     * Retreives all service instances, running, stopped, or available.
     * @param tierName
     * @param environmentName
     * @return
     */
    @CompileStatic
    @RequestMapping("/{tierName}/{environmentName}/serviceInstances")
    public Collection<ServiceInstance> getServiceInstances(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName) {
        // Get the created instances
        Collection<ServiceInstance> allInstances = getServiceInstanceService().getServiceInstances().findAll { ServiceInstance serviceInstance ->
            serviceInstance.getTierName() == tierName && serviceInstance.getEnvironmentName() == environmentName
        }

        // Now get the available ones
        EnvironmentConfiguration envConfiguration = environmentConfigurationService.getEnvironment(tierName, environmentName);
        envConfiguration.getServers().each { ServerConfiguration serverConfiguration ->
            serviceInstanceService.getAvailableServiceInstances(serverConfiguration).each { EligibleServiceConfiguration eligibleServiceConfiguration ->
                ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(eligibleServiceConfiguration.getType());

                allInstances << serviceInstanceService.createServiceInstance([
                        tierName: tierName,
                        environmentName: environmentName,
                        name: eligibleServiceConfiguration.getType(),
                        serverName: serverConfiguration.getHostname(),
                        instanceNumber: eligibleServiceConfiguration.getInstanceNumber(),
                        status: ServiceInstance.Status.Available,
                        buildPossible: serviceConfiguration.isBuildPossible(),
                        newBuildPossible: serviceConfiguration.isNewBuildPossible(),
                        portDefinitions: eligibleServiceConfiguration.portMappings.collect { portMapping ->
                            return new PortDefinition([
                                    portType: portMapping.type,
                                    hostPort: portMapping.port,
                                    containerPort: serviceConfiguration.containerPorts.find { it.type == portMapping.type }.port
                            ])
                        }
                ])
            }
        }

        return SecurityUtil.filter(allInstances, DockerManagerPermission.READ)
    }

    @PreAuthorize("hasPermission(#tierName + '.' + #environmentName + '.' + #applicationId, 'STOP')")
    @CompileStatic
    @RequestMapping(value = "/{tierName}/{environmentName}/app/{applicationId}/stop", method = RequestMethod.POST)
    public ApplicationDetails stopApplication(@PathVariable String tierName, @PathVariable String environmentName, @PathVariable String applicationId) {
        ApplicationDetails application = getEnvironmentDetails(tierName, environmentName).find { it.getId() == applicationId }

        application.getServiceInstances().each { ServiceInstance serviceInstance ->
            if (serviceInstance.getStatus() == ServiceInstance.Status.Running) {
                hostsController.stopContainer(serviceInstance.getServerName(), serviceInstance.getContainerId());
                serviceInstance.setStatus(ServiceInstance.Status.Stopped);
                serviceInstance.setContainerStatusTime(new Date());
            }
        }

        return application;
    }

    @PreAuthorize("hasPermission(#tierName + '.' + #environmentName + '.' + #applicationId, 'START')")
    @CompileStatic
    @RequestMapping(value = "/{tierName}/{environmentName}/app/{applicationId}/start", method = RequestMethod.POST)
    public ApplicationDetails startApplication(@PathVariable String tierName, @PathVariable String environmentName, @PathVariable String applicationId) {
        ApplicationDetails application = getEnvironmentDetails(tierName, environmentName).find { it.getId() == applicationId }

        application.getServiceInstances().each { ServiceInstance serviceInstance ->
            if (serviceInstance.getStatus() == ServiceInstance.Status.Stopped) {
                hostsController.startContainer(serviceInstance.getServerName(), serviceInstance.getContainerId());
                serviceInstance.setStatus(ServiceInstance.Status.Running);
                serviceInstance.setContainerStatusTime(new Date());
            }
        }

        return application;
    }

    @PreAuthorize("hasPermission(#tierName + '.' + #environmentName + '.' + #applicationId, 'RESTART')")
    @CompileStatic
    @RequestMapping(value = "/{tierName}/{environmentName}/app/{applicationId}/restart", method = RequestMethod.POST)
    public ApplicationDetails restartApplication(@PathVariable String tierName, @PathVariable String environmentName, @PathVariable String applicationId) {
        ApplicationDetails application = getEnvironmentDetails(tierName, environmentName).find { it.getId() == applicationId }

        application.getServiceInstances().each { ServiceInstance serviceInstance ->
            if (serviceInstance.getStatus() == ServiceInstance.Status.Running) {
                hostsController.restartContainer(serviceInstance.getServerName(), serviceInstance.getContainerId());
                serviceInstance.setStatus(ServiceInstance.Status.Running);
                serviceInstance.setContainerStatusTime(new Date());
            }
        }

        return application;
    }

    @PreAuthorize("hasPermission(#tierName + '.' + #environmentName + '.' + #applicationId, 'READ')")
    @CompileStatic
    @RequestMapping(value = "/{tierName}/{environmentName}/app/{applicationId}/extraInformation", method = RequestMethod.GET)
    public void getApplicationExtraInformation(@PathVariable String tierName, @PathVariable String environmentName, @PathVariable String applicationId, HttpServletResponse response) {
        ApplicationDetails application = getEnvironmentDetails(tierName, environmentName).find { it.getId() == applicationId }
        String extraInformationPartial = application.getApplicationConfiguration().getExtraInformationPartial();

        if (extraInformationPartial) {
            response.setContentType(MediaType.TEXT_HTML_VALUE);

            org.springframework.core.io.Resource resource = new FileSystemResource(extraInformationPartial)
            IOUtils.copy(resource.getInputStream(), response.getOutputStream());
        }
    }

    @CompileStatic
    @RequestMapping(value = "/{tierName}/{environmentName}/switch/{applicationId1}/{applicationId2}", method = RequestMethod.POST)
    public void switchApplicationInstances(@PathVariable String tierName, @PathVariable String environmentName, @PathVariable String applicationId1, @PathVariable String applicationId2) {
        Collection<ServiceInstance> serviceInstances1 = serviceInstanceService.getServiceInstances().findAll {
            it.getTierName() == tierName && it.getEnvironmentName() == it.getEnvironmentName() && it.getApplicationId() == applicationId1
        }

        Collection<ServiceInstance> serviceInstances2 = serviceInstanceService.getServiceInstances().findAll {
            it.getTierName() == tierName && it.getEnvironmentName() == it.getEnvironmentName() && it.getApplicationId() == applicationId2
        }

        // Verify each service is on a server with at least v1.17 of docker (that is when rename was introduced)
        (serviceInstances1 + serviceInstances2).collect { it.getServerName() }.unique().each { String serverName ->
            if (dockerService.getMinorApiVersion(serverName) < 17)
                throw new IllegalArgumentException("Unable to rename containers as $serverName is not on at least docker 1.5 (api v1.17)");
        }

        serviceInstances1.each { ServiceInstance serviceInstance ->
            serviceInstance.setApplicationId(applicationId2);       // Rename the instance
            dockerService.getDockerClient(serviceInstance.getServerName())
                    .renameContainerCmd(serviceInstance.getContainerId()).withName(serviceInstance.toString())
                    .exec();
            //dockerService.getRenameContainerCmd(serviceInstance.getServerName(), serviceInstance.getContainerId(), serviceInstance.toString()).exec()
        }

        serviceInstances2.each { ServiceInstance serviceInstance ->
            serviceInstance.setApplicationId(applicationId1);       // Rename the instance
            dockerService.getDockerClient(serviceInstance.getServerName())
                    .renameContainerCmd(serviceInstance.getContainerId()).withName(serviceInstance.toString())
                    .exec();

            //dockerService.getRenameContainerCmd(serviceInstance.getServerName(), serviceInstance.getContainerId(), serviceInstance.toString()).exec()
        }
    }

    public BuildApplicationInfo buildApplication(ApplicationDetails applicationDetails, String branch, Map<String, String> versionToBuild) {
        String applicationKey = "${applicationDetails.tierName}.${applicationDetails.environmentName}.${applicationDetails.id}";
        if (!buildingApplications.containsKey(applicationKey) || !buildingApplications[applicationKey].isBuilding()) {
            BuildApplicationInfo buildApplicationInfo = new BuildApplicationInfo(applicationDetails);
            buildingApplications[applicationKey] = buildApplicationInfo;

            buildApplicationInfo.getServiceBuildInfoList().each { ServiceBuildInfo serviceBuildInfo ->
                ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(serviceBuildInfo.getServiceName());

                // Let's make sure that this service can be built - TODO: Ensure that this service isn't being built from another application
                if (serviceConfiguration.getBuild()) {
                    log.debug("Scheduling build for service: ${serviceConfiguration.getName()}");
                    serviceBuildInfo.setPromise(serviceConfiguration.getBuild().execute(applicationDetails, serviceConfiguration, branch, versionToBuild[serviceConfiguration.getName()]));
                }
            }
        }

        return buildingApplications[applicationKey];
    }

    public Map<String, List<EnvironmentConfiguration>> getAllEnvironments() {
        return environmentConfigurationService.getAllEnvironments().groupBy { it.getTierName() }
    }

    public String createDockerContainer(ApplicationDetails applicationDetails, ServiceInstance instance, String branch, String desiredVersion) {
        log.info("Creating Container for Application ($applicationDetails), Service ($instance), version ($desiredVersion)")
        EnvironmentConfiguration environmentConfiguration = environmentConfigurationService.getEnvironment(applicationDetails.getTierName(), applicationDetails.getEnvironmentName());
        ServerConfiguration serverConfiguration = environmentConfiguration.getServers().find { it.getHostname() == instance.getServerName() }
        ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(instance.getName());
        ServiceInstanceConfiguration serviceInstanceConfiguration = applicationDetails.getApplicationConfiguration().getServiceInstances().find { it.getType() == instance.getName() };
        DockerClient docker = dockerService.getDockerClient(instance.getServerName())

        // Get the image name, so we can build out a DockerTag with the proper version
        String imageName = serviceConfiguration.image
        DockerTag toDeploy = new DockerTag(imageName)
        String tagStr = serviceConfiguration.buildPossible && branch ? branch + "-" + desiredVersion : desiredVersion
        toDeploy.setTag(tagStr);
        instance.setContainerImage(toDeploy);

        // Build a host config, just in case we need to resolve the host name
        HostConfig hostConfig = new HostConfig();
        if (serverConfiguration.getResolveHostname()) {
            InetAddress address = InetAddress.getByName(instance.getServerName());
            hostConfig.withExtraHosts(["${address.getHostName()}:${address.getHostAddress()}"] as String[]);
        }

        // Get the environment variables
        Map<String, Object> env = [:]
        if (serviceConfiguration.getEnvironment())
            env.putAll(serviceConfiguration.getEnvironmentWithGroovyShells())
        if (applicationDetails.getApplicationConfiguration().getServiceInstances().find { it.getType() == instance.getName() }.getEnvironment())
            env.putAll(applicationDetails.getApplicationConfiguration().getServiceInstances().find { it.getType() == instance.getName() }.getEnvironmentWithGroovyShells())

        // Resolve them
        def resolutionVariables = [
                application: applicationDetails,
                instance: instance,
                serviceInstances: applicationDetails.getServiceInstances().collectEntries { ServiceInstance serviceInstance ->
                    return [serviceInstance.getName(), serviceInstance.getTemplateBindings()]
                }
        ]

        // Convert the ones that came back as CachingGroovyShell instances
        env.each { k, v ->
            if (v instanceof CachingGroovyShell)
                env.put(k, v.eval(resolutionVariables))
        }

        instance.setResolvedEnvironmentVariables(env);

        PullImageResultCallback pullCallback = new PullImageResultCallback();
        try {
            // Do a docker pull, just to ensure we have the image locally
            eventPublisher.publishEvent(new PullImageEvent(instance));
            log.info("Pulling image: ${toDeploy.toString()}")
            docker.pullImageCmd(toDeploy.toString()).exec(pullCallback);
        } catch (Exception ignored) {
            log.warn("Pull of image $toDeploy was unsuccessful, is this pointing to a real registry?");
        }

        // Determine the memory limits
        long memoryLimit = 0;
        long memorySwapLimit = 0;
        if (serviceInstanceConfiguration.getMemoryLimit())
            memoryLimit = DockerUtils.convertToBytes(serviceInstanceConfiguration.getMemoryLimit())
        else if (serviceConfiguration.getMemoryLimit())
            memoryLimit = DockerUtils.convertToBytes(serviceConfiguration.getMemoryLimit())

        if (serviceInstanceConfiguration.getMemorySwapLimit())
            memorySwapLimit = DockerUtils.convertToBytes(serviceInstanceConfiguration.getMemorySwapLimit())
        else if (serviceConfiguration.getMemorySwapLimit())
            memorySwapLimit = DockerUtils.convertToBytes(serviceConfiguration.getMemorySwapLimit())

        if (memorySwapLimit < memoryLimit)
            memorySwapLimit = memoryLimit

        // Plugin Hook!
        pluginService.getCreateContainerPlugins()?.each { it.doWithServiceInstance(instance) }

        // Resolve the links - TODO: Ambassador Pattern (https://docs.docker.com/articles/ambassador_pattern_linking/)
        Link[] links = serviceConfiguration.getLinks().collect() { LinkConfiguration linkConfiguration ->
            // First, determine the container - must be part of the same application
            ServiceInstance foreignServiceInstance = applicationDetails.getServiceInstances().find { it.getName() == linkConfiguration.getContainer() }

            return new Link(foreignServiceInstance.toString(), linkConfiguration.getAlias())
        } as Link[]
        hostConfig.withLinks(new Links(links));

        // Port Bindings
        Ports portBindings = new Ports();
        instance.getPortDefinitions().each {
            portBindings.bind(new ExposedPort(it.getContainerPort()), new Ports.Binding('0.0.0.0', it.getHostPort().toString()));
        }

        // Now, create the container - Waiting for the pull to finish first
        pullCallback.awaitCompletion();

        // Create the actual container
        log.info("Creating new Docker Container on Host: '${instance.getServerName()}' " +
                "with image: '${instance.getContainerImage().toString()}', " +
                "name: '${instance.toString()}', " +
                "env: ${instance.getResolvedEnvironmentVariables()?.collect {k, v -> "$k=$v"}}, " +
                "memoryLimit: $memoryLimit, memorySwapLimit: $memorySwapLimit")
        CreateContainerCmd createContainerCmd = docker.createContainerCmd(toDeploy.toString())
                //.withHostConfig(hostConfig)
                .withName(instance.toString())
                .withEnv(instance.getResolvedEnvironmentVariables()?.collect {k, v -> "$k=$v"} as String[])
                .withVolumes(serviceConfiguration.getContainerVolumes().collect { new Volume(it.getPath()) } as Volume[])
                .withExposedPorts(instance.getPortDefinitions().findAll { it.getHostPort() }.collect { new ExposedPort(it.getHostPort()) } as ExposedPort[])
                .withPortBindings(portBindings)
                .withLinks(links)
                .withMemory(memoryLimit)
                .withMemorySwap(memorySwapLimit);

        if (serviceInstanceConfiguration.getVolumeMappings())
            createContainerCmd.withBinds(serviceInstanceConfiguration.getVolumeMappings()?.collect { volumeMapping -> new Bind(volumeMapping.getPath(), new Volume(serviceConfiguration.getContainerVolumes().find { it.getType() == volumeMapping.getType() }.getPath())) } as Bind[])

        if (hostConfig.getExtraHosts())
            createContainerCmd.withExtraHosts(hostConfig.getExtraHosts());

        CreateContainerResponse resp = createContainerCmd.exec();

        // Lets get an updated port mapping, just in case it was lost because the container was stopped
        instance.setPortsFromConfiguration();

        // Now start the container
        log.info("Starting container '${resp.id}' with " +
                "ports: ${instance.getPortDefinitions().collect { '0.0.0.0:' + it.getHostPort() + '->' + it.getContainerPort() } }, " +
                "volumes: ${serviceInstanceConfiguration.getVolumeMappings()?.collect { volumeMapping -> volumeMapping.getPath() + '->' + serviceConfiguration.getContainerVolumes()?.find { it.getType() == volumeMapping.getType() }?.getPath() } }")
        StartContainerCmd startCmd = docker.startContainerCmd(resp.id).exec();
        //eventPublisher.publishEvent(new ContainerStartedEvent(hostsController.getServiceInstance(instance.getServerName(), resp.id)));

        // Create a ServiceInstance out of this Container
        Container container = docker.listContainersCmd().withShowAll(true).exec().find { it.getId() == resp.id }
        if (container) applicationDetails.getServiceInstances() << instance.withDockerContainer(container);

        return resp.id;
    }

}
