package org.ventiv.docker.manager.model

/**
 * Information needed to build out an Application
 */
class BuildApplicationRequest {

    /**
     * Application Name to build out
     */
    String name;

    /**
     * A map of ServiceName -> Versions that are being requested.
     * For example: [
     *      rabbit: 3.4.3-management
     *      logstash: 1.4.2
     * ]
     *
     * Will request that all 'rabbit' service instances in this application be on v3.4.3-management and all logstash
     * service instances be on 1.4.2.
     */
    Map<String, String> serviceVersions;

}
