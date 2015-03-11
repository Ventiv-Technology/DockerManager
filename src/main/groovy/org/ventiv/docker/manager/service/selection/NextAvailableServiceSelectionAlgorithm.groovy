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

import org.ventiv.docker.manager.exception.NoAvailableServiceException
import org.ventiv.docker.manager.model.ServiceInstance

/**
 * Created by jcrygier on 2/28/15.
 */
class NextAvailableServiceSelectionAlgorithm implements ServiceSelectionAlgorithm {

    @Override
    ServiceInstance getAvailableServiceInstance(String serviceName, List<ServiceInstance> allServiceInstances, String applicationId) throws NoAvailableServiceException {
        return allServiceInstances.find { it.getName() == serviceName && it.getStatus() == ServiceInstance.Status.Available }
    }

}
