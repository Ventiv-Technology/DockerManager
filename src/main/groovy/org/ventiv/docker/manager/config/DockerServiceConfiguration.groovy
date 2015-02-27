package org.ventiv.docker.manager.config

import groovy.util.logging.Slf4j
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.yaml.snakeyaml.Yaml

import javax.annotation.PostConstruct

/**
 * Created by jcrygier on 2/25/15.
 */
@Slf4j
@Service
class DockerServiceConfiguration {

    Map<String, Object> configuration;

    @PostConstruct
    public void readConfiguration() {
        Yaml yaml = new Yaml()

        Resource resource = new ClassPathResource("/data/env-config/services.yml")

        if (log.isDebugEnabled()) {
            log.debug("Loading from YAML: " + resource);
        }

        configuration = (Map<String, Object>) yaml.load(resource.getInputStream());
    }

    public List<String> getServiceNames() {
        return configuration.services*.name;
    }

    public Map<String, Object> getServiceConfiguration(String serviceName) {
        return configuration.services.find { it.name == serviceName }
    }

}
