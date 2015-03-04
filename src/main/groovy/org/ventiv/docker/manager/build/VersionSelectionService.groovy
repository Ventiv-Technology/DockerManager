package org.ventiv.docker.manager.build

import com.jayway.jsonpath.JsonPath
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.model.DockerTag
import org.ventiv.docker.manager.service.DockerRegistryApiService

import javax.annotation.Resource

/**
 * Created by jcrygier on 2/25/15.
 */
@Service
class VersionSelectionService {

    @Resource DockerRegistryApiService dockerRegistryApiService;

    public List<String> getPossibleVersions(def serviceConfiguration) {
        if (serviceConfiguration.build?.versionSelection)  // We know how to build an image
            return getVersionsFromConfig(serviceConfiguration.build.versionSelection);
        else if (serviceConfiguration.image) {             // We already have a docker images
            DockerTag tag = new DockerTag(serviceConfiguration.image);

            if (tag.tag)  // We've specified a version in the config
                return [ tag.tag ]
            else if (tag.registry)                  // We need to query the Docker Remote API to get the list of versions
                return dockerRegistryApiService.getRegistry(tag).listRepositoryTags(tag.namespace, tag.repository).keySet() as List<String>;
        }

        return [];
    }

    private List<String> getVersionsFromConfig(def versionSelection) {
        List<String> versions = [];

        if (versionSelection.uri && versionSelection.jsonPath)
            versions = JsonPath.read(new URL(versionSelection.uri), versionSelection.jsonPath)

        if (versionSelection.filters) {
            versionSelection.filters.each { filterDef ->
                if (filterDef.groovy) {
                    versions = versions.collect {
                        return Eval.me('value', it, filterDef.groovy)
                    }
                }
            }
        }

        return versions.sort().reverse();
    }

}
