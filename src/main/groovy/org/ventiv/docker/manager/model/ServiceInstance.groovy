package org.ventiv.docker.manager.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.ToString

/**
 * An instantiated instance of a service, that has been assigned to a host / list of ports.  In order to keep track
 * and not have to persist this in a database, we will use the Docker Name in a special format.  This format is as follows:
 *
 * <tierName>.<environmentName>.<name>.<instanceNumber>
 */
@ToString
class ServiceInstance {

    public static final def DOCKER_NAME_PATTERN = /([a-zA-Z0-9][a-zA-Z0-9_-]*).([a-zA-Z0-9][a-zA-Z0-9_-]*).([a-zA-Z0-9][a-zA-Z0-9_-]*).([0-9])/

    @JsonIgnore
    String tierName;

    @JsonIgnore
    String environmentName;

    String name;
    String serverName;
    Integer instanceNumber
    String containerId;
    String containerImage;
    Date containerCreatedDate;

    List<PortDefinition> portDefinitions;

}
