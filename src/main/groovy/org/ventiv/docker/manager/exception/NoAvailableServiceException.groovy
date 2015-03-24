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
package org.ventiv.docker.manager.exception

import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.model.ApplicationDetails
import org.ventiv.docker.manager.model.PortMappingConfiguration
import org.ventiv.docker.manager.model.ServiceConfiguration

/**
 * Created by jcrygier on 2/27/15.
 */
class NoAvailableServiceException extends RuntimeException {

    NoAvailableServiceException(String serviceName, ApplicationDetails applicationDetails) {
        super("No available service '$serviceName'.\nPlease add:\n\n- type: ${serviceName}\n  portMappings:\n${getExampleMappings(serviceName)}\n\nas a child of eligibleServices of a server in: ${DockerManagerApplication.props.config.location}/tiers/${applicationDetails.getTierName()}/${applicationDetails.getEnvironmentName()}.yml")
    }

    private static String getExampleMappings(String serviceName) {
        ServiceConfiguration serviceConfiguration = DockerManagerApplication.getApplicationContext().getBean(DockerServiceConfiguration).getServiceConfiguration(serviceName);

        StringBuilder sb = new StringBuilder();
        serviceConfiguration.getContainerPorts().each { PortMappingConfiguration portMap ->
            sb.append("    - type: ").append(portMap.getType()).append("\n")
            sb.append("      port: <Available Port on Host For ").append(portMap.getPort()).append(">").append("\n")
        }

        return sb.toString();
    }
}
