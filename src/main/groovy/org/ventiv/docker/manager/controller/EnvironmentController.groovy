package org.ventiv.docker.manager.controller

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Ports
import groovy.util.logging.Slf4j
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.config.DockerEnvironmentConfiguration
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.model.BuildApplicationRequest
import org.ventiv.docker.manager.model.DockerTag
import org.ventiv.docker.manager.model.PortDefinition
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.service.DockerService
import org.ventiv.docker.manager.service.selection.ServiceSelectionAlgorithm

import javax.annotation.Resource

/**
 * Created by jcrygier on 2/27/15.
 */
@Slf4j
@RequestMapping("/api/environment")
@RestController
class EnvironmentController {

    @Resource DockerService dockerService;
    @Resource DockerServiceConfiguration dockerServiceConfiguration;

    @RequestMapping
    public Collection<String> getTiers() {
        getAllEnvironments().keySet();
    }

    @RequestMapping("/{tierName}")
    public Collection<String> getEnvironments(@PathVariable("tierName") String tierName) {
        getAllEnvironments()[tierName];
    }

    @RequestMapping("/{tierName}/{environmentName}")
    public def getEnvironmentDetails(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName) {
        DockerEnvironmentConfiguration envConfiguration = new DockerEnvironmentConfiguration(tierName, environmentName);
        List<ServiceInstance> serviceInstances = getServiceInstances(tierName, environmentName);

        return envConfiguration.configuration.applications.collect { applicationDef ->
            List<ServiceInstance> applicationInstances = serviceInstances.findAll { it.getApplicationId() == applicationDef.id };

            // Now make ServiceInstance objects for each one defined
            List<String> requiredServices = applicationDef.serviceInstances.collect { [it.type] * it.count }.flatten()
            List<String> missingServices = new ArrayList<>(requiredServices)
            applicationInstances.each { missingServices.remove(it.getName())  }  // Remove any that actually exist

            return [
                    id: applicationDef.id,
                    name: applicationDef.name,
                    url: applicationDef.url,
                    tierName: tierName,
                    environmentName: environmentName,
                    serviceInstances: applicationInstances,
                    missingServiceInstances: missingServices
            ]
        }
    }

    /**
     * Builds out an application.  Ensures the docker environment of two things:
     * 1.) Any missingServiceInstances (see getEnvironmentDetails) will be built out according to the requested versions
     * 2.) Each serviceInstance (see getEnvironmentDetails) is on the proper version.  If not, it will destroy the container and rebuild.
     */
    @RequestMapping(value = "/{tierName}/{environmentName}", method = RequestMethod.POST)
    public def buildApplication(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName, @RequestBody BuildApplicationRequest buildRequest) {
        def environmentDetails = getEnvironmentDetails(tierName, environmentName);
        def applicationDetails = environmentDetails.find { it.id == buildRequest.getName() }
        List<ServiceInstance> allServiceInstances = getServiceInstances(tierName, environmentName);

        // First, let's find any missing services
        applicationDetails.missingServiceInstances.each { String missingService ->
            // Find an 'Available' Service Instance
            ServiceInstance toUse = ServiceSelectionAlgorithm.Util.getAvailableServiceInstance(missingService, allServiceInstances, applicationDetails);
            toUse.setApplicationId(buildRequest.getName());

            // Create (and start) the container
            createDockerContainer(toUse, buildRequest.getServiceVersions().get(missingService));

            // Mark this service instance as 'Running' so it won't get used again
            toUse.setStatus(ServiceInstance.Status.Running);
        }

        // Verify all running serviceInstances to ensure they're the correct version
        allServiceInstances.each { ServiceInstance anInstance ->
            if (anInstance.getStatus() != ServiceInstance.Status.Available && anInstance.getContainerImage() != null) {
                String expectedVersion = buildRequest.getServiceVersions()[anInstance.getName()];
                String runningVersion = anInstance.getContainerImage().getTag();

                // We have a version mismatch...destroy the container and rebuild it
                if (expectedVersion != runningVersion) {
                    // First, let's destroy the container
                    destroyDockerContainer(anInstance);

                    // Now, create a new one
                    createDockerContainer(anInstance, buildRequest.getServiceVersions().get(anInstance.getName()));
                }
            }
        }
    }

