package org.ventiv.docker.manager.model

/**
 * Data representing an Application's Configuration.  This differentiates from an ApplicationDetails object by being
 * a configuration as opposed to what is actually running.
 *
 * Generally configured via YAML.
 */
class ApplicationConfiguration {

    String id;
    String description;
    String url;
    Object loadBalance;     // TODO: Build out
    List<ServiceInstanceConfiguration> serviceInstances;

}
