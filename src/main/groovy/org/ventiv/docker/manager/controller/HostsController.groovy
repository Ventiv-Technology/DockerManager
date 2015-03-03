package org.ventiv.docker.manager.controller

import com.github.dockerjava.api.model.Container
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.config.DockerEnvironmentConfiguration
import org.ventiv.docker.manager.config.PropertyTypes
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.service.DockerService

import javax.annotation.Resource

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

    @RequestMapping
    public def getHostDetails() {
        return getAllHosts().collect { def hostConfiguration ->
            List<Container> hostContainers = null;
            String status = "Online";

            try {
                hostContainers = dockerService.getDockerClient(hostConfiguration.hostname.toString()).listContainersCmd().withShowAll(true).exec()
            } catch (Exception ex) {
                log.error("Unable to fetch containers from '${hostConfiguration.hostname}'", ex)
                status = "Disconnected"
            }

            return [
                    id: hostConfiguration.id,
                    name: hostConfiguration.name,
                    hostname: hostConfiguration.hostname,
                    status: status,
                    serviceInterfaces: hostContainers?.collect { new ServiceInstance().withDockerContainer(it) }
            ]
        }
    }

    private List<Map<String, Object>> getAllHosts() {
        List<String> activeTiers = PropertyTypes.Active_Tiers.getStringListValue();
        List<Map<String, Object>> answer = [];

        environmentController.getAllEnvironments().each { tierName, environmentList ->
            if (activeTiers.contains(tierName)) {
                environmentList.each { environmentName ->
                    DockerEnvironmentConfiguration envConfiguration = new DockerEnvironmentConfiguration(tierName, environmentName);
                    answer.addAll(envConfiguration.getServers());
                }
            }
        }

        return answer;
    }

}
