package org.ventiv.docker.manager.config

import groovy.util.logging.Slf4j
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.yaml.snakeyaml.Yaml

/**
 * Created by jcrygier on 2/25/15.
 */
@Slf4j
class DockerEnvironmentConfiguration {

    Map<String, Object> configuration;

    public DockerEnvironmentConfiguration(String tierName, String environmentName) {
        Yaml yaml = new Yaml()

        Resource resource = new ClassPathResource("/data/env-config/tiers/$tierName/${environmentName}.yml")

        if (log.isDebugEnabled()) {
            log.debug("Loading Environment Configuration from YAML: " + resource);
        }

        configuration = (Map<String, Object>) yaml.load(resource.getInputStream());
    }

    List<Map<String, Object>> getServers() {
        if (configuration)
            return configuration.servers
        else
            return []
    }

    Set<String> getHostnames() {
        if (configuration)
            return configuration.servers*.hostname;
        else
            return [];
    }

}
