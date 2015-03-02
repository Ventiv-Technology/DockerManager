package org.ventiv.docker.manager.service.selection;

import org.ventiv.docker.manager.exception.NoAvailableServiceException;
import org.ventiv.docker.manager.model.ServiceInstance;

import java.util.List;
import java.util.Map;

/**
 * Created by jcrygier on 2/28/15.
 */
public interface ServiceSelectionAlgorithm {

    public ServiceInstance getAvailableServiceInstance(String serviceName, List<ServiceInstance> allServiceInstances, String applicationId);

    public static class Util {

        public static ServiceInstance getAvailableServiceInstance(String serviceName, List<ServiceInstance> allServiceInstances, Map<String, Object> applicationConfiguration) {
            ServiceSelectionAlgorithm instance = new NextAvailableServiceSelectionAlgorithm();
            if (applicationConfiguration.containsKey("serviceSelectionAlgorithm")) {
                try {
                    instance = (ServiceSelectionAlgorithm) Class.forName(applicationConfiguration.get("serviceSelectionAlgorithm").toString()).newInstance();
                } catch (Exception ex) {
                    // TODO: Log exception
                }
            }

            ServiceInstance answer = instance.getAvailableServiceInstance(serviceName, allServiceInstances, applicationConfiguration.get("id").toString());
            if (answer == null)
                throw new NoAvailableServiceException(serviceName, applicationConfiguration.get("tierName").toString(), applicationConfiguration.get("environmentName").toString(), applicationConfiguration.get("id").toString());

            return answer;
        }

    }

}
