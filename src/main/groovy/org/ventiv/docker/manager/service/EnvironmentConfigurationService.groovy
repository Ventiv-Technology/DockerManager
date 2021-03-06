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
import org.ventiv.docker.manager.model.ApplicationThumbnail
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.configuration.ApplicationConfiguration
import org.ventiv.docker.manager.model.configuration.EligibleServiceConfiguration
import org.ventiv.docker.manager.model.configuration.EnvironmentConfiguration
import org.ventiv.docker.manager.model.configuration.ServerConfiguration
import org.ventiv.docker.manager.repository.ApplicationThumbnailRepository
import org.ventiv.docker.manager.utils.YamlUtils

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
    @javax.annotation.Resource DockerService dockerService;
    @javax.annotation.Resource ApplicationThumbnailRepository applicationThumbnailRepository;

    List<Closure<?>> environmentChangeCallbacks = []

    @PostConstruct
    public void loadConfigurationFromFile() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        resolver.getResources(props.config.location + "/tiers/**/*.yml").each { Resource environmentConfigurationResource ->
            // Watch this resource for changes
            resourceWatcherService.watchResource(environmentConfigurationResource, this.&readConfiguration)

            // Also, load it immediately
            readConfiguration(environmentConfigurationResource);
        }
    }

    public void readConfiguration(Resource resource) {
        if (log.isDebugEnabled()) {
            log.debug("Loading environment definition from YAML: " + resource);
        }

        String tierName = resource.getFile().getParentFile().getName();
        String environmentId = resource.getFilename().replaceAll("\\.yml", "")

        EnvironmentConfiguration environmentConfiguration = YamlUtils.loadAs(resource, EnvironmentConfiguration)
        environmentConfiguration.setTierName(tierName);
        environmentConfiguration.setId(environmentId);

        allEnvironments.remove(getEnvironment(tierName, environmentId));
        allEnvironments << environmentConfiguration

        // If this is an active environment, connect to docker
        if (props.getActiveTiers() == null || props.getActiveTiers().contains(environmentConfiguration.getTierName())) {
            // Call over to DockerService so we can ensure that we've cached things as necessary
            environmentConfiguration.getServers()*.getHostname().unique().each { String hostName ->
                try {
                    dockerService.getDockerClient(hostName).pingCmd().exec();
                } catch (Exception ignored) {
                    log.warn("Unable to connect to host $hostName, removing it from the Environment Configuration")
                    environmentConfiguration.getServers().removeAll { ServerConfiguration serverConfiguration ->
                        serverConfiguration.getHostname() == hostName
                    }
                }
            }

            // Alert the presses!!!  We have an environment change!!!
            environmentChangeCallbacks.each { it.call(environmentConfiguration) }
        }
    }

    public Collection<EnvironmentConfiguration> getActiveEnvironments() {
        return allEnvironments.findAll { props.getActiveTiers() == null || props.getActiveTiers().contains(it.getTierName()) }
    }

    public EnvironmentConfiguration getEnvironment(String tierName, String environmentName) {
        return allEnvironments.find { it.getTierName() == tierName && it.getId() == environmentName }
    }

    public ApplicationConfiguration getApplication(String tierName, String environmentName, String applicationId) {
        return getEnvironment(tierName, environmentName).getApplications().find { it.getId() == applicationId};
    }

    public ApplicationThumbnail getApplicationThumbnail(String tierName, String environmentId, String applicationId) {
        ApplicationThumbnail applicationThumbnail = applicationThumbnailRepository.findByTierNameAndEnvironmentNameAndApplicationId(tierName, environmentId, applicationId);

        if (applicationThumbnail == null)
            applicationThumbnail = applicationThumbnailRepository.save(new ApplicationThumbnail(tierName: tierName, environmentName: environmentId, applicationId: applicationId));

        return applicationThumbnail;
    }

    public EligibleServiceConfiguration getEligibleServiceConfiguration(ServiceInstance serviceInstance) {
        return getEligibleServiceConfiguration(serviceInstance.getTierName(), serviceInstance.getEnvironmentName(), serviceInstance.getServerName(), serviceInstance.getName(), serviceInstance.getInstanceNumber());
    }

    public EligibleServiceConfiguration getEligibleServiceConfiguration(String tierName, String environmentName, String hostName, String serviceType, Integer instanceNumber) {
        EnvironmentConfiguration environmentConfiguration = getEnvironment(tierName, environmentName)
        if (environmentConfiguration) {
            ServerConfiguration serverConfiguration = environmentConfiguration.getServers().find { it.getHostname() == hostName }
            if (serverConfiguration) {
                return serverConfiguration.getEligibleServices().find { it.getType() == serviceType && it.getInstanceNumber() == instanceNumber }
            }
        }

        return null;
    }

}
