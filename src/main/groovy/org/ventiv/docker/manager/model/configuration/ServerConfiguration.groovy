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
import org.ventiv.docker.manager.config.DockerServiceConfiguration

import javax.annotation.Nullable
import javax.validation.constraints.NotNull

/**
 * Created by jcrygier on 3/3/15.
 */
class ServerConfiguration {

    /**
     * Unique id for the host
     */
    @NotNull
    String id;

    /**
     * Name of the host to be shown in the UI.
     */
    @NotNull
    String description;

    /**
     * DNS Name of the host.
     */
    @NotNull
    String hostname;

    /**
     * Should we resolve this server to an IP and add it to /etc/hosts of any container created on this host?
     * This is mostly a workaround for boot2docker, since it cannot resolve to a real hostname.
     */
    @Nullable
    Boolean resolveHostname = false;

    /**
     * Should this host be allowed to execute docker builds?  Default is no, and will determine build host
     * from overall application configuration.  If this is enabled, it will first try to use this host to build, before
     * using the overall application configuration.
     */
    @Nullable
    Boolean buildEnabled = false;

    /**
     * Extra host mappings to put in ALL container's /etc/hosts file for resolution
     */
    @NotNull
    Map<String, String> extraHosts = [:]

    /**
     * The list of services that are eligible to run on this host.
     */
    @NotNull
    List<EligibleServiceConfiguration> eligibleServices;

    /**
     * Setter for eligibleServices.  This will do the denormalization of creating many EligibleServiceConfiguraion objects
     * if ports is specified instead of port (allowing for ranges).
     */
    public void setEligibleServices(List<EligibleServiceConfiguration> eligibleServiceConfigurations) {
        this.eligibleServices = eligibleServiceConfigurations;

        List<EligibleServiceConfiguration> toAdd = [];
        List<EligibleServiceConfiguration> toRemove = [];
        eligibleServiceConfigurations.each { EligibleServiceConfiguration eligibleServiceConfiguration ->
            // Denormalize the ports option into a Map [Type -> List<PortNumber>]
            Map<String, List<Integer>> newMappings = eligibleServiceConfiguration.getPortMappings().findAll { it.getPorts() != null }.collectEntries { PortMappingConfiguration portMappingConfiguration ->
                return [portMappingConfiguration.getType(),
                        portMappingConfiguration.getPorts().split(",").collect {
                    if (it.contains("-")) {
                        String[] highLow = it.split("-");
                        return [Integer.parseInt(highLow[0])..Integer.parseInt(highLow[1])]
                    } else
                        return Integer.parseInt(it);
                }.flatten()]
            }

            if (newMappings) {
                // Validate that the lists are all the same size
                if (newMappings.collect { return it.getValue().size() }.unique().size() != 1)
                    throw new IllegalArgumentException("Port ranges must be equal for service ${eligibleServiceConfiguration.getType()}")

                // First, create shells of EligibleServiceConfiguration
                List<EligibleServiceConfiguration> newServices = newMappings.values().first().collect {
                    new EligibleServiceConfiguration(type: eligibleServiceConfiguration.getType(), portMappings: [])
                }

                // Now, populate
                newServices.eachWithIndex { EligibleServiceConfiguration newConfig, Integer idx ->
                    newConfig.setPortMappings(newMappings.collect { String type, List<Integer> ports ->
                        new PortMappingConfiguration(type: type, port: ports[idx]);
                    })
                }

                // Now, indicate that we need too add / remove these
                toAdd.addAll(newServices)
                toRemove << eligibleServiceConfiguration;
            }
        }

        // Do the adding / removing
        eligibleServices.addAll(toAdd);
        eligibleServices.removeAll(toRemove);

        // Calculate the Service Instance Numbers
        this.eligibleServices.groupBy { it.getType() }.each { String type, List<EligibleServiceConfiguration> services ->
            services.eachWithIndex { EligibleServiceConfiguration entry, int i -> entry.setInstanceNumber(i+1) }
        }

        // Populate the Service Name Description
        this.eligibleServices.each {
            it.setServiceNameDescription(DockerManagerApplication.getApplicationContext()?.getBean(DockerServiceConfiguration)?.getServiceConfiguration(it.getType())?.getDescription());
        }
    }

}
