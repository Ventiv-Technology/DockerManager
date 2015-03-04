package org.ventiv.docker.manager.model

/**
 * Created by jcrygier on 3/4/15.
 */
class ServiceConfiguration {

    String name;
    String description;
    String image;
    ServiceBuildConfiguration build;
    Collection<PortMapptingConfiguration> containerPorts;
    Map<String, String> environment;

}
