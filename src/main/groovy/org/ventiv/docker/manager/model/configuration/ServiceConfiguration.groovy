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
package org.ventiv.docker.manager.model.configuration

import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.model.DockerTag
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
    Integer maxPossibleVersions = 200;

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
    Collection<PortMappingConfiguration> containerPorts;

    /**
     * Environment variables to set when constructing a ServiceInstance.  These variables can be thought of as generic,
     * simply to set up the service.  The 'specific' environment variables may be defined in ServiceInstanceConfiguration
     *
     * @see ServiceInstanceConfiguration for variable replacement definition
     */
    @Nullable
    Map<String, String> environment;

    /**
     * List of Volumes that are allowed for this container, each mapped to a type.  This will be used to match up
     * to a server's mapping to make the assignment at deployment time.
     */
    @Nullable
    List<VolumeMappingConfiguration> containerVolumes;

    /**
     * Additional Metrics for this service.  This is a way to get additional information about a running service, and possibly
     * display it on the UI.
     */
    @Nullable
    List<AdditionalMetricsConfiguration> additionalMetrics;

    /**
     * Links between containers.  Currently, it's very simple and will error if a container is linked to a container on
     * a different docker host.  In the future, Docker Manager might automatically implement the ambassador pattern:
     * https://docs.docker.com/articles/ambassador_pattern_linking/
     */
    @Nullable
    List<LinkConfiguration> links;

    /**
     * Sets the maximum memory that the container may have.  Format: <number><optional unit>, where unit = b, k, m or g
     */
    @Nullable
    String memoryLimit;

    /**
     * Sets the total memory limit that the container may have (memory + swap).  Format: <number><optional unit>, where unit = b, k, m or g.
     * If this is specified, you MUST specify memoryLimit, and this value MUST be larger than memoryLimit.
     */
    @Nullable
    String memorySwapLimit;

    public List<String> getPossibleVersions(String branch, String query = null) {
        List<String> answer = [];

        if (getBuild()?.getVersionSelection())  // We know how to build an image
            answer = getBuild().getVersionSelection().getPossibleVersions(branch);
        else if (getImage()) {             // We already have a docker images
            DockerTag tag = new DockerTag(getImage());

            if (tag.getTag())  // We've specified a version in the config
                answer = [ tag.getTag() ]
            else if (tag.getRegistry())                  // We need to query the Docker Remote API to get the list of versions
                answer = DockerManagerApplication.getApplicationContext().getBean(DockerRegistryApiService).getRegistry(tag).listRepositoryTags(tag.namespace, tag.repository).keySet() as List<String>;
        }

        if (query) {
            answer = answer.findAll { it.contains(query) }
            if (answer.size() == 0)
                return []
        }

        Integer toTake = Math.min(maxPossibleVersions, answer.size()) - 1;
        return answer ? answer.sort(this.&stringToNumber).reverse()[0..toTake] : [ 'latest' ];
    }

    public boolean isBuildPossible() {
        return getBuild()?.getStages()
    }

    public boolean isNewBuildPossible() {
        return isBuildPossible() && !getBuild().getVersionSelection();
    }

    /**
     * Returns the pinned version, if this Service is configured to be pinned to a particular version.  Null otherwise.
     * @return
     */
    public String getPinnedVersion() {
        if (getImage()) {
            DockerTag tag = new DockerTag(getImage());
            if (tag.getTag())
                return tag.getTag()
        }

        return null;
    }

    public String getUrl() {
        // Attempt to auto-derive the URL if an HTTP or HTTPS are provided
        if (!url) {
            PortMappingConfiguration port = getContainerPorts()?.find { it.getType() == 'http' }
            if (!port) port = getContainerPorts()?.find { it.getType() == 'https' }

            if (port)
                url = "${port.getType()}://\${server}:\${port.http}"
        }

        return url;
    }

    private Long stringToNumber(String versionNumber) {
        String strippedAlphas = versionNumber.replaceAll('[^\\d]', '');
        if (strippedAlphas)
            return Long.parseLong(strippedAlphas);
        else
            return Long.MAX_VALUE;
    }

}
