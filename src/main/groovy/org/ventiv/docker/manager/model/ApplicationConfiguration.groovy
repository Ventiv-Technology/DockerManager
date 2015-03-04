package org.ventiv.docker.manager.model

/**
 * Created by jcrygier on 3/4/15.
 */
class ApplicationConfiguration {

    String id;
    String description;
    String url;
    Object loadBalance;     // TODO: Build out
    List<ServiceInstanceConfiguration> serviceInstances;

}
