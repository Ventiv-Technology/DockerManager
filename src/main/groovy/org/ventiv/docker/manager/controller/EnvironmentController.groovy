package org.ventiv.docker.manager.controller

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.config.DockerEnvironmentConfiguration
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.model.DockerTag
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

        // Loop through this Environment's Servers, and get any Instances running on that server.  Then find the ones that belong to this env
        List<ServiceInstance> deployedServiceInstances = envConfiguration.configuration.servers
                .collect { getServiceInstances(it.hostname) }.flatten()
                .findAll { it.getTierName() == tierName && it.getEnvironmentName() == environmentName }

        return envConfiguration.configuration.applications.collect { applicationDef ->
            List<ServiceInstance> applicationServiceInstances = deployedServiceInstances.findAll {
                it.getApplicationId() == applicationDef.id
            }

            // Now make ServiceInstance objects for each one defined
            List<ServiceInstance> definedServiceInstances = []
            applicationDef.serviceInstances.each {
                for (int i = 0; i < it.count; i++) {
                    definedServiceInstances << new ServiceInstance([
                            name: it.type,
                            instanceNumber: i + 1
                    ])
                }
            }

            // Now, remove the ones that are found
            List<ServiceInstance> missingServiceInstances = definedServiceInstances.findAll { definedServiceInstance ->
                return !applicationServiceInstances.find { it.name == definedServiceInstance.name && it.instanceNumber == definedServiceInstance.instanceNumber }
            }


            return [
                    id: applicationDef.id,
                    name: applicationDef.name,
                    url: applicationDef.url,
                    serviceInstances: applicationServiceInstances,
                    missingServiceInstances: missingServiceInstances.collect { it.name }
            ]
        }
    }

    private List<ServiceInstance> getServiceInstances(String hostName) {
        DockerClient dc = dockerService.getDockerClient(hostName);
        List<Container> containers = dc.listContainersCmd().withShowAll(true).exec();

        List<ServiceInstance> answer = [];
        containers.each { Container c ->
            String matchingName = c.getNames().find { it =~ ServiceInstance.DOCKER_NAME_PATTERN }
            if (matchingName) {
                def matcher = matchingName =~ ServiceInstance.DOCKER_NAME_PATTERN
                def serviceInformation = dockerServiceConfiguration.getServiceConfiguration(matcher[0][4]);

                answer << new ServiceInstance([
                        tierName: matcher[0][1],
                        environmentName: matcher[0][2],
                        applicationId: matcher[0][3],
                        name: matcher[0][4],
                        instanceNumber: Integer.parseInt(matcher[0][5]),
                        serverName: hostName,
                        status: c.getStatus().startsWith("Up") ? ServiceInstance.Status.Running : ServiceInstance.Status.Stopped,
                        containerStatus: c.getStatus(),
                        containerId: c.getId(),
                        containerImage: new DockerTag(c.getImage()),
                        containerCreatedDate: new Date(c.getCreated() * 1000),
                        portDefinitions: c.getPorts().collect { Container.Port port ->
                            return [
                                    portType: serviceInformation.containerPorts.find { it.port == port.getPrivatePort() }.type,
                                    hostPort: port.getPublicPort(),
                                    containerPort: port.getPrivatePort()
                            ]
                        }
                ])
            }
        }

        return answer;
    }

    private Map<String, List<String>> getAllEnvironments() {
        // Search for all YAML files under /data/env-config/tiers
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        def allEnvironments = resolver.getResources("classpath:/data/env-config/tiers/**/*.yml")

        // Group by Directory, then Massage the ClassPathResource elements into the filename minus .yml
        return allEnvironments.groupBy { new File(it.path).getParentFile().getName() }.collectEntries { k, v -> [k, v.collect { it.getFilename().replaceAll("\\.yml", "") }] }
    }


}
