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
