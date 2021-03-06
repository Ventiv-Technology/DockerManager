/**
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
package org.ventiv.docker.manager.service.selection;

import org.ventiv.docker.manager.exception.NoAvailableServiceException;
import org.ventiv.docker.manager.model.ApplicationDetails;
import org.ventiv.docker.manager.model.ServiceInstance;

import java.util.Collection;

/**
 * Created by jcrygier on 2/28/15.
 */
public interface ServiceSelectionAlgorithm {

    public ServiceInstance getAvailableServiceInstance(String serviceName, Collection<ServiceInstance> allServiceInstances, ApplicationDetails applicationDetails);

    public static class Util {

        private static ServiceSelectionAlgorithm defaultImplementation = new OverridableDistributedServerServiceSelectionAlgorithm();

        public static ServiceInstance getAvailableServiceInstance(String serviceName, Collection<ServiceInstance> allServiceInstances, ApplicationDetails applicationDetails) {
            ServiceSelectionAlgorithm instance = defaultImplementation;

            // Check if we have defined a custom selection algorithm, and it's different
            if (applicationDetails.getApplicationConfiguration().getServiceSelectionAlgorithm() != null && applicationDetails.getApplicationConfiguration().getServiceSelectionAlgorithm() != defaultImplementation.getClass()) {
                try {
                    instance = applicationDetails.getApplicationConfiguration().getServiceSelectionAlgorithm().newInstance();
                } catch (Exception ex) {
                    // TODO: Log exception
                }
            }

            ServiceInstance answer = instance.getAvailableServiceInstance(serviceName, allServiceInstances, applicationDetails);
            if (answer == null)
                throw new NoAvailableServiceException(serviceName, applicationDetails);

            return answer;
        }

    }

}
