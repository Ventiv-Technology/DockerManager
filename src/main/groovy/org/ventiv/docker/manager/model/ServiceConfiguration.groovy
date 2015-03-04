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

    public List<String> getPossibleVersions() {
        if (getBuild()?.getVersionSelection())  // We know how to build an image
            return getBuild().getVersionSelection().getPossibleVersions();
        else if (getImage()) {             // We already have a docker images
            DockerTag tag = new DockerTag(getImage());

            if (tag.getTag())  // We've specified a version in the config
                return [ tag.getTag() ]
            else if (tag.getRegistry())                  // We need to query the Docker Remote API to get the list of versions
                return DockermanagerApplication.getApplicationContext().getBean(DockerRegistryApiService).getRegistry(tag).listRepositoryTags(tag.namespace, tag.repository).keySet() as List<String>;
        }

        return [];
    }

}
