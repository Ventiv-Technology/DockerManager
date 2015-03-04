package org.ventiv.docker.manager.config

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.model.ServiceConfiguration
import org.yaml.snakeyaml.Yaml

/**
 * Created by jcrygier on 2/25/15.
 */
@Slf4j
@Service
@CompileStatic
class DockerServiceConfiguration {

    private List<ServiceConfiguration> config;

    public void readConfiguration() {
        Yaml yaml = new Yaml()

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        Resource resource = resolver.getResource(PropertyTypes.Environment_Configuration_Location.getValue() + "/services.yml")

        if (log.isDebugEnabled()) {
            log.debug("Loading service definition from YAML: " + resource);
        }

        config = yaml.loadAs(resource.getInputStream(), ServiceConfigurationFile).getServices();
    }

    public List<String> getServiceNames() {
        return getConfiguration()*.getName();
    }

    public ServiceConfiguration getServiceConfiguration(String serviceName) {
        return getConfiguration().find { it.name == serviceName }
    }

    public List<ServiceConfiguration> getConfiguration() {
        if (config == null)
            readConfiguration();

        return config;
    }

    public static final class ServiceConfigurationFile {
        List<ServiceConfiguration> services;
    }

}
