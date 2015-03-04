package org.ventiv.docker.manager.model

/**
 * Created by jcrygier on 3/3/15.
 */
class EnvironmentConfiguration {

    String id;
    String description;
    List<ServerConfiguration> servers;
    List<ApplicationConfiguration> applications;
}
