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
