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
package org.ventiv.docker.manager.service

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.model.EnvironmentConfiguration
import org.yaml.snakeyaml.Yaml

import javax.annotation.PostConstruct

/**
 * Service to read and keep track of the environment configurations
 */
@CompileStatic
@Slf4j
@Service
class EnvironmentConfigurationService {

    List<EnvironmentConfiguration> allEnvironments = []

    @javax.annotation.Resource DockerManagerConfiguration props;
    @javax.annotation.Resource ResourceWatcherService resourceWatcherService;

    @PostConstruct
    public void loadConfigurationFromFile() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        resolver.getResources(props.config.location + "/tiers/**/*.yml").each { Resource environmentConfigurationResource ->
            resourceWatcherService.watchResource(environmentConfigurationResource, this.&readConfiguration)
        }
    }

    public void readConfiguration(Resource resource) {
        Yaml yaml = new Yaml()

        if (log.isDebugEnabled()) {
            log.debug("Loading environment definition from YAML: " + resource);
        }

        String tierName = resource.getFile().getParentFile().getName();
        String environmentId = resource.getFilename().replaceAll("\\.yml", "")

        EnvironmentConfiguration environmentConfiguration = yaml.loadAs(resource.getInputStream(), EnvironmentConfiguration)
        environmentConfiguration.setTierName(tierName);
        environmentConfiguration.setId(environmentId);

        allEnvironments.remove(getEnvironment(tierName, environmentId));
        allEnvironments << environmentConfiguration
    }

    public EnvironmentConfiguration getEnvironment(String tierName, String environmentName) {
        return allEnvironments.find { it.getTierName() == tierName && it.getId() == environmentName }
    }

}
