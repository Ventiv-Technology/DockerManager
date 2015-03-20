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
package org.ventiv.docker.manager.service.selection

import org.ventiv.docker.manager.model.ServiceInstance

/**
 * Created by jcrygier on 3/20/15.
 */
class DistributedServerServiceSelectionAlgorithm implements ServiceSelectionAlgorithm {

    @Override
    ServiceInstance getAvailableServiceInstance(String serviceName, List<ServiceInstance> allServiceInstances, String applicationId) {
        // First, lets group the service instances by server
        Map<String, List<ServiceInstance>> groupedByServer = allServiceInstances.groupBy { it.getServerName() }

        // Now, lets only pick servers that have at least one service available
        Map<String, List<ServiceInstance>> serversWithAvailable = groupedByServer.findAll { String serverName, List<ServiceInstance> serverInstances ->
            return serverInstances.find { it.getName() == serviceName && it.getStatus() == ServiceInstance.Status.Available }
        }

        // Now, sort the remaining to see which ones have the LEAST number of this service allocated
        Map<String, List<ServiceInstance>> sortedServers = serversWithAvailable.sort { Map.Entry<String, List<ServiceInstance>> entry ->
            List<ServiceInstance> serverInstances = entry.getValue();

            return serverInstances.findAll { it.getName() == serviceName && it.getStatus() != ServiceInstance.Status.Available }.size();
        }

        // Finally, pick one off the top!
        if (sortedServers.entrySet()) {
            List<ServiceInstance> bestServerAllInstances = sortedServers.entrySet().first()?.getValue()
            return bestServerAllInstances?.find { it.getName() == serviceName && it.getStatus() == ServiceInstance.Status.Available }
        }

        return null;
    }

}
