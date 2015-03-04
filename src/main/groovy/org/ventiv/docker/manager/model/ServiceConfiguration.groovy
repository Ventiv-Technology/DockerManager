package org.ventiv.docker.manager.model

import org.ventiv.docker.manager.DockermanagerApplication
import org.ventiv.docker.manager.service.DockerRegistryApiService

/**
 * Created by jcrygier on 3/4/15.
 */
class ServiceConfiguration {

    String name;
    String description;
    String image;
    ServiceBuildConfiguration build;
    Collection<PortMapptingConfiguration> containerPorts;
    Map<String, String> environment;
    Integer maxPossibleVersions = 20;

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
