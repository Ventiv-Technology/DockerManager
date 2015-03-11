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
package org.ventiv.docker.manager.model

import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.service.DockerRegistryApiService

import javax.annotation.Nullable
import javax.validation.constraints.NotNull

/**
 * Created by jcrygier on 3/4/15.
 */
class ServiceConfiguration {

    /**
     * Name of this service.  This is used elsewhere to refer to this configuration.
     */
    @NotNull
    String name;

    /**
     * Description of this service to be shown in the UI.
     */
    @NotNull
    String description;

    /**
     * Docker Image for this service.  This is in the standard Docker format, and the 'tag' (or version) is optional.
     * If the tag is omitted, it will prompt the system to search the assigned registry for a list of possible versions.
     */
    @NotNull
    String image;

    /**
     * Maximum possible versions to present in the UI for selection.  Defaults to 20, but can be any number.  The smaller
     * the better, because of the version aggregation logic.  For example, if there are two different services with the top
     * 20 versions being the same, they may be assigned at the application level.  However, if the 21st version is different,
     * then each service must be selected manually.
     */
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

    /**
     * Build information for this service.  This will contain information on how to construct a Docker Image.
     */
    @Nullable
    ServiceBuildConfiguration build;

    /**
     * Service definition of ports, and mapping them to 'types'.  The actual ports that are assigned to the host is part
     * of the ServiceInstance, and not the ServiceConfiguration.
     */
    @Nullable
    Collection<PortMapptingConfiguration> containerPorts;

    /**
     * Environment variables to set when constructing a ServiceInstance.  These variables can be thought of as generic,
     * simply to set up the service.  The 'specific' environment variables may be defined in ServiceInstanceConfiguration
     *
     * @see org.ventiv.docker.manager.model.ServiceInstanceConfiguration for variable replacement definition
     */
    @Nullable
    Map<String, String> environment;

    /**
     * List of Volumes that are allowed for this container, each mapped to a type.  This will be used to match up
     * to a server's mapping to make the assignment at deployment time.
     */
    @Nullable
    List<VolumeMapping> containerVolumes;

    public List<String> getPossibleVersions() {
        List<String> answer = [];

        if (getBuild()?.getVersionSelection())  // We know how to build an image
            answer = getBuild().getVersionSelection().getPossibleVersions();
        else if (getImage()) {             // We already have a docker images
            DockerTag tag = new DockerTag(getImage());

            if (tag.getTag())  // We've specified a version in the config
                answer = [ tag.getTag() ]
            else if (tag.getRegistry())                  // We need to query the Docker Remote API to get the list of versions
                answer = DockerManagerApplication.getApplicationContext().getBean(DockerRegistryApiService).getRegistry(tag).listRepositoryTags(tag.namespace, tag.repository).keySet() as List<String>;
        }

        Integer toTake = Math.min(maxPossibleVersions, answer.size()) - 1;
        return answer ? answer.sort().reverse()[0..toTake] : [ 'latest' ];
    }

}
