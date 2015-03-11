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

import java.util.List;

/**
 * Created by jcrygier on 2/28/15.
 */
public interface ServiceSelectionAlgorithm {

    public ServiceInstance getAvailableServiceInstance(String serviceName, List<ServiceInstance> allServiceInstances, String applicationId);

    public static class Util {

        public static ServiceInstance getAvailableServiceInstance(String serviceName, List<ServiceInstance> allServiceInstances, ApplicationDetails applicationDetails) {
            ServiceSelectionAlgorithm instance = new NextAvailableServiceSelectionAlgorithm();
            if (applicationDetails.getApplicationConfiguration().getServiceSelectionAlgorithm() != null) {
                try {
                    instance = applicationDetails.getApplicationConfiguration().getServiceSelectionAlgorithm().newInstance();
                } catch (Exception ex) {
                    // TODO: Log exception
                }
            }

            ServiceInstance answer = instance.getAvailableServiceInstance(serviceName, allServiceInstances, applicationDetails.getId());
            if (answer == null)
                throw new NoAvailableServiceException(serviceName, applicationDetails);

            return answer;
        }

    }

}
