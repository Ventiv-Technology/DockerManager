package org.ventiv.docker.manager.model

import org.ventiv.docker.manager.DockermanagerApplication
import org.ventiv.docker.manager.service.DockerRegistryApiService

import javax.annotation.Nullable
import javax.validation.constraints.NotNull

/**
 * Created by jcrygier on 3/4/15.
 */
class ServiceConfiguration {

    @NotNull
    String name;

    @NotNull
    String description;

    @NotNull
    String image;

    @Nullable
    Integer maxPossibleVersions = 20;

    /**
     * Url to get to this Service, to be populated with variables from a ServiceInstance at runtime.
     *
     * Variables Include:
     * - server: ServiceInstance.serverName
     * - port.<portType>: ServiceInstance.portDefinitions.find { it.type == portType }.hostPort
     */
    @Nullable
    String url;

    @Nullable
    ServiceBuildConfiguration build;

    @Nullable
    Collection<PortMapptingConfiguration> containerPorts;

    @Nullable
    Map<String, String> environment;

    public List<String> getPossibleVersions() {
        List<String> answer = [];

        if (getBuild()?.getVersionSelection())  // We know how to build an image
            answer = getBuild().getVersionSelection().getPossibleVersions();
        else if (getImage()) {             // We already have a docker images
            DockerTag tag = new DockerTag(getImage());

            if (tag.getTag())  // We've specified a version in the config
                answer = [ tag.getTag() ]
            else if (tag.getRegistry())                  // We need to query the Docker Remote API to get the list of versions
                answer = DockermanagerApplication.getApplicationContext().getBean(DockerRegistryApiService).getRegistry(tag).listRepositoryTags(tag.namespace, tag.repository).keySet() as List<String>;
        }

        Integer toTake = Math.min(maxPossibleVersions, answer.size()) - 1;
        return answer.sort().reverse()[0..toTake];
    }

}
