package org.ventiv.docker.manager.controller

import com.github.dockerjava.api.model.Container
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

import javax.annotation.Resource

/**
 * Created by jcrygier on 2/27/15.
 */
@RequestMapping("/environment")
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
            List<String> missingServices = requiredServices - applicationInstances.collect { it.getName() }

            return [
                    id: applicationDef.id,
                    name: applicationDef.name,
                    url: applicationDef.url,
                    serviceInstances: applicationInstances,
                    missingServiceInstances: missingServices
            ]
        }
    }

    /**
     * Builds out an environment.  Ensures the docker environment of two things:
     * 1.) Any missingServiceInstances (see getEnvironmentDetails) will be built out according to the requested versions
     * 2.) Each serviceInstance (see getEnvironmentDetails) is on the proper version.  If not, it will destroy the container and rebuild.
     */
    @RequestMapping(value = "/{tierName}/{environmentName}", method = RequestMethod.POST)
    public def buildEnvironment(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName, @RequestBody BuildApplicationRequest buildRequest) {
        def environmentDetails = getEnvironmentDetails(tierName, environmentName);

        // First, let's find any open services
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
                        status: ServiceInstance.Status.Available
                ])

                if (dockerContainer) {
                    serviceInstance.setDockerName(dockerContainer.getNames()[0])
                    serviceInstance.status = dockerContainer.getStatus().startsWith("Up") ? ServiceInstance.Status.Running : ServiceInstance.Status.Stopped;
                    serviceInstance.containerStatus = dockerContainer.getStatus();
                    serviceInstance.containerId = dockerContainer.getId();
                    serviceInstance.containerImage = new DockerTag(dockerContainer.getImage());
                    serviceInstance.containerCreatedDate = new Date(dockerContainer.getCreated() * 1000);
                    serviceInstance.portDefinitions = dockerContainer.getPorts().collect { Container.Port port ->
                        return new PortDefinition([
                                portType: serviceConf.portMappings.find { it.port == port.getPublicPort() }.type,
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

    private Map<String, List<String>> getAllEnvironments() {
        // Search for all YAML files under /data/env-config/tiers
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        def allEnvironments = resolver.getResources("classpath:/data/env-config/tiers/**/*.yml")

        // Group by Directory, then Massage the ClassPathResource elements into the filename minus .yml
        return allEnvironments.groupBy { new File(it.path).getParentFile().getName() }.collectEntries { k, v -> [k, v.collect { it.getFilename().replaceAll("\\.yml", "") }] }
    }


}
