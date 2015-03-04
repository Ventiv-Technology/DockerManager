package org.ventiv.docker.manager.controller

import feign.FeignException
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.api.DockerRegistry
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.model.DockerTag
import org.ventiv.docker.manager.model.ImageLayerInformation
import org.ventiv.docker.manager.model.ServiceConfiguration
import org.ventiv.docker.manager.service.DockerRegistryApiService

import javax.annotation.Resource

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

}
