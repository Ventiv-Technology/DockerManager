package org.ventiv.docker.manager.model

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * The instantiated, running version of an ApplicationConfiguration.  Much of the information is copied from an
 * ApplicationConfiguration object.
 */
class ApplicationDetails {

    String id;
    String description;
    String url;
    String tierName;
    String environmentName;
    String version;
    Collection<String> availableVersions;
    Collection<ServiceInstance> serviceInstances;
    List<MissingService> missingServiceInstances;
    Map<String, String> buildServiceVersionsTemplate = [:];

    @JsonIgnore
    ApplicationConfiguration applicationConfiguration;

    public ApplicationDetails withApplicationConfiguration(ApplicationConfiguration appConfig) {
        this.applicationConfiguration = appConfig;
        this.id = appConfig.getId();
        this.description = appConfig.getDescription();

        return this;
    }

}
