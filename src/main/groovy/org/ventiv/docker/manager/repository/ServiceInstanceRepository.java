package org.ventiv.docker.manager.repository;

import org.springframework.data.repository.CrudRepository;
import org.ventiv.docker.manager.model.ServiceInstance;

import java.util.List;

/**
 * Created by jcrygier on 2/26/15.
 */
public interface ServiceInstanceRepository extends CrudRepository<ServiceInstance, Long> {

    List<ServiceInstance> findByTierAndEnvironmentName(String tier, String environmentName);

}