    @RequestMapping("/{tierName}/{environmentName}/serviceInstances")
    public List<ServiceInstance> getServiceInstances(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName) {
        DockerEnvironmentConfiguration envConfiguration = new DockerEnvironmentConfiguration(tierName, environmentName);

        List<ServiceInstance> definedServiceInstances = [];
        envConfiguration.configuration.servers.each { def serverConf ->
            String hostname = serverConf.hostname;
            List<Container> containers = dockerService.getDockerClient(hostname).listContainersCmd().withShowAll(true).exec();

            Map<String, Integer> instanceNumbers = [:]
            serverConf.eligibleServices.each { def serviceConf ->
                String serviceName = serviceConf.type;
                instanceNumbers.put(serviceName, (instanceNumbers[serviceName] ?: 0) + 1);      // Increment/populate the service instance number
                Integer instanceNumber = instanceNumbers[serviceName];
                Container dockerContainer = containers.find { it.getNames()[0].startsWith("/${tierName}.${environmentName}.") && it.getNames()[0].endsWith(".${serviceName}.${instanceNumber}") }

                ServiceInstance serviceInstance = new ServiceInstance([
                        tierName: tierName,
                        environmentName: environmentName,
                        name: serviceName,
                        serverName: hostname,
                        instanceNumber: instanceNumber,
                        status: ServiceInstance.Status.Available,
                        portDefinitions: serviceConf.portMappings.collect { portMapping ->
                            return new PortDefinition([
                                    portType: portMapping.type,
                                    hostPort: portMapping.port,
                                    containerPort: dockerServiceConfiguration.getServiceConfiguration(serviceName).containerPorts.find { it.type == portMapping.type }.port
                            ])
                        }
                ])

                if (dockerContainer) {
                    serviceInstance.withDockerContainer(dockerContainer);
                    serviceInstance.portDefinitions = dockerContainer.getPorts().collect { Container.Port port ->
                        return new PortDefinition([
                                portType: dockerServiceConfiguration.getServiceConfiguration(serviceName).containerPorts.find { it.port == port.getPrivatePort() }?.type,
                                hostPort: port.getPublicPort(),
                                containerPort: port.getPrivatePort()
                        ])
                    }
                }

                definedServiceInstances << serviceInstance;
            }
        }

        return definedServiceInstances;
    }

    public Map<String, List<String>> getAllEnvironments() {
        // Search for all YAML files under /data/env-config/tiers
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        def allEnvironments = resolver.getResources("classpath:/data/env-config/tiers/**/*.yml")

        // Group by Directory, then Massage the ClassPathResource elements into the filename minus .yml
        return allEnvironments.groupBy { new File(it.path).getParentFile().getName() }.collectEntries { k, v -> [k, v.collect { it.getFilename().replaceAll("\\.yml", "") }] }
    }

    private String createDockerContainer(ServiceInstance instance, String desiredVersion) {
        // Get the image name, so we can build out a DockerTag with the proper version
        String imageName = dockerServiceConfiguration.getServiceConfiguration(instance.getName()).image
        DockerTag toDeploy = new DockerTag(imageName)
        toDeploy.setTag(desiredVersion);

        // Build out port bindings
        Ports portBindings = new Ports();
        instance.getPortDefinitions().each {
            portBindings.bind(new ExposedPort(it.getContainerPort()), new Ports.Binding(it.getHostPort()));
        }

        HostConfig hostConfig = new HostConfig();
        hostConfig.setPortBindings(portBindings);

        // Do a docker pull, just to ensure we have the image locally
        DockerClient docker = dockerService.getDockerClient(instance.getServerName())
        log.info("Pulling docker image: '$toDeploy");
        InputStream pullIn = docker.pullImageCmd(toDeploy.toString()).exec();
        pullIn.eachLine {
            log.debug(it);
        }

        // Create the actual container
        log.info("Creating new Docker Container on Host: '${instance.getServerName()}' with image: '${toDeploy.toString()}' and name: '${instance.toString()}'")
        CreateContainerResponse resp = docker.createContainerCmd(toDeploy.toString())
                .withName(instance.toString())
                .withHostConfig(hostConfig).exec();

        log.info("Created container with ID: '${resp.id}'.  Starting...")
        docker.startContainerCmd(resp.id).exec();

        return resp.id;
    }

    private void destroyDockerContainer(ServiceInstance instance) {
        log.info("Destroying docker container: '${instance.toString()}")
        dockerService.getDockerClient(instance.getServerName()).removeContainerCmd(instance.toString()).withForce().exec();
    }


}
