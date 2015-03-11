/*
 * Copyright (c) 2014 - 2015 Ventiv Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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

    @javax.annotation.Resource DockerManagerConfiguration props;
    private List<ServiceConfiguration> config;

    public void readConfiguration() {
        Yaml yaml = new Yaml()

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        Resource resource = resolver.getResource(props.config.location + "/services.yml")

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
