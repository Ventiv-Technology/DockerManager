package org.ventiv.docker.manager.controller

import com.github.dockerjava.api.model.Container
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.model.EnvironmentConfiguration
import org.ventiv.docker.manager.model.ServerConfiguration
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
                    serviceInterfaces: hostContainers?.collect { new ServiceInstance().withDockerContainer(it) }
            ]
        }
    }

    private List<ServerConfiguration> getAllHosts() {
        List<ServerConfiguration> answer = [];
        environmentController.getTiers().each { String tierId, List<EnvironmentConfiguration> envConfigurations ->
            envConfigurations.each {
                if (it && it.getServers()) answer.addAll(it.getServers());
            }
        }

        return answer;
    }

}
