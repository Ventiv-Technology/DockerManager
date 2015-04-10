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
package org.ventiv.docker.manager.controller

import feign.FeignException
import org.apache.commons.io.IOUtils
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.api.DockerRegistry
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.model.configuration.AdditionalMetricsConfiguration
import org.ventiv.docker.manager.model.DockerTag
import org.ventiv.docker.manager.model.configuration.ImageLayerInformation
import org.ventiv.docker.manager.model.configuration.ServiceConfiguration
import org.ventiv.docker.manager.service.DockerRegistryApiService

import javax.annotation.Resource
import javax.servlet.http.HttpServletResponse

/**
 * Docker Service Controller, this will control anything to do with a docker 'service'.  A Service is a defined docker image
 * (or how to create an image).  It will also contain information about ports, volumes, etc...
 */
@RequestMapping("/api/service")
@RestController
class DockerServiceController {

    @Resource DockerServiceConfiguration dockerServiceConfiguration;
    @Resource DockerRegistryApiService dockerRegistryApiService;

    @RequestMapping()
    public List<String> getServiceNames() {
        dockerServiceConfiguration.getServiceNames()
    }

    @RequestMapping("/{serviceName}")
    public List<String> getAvailableVersions(@PathVariable("serviceName") String serviceName) {
        ServiceConfiguration serviceConfig = dockerServiceConfiguration.getServiceConfiguration(serviceName);
        if (serviceConfig) {
            return serviceConfig.getPossibleVersions();
        }
    }

    @RequestMapping("/{serviceName}/{versionNumber:.*}")
    public def getServiceInformation(@PathVariable("serviceName") String serviceName, @PathVariable("versionNumber") String versionNumber) {
        def serviceConfig = dockerServiceConfiguration.getServiceConfiguration(serviceName);

        DockerTag tag = new DockerTag(serviceConfig.image);
        tag.tag = versionNumber;

        DockerRegistry dockerRegistry = dockerRegistryApiService.getRegistry(tag);

        String imageHash = null;
        try {
            def repositoryTags = dockerRegistry.listRepositoryTags(tag.namespace, tag.repository);
            imageHash = repositoryTags?.get(versionNumber);
        } catch (FeignException ignored) {
            // Generally means that this image hasn't been built / pushed
        }

        def imageInformation;
        if (imageHash) {
            ImageLayerInformation layerInfo = dockerRegistry.getImageLayer(imageHash);
            List<Integer> serviceExposedPorts = serviceConfig.containerPorts*.port;
            List<Integer> containerExposedPorts = layerInfo.getExposedPorts();

            imageInformation = [
                    // TODO: Add volumes
                    environment: layerInfo.getEnvironmentVariables(),
                    nonExposedPorts: containerExposedPorts.findAll { !serviceExposedPorts.contains(it) },
                    extraExposedPorts: serviceExposedPorts.findAll { !containerExposedPorts.contains(it) }
            ]
        }

        return [
                containerPorts: serviceConfig.containerPorts,
                imageBuildStatus: imageHash ? 'PushedInRegistry' : 'NotBuilt',
                imageHash: imageHash,
                imageInformation: imageInformation,
                tag: tag
        ]
    }

    @RequestMapping("/{serviceName}/metrics/{metricName}/button")
    public void getAdditionalMetricsButtonPartial(@PathVariable("serviceName") String serviceName, @PathVariable("metricName") String metricName, HttpServletResponse response) {
        ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(serviceName);
        AdditionalMetricsConfiguration metricsConfiguration = serviceConfiguration?.getAdditionalMetrics()?.find { it.getName() == metricName }
        if (metricsConfiguration) {
            response.setContentType(MediaType.TEXT_HTML_VALUE);

            if (metricsConfiguration.getUi().getButtonPartial()) {
                org.springframework.core.io.Resource resource = new FileSystemResource(metricsConfiguration.getUi().getButtonPartial())
                IOUtils.copy(resource.getInputStream(), response.getOutputStream());
            } else if (metricsConfiguration.getUi().getButtonTemplate())
                response.getOutputStream() << metricsConfiguration.getUi().getButtonTemplate()
        }
    }

    @RequestMapping("/{serviceName}/metrics/{metricName}/details")
    public void getAdditionalMetricsDetailsPartial(@PathVariable("serviceName") String serviceName, @PathVariable("metricName") String metricName, HttpServletResponse response) {
        ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(serviceName);
        AdditionalMetricsConfiguration metricsConfiguration = serviceConfiguration?.getAdditionalMetrics()?.find { it.getName() == metricName }
        if (metricsConfiguration) {
            response.setContentType(MediaType.TEXT_HTML_VALUE);

            if (metricsConfiguration.getUi().getDetailsPartial()) {
                org.springframework.core.io.Resource resource = new FileSystemResource(metricsConfiguration.getUi().getDetailsPartial())
                IOUtils.copy(resource.getInputStream(), response.getOutputStream());
            } else if (metricsConfiguration.getUi().getDetailsTemplate())
                response.getOutputStream() << metricsConfiguration.getUi().getDetailsTemplate()
        }
    }

}
