package org.ventiv.docker.manager.service

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig
import org.springframework.stereotype.Service

import javax.annotation.Resource

/**
 * Gets a DockerClient for a given host.  Will attempt to use default values, but will override with application properties
 * configured via spring boot.
 *
 * - docker.client.<hostName>.apiVersion: Default: 1.17
 * - docker.client.<hostName>.uri: Default: https://<hostName>:2376
 * - docker.client.<hostName>.certPath: Default: ./certs/<hostName>
 */
@Service
class DockerService {

    @Resource PropertyService props;

    public DockerClient getDockerClient(String hostName) {
        String apiVersion = props["docker.client.${hostName}.apiVersion"] ?: "1.17"
        String uri = props["docker.client.${hostName}.uri"] ?: "https://${hostName}:2376"
        String certs = props["docker.client.${hostName}.certPath"] ?: "./certs/${hostName}"
        certs = certs.replaceAll('~', System.getProperty('user.home'))

        DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
            .withVersion(apiVersion)
            .withUri(uri)
            .withDockerCertPath(certs)
            .build();

        return DockerClientBuilder.getInstance(config).build();
    }

}
