package org.ventiv.docker.manager.model

import org.ventiv.docker.manager.service.selection.ServiceSelectionAlgorithm

import javax.annotation.Nullable
import javax.validation.constraints.NotNull

/**
 * Data representing an Application's Configuration.  This differentiates from an ApplicationDetails object by being
 * a configuration as opposed to what is actually running.
 *
 * Generally configured via YAML.
 */
class ApplicationConfiguration {

    @NotNull
    String id;

    @NotNull
    String description;

    /**
     * Hardcoded URL.  Often used when there is an external load balancer to utilize multiple services.
     */
    @Nullable
    String url;

    /**
     * If this is populated, it will look for that particular ServiceInstance (by name) and copy it's url to this url.
     */
    @Nullable
    String serviceInstanceUrl;

    @Nullable
    Object loadBalance;     // TODO: Build out

    @Nullable
    Class<? extends ServiceSelectionAlgorithm> serviceSelectionAlgorithm;

    @NotNull
    List<ServiceInstanceConfiguration> serviceInstances;

}
