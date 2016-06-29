package org.ventiv.docker.manager.model

import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.ContainerPort
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.model.configuration.PortMappingConfiguration
import org.ventiv.docker.manager.model.configuration.ServiceConfiguration
import spock.lang.Specification

/**
 * Created by jcrygier on 6/29/16.
 */
class ServiceInstanceTest extends Specification {

    def 'can generate URL from template'() {
        when:
        def serviceConfigurations = [
                new ServiceConfiguration([
                        name: 'test-service',
                        description: 'Testing Service',
                        containerPorts: [
                            new PortMappingConfiguration([type: 'http', port: 9080])
                        ]
            ])
        ]
        DockerServiceConfiguration serviceConfiguration = new DockerServiceConfiguration([config: serviceConfigurations]);
        ServiceInstance si = new ServiceInstance([dockerServiceConfiguration: serviceConfiguration, serverName: 'localhost']).withDockerContainer([
                names: ["/localhost.boot2docker.test-app.test-service.1"] as String[],
                status: "Up 10 Minutes",
                id: "a2a9c0d6d7f5b2fdcac7ec6da29fd78395c50b458457389bd3efb2d1a2eb1aab",
                image: "hello-world:latest",
                created: (System.currentTimeMillis() - 100000) / 1000,
                ports: [
                        [ip: "0.0.0.0", privatePort: 9080, publicPort: 9090, type: 'tcp'] as ContainerPort
                ] as ContainerPort[],
        ] as Container)

        then:
        si.getUrl() == "http://localhost:9090"
    }

}
