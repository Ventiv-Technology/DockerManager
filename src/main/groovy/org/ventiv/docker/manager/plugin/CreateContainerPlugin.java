package org.ventiv.docker.manager.plugin;

import org.ventiv.docker.manager.model.ServiceInstance;

/**
 * A plugin that will modify a Service Instance before creating the container.
 */
public interface CreateContainerPlugin {

    public void doWithServiceInstance(ServiceInstance serviceInstance);

}
