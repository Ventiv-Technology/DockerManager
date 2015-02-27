package org.ventiv.docker.manager.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.ToString

/**
 * An instantiated instance of a service, that has been assigned to a host / list of ports.  In order to keep track
 * and not have to persist this in a database, we will use the Docker Name in a special format.  This format is as follows:
 *
 * <tierName>.<environmentName>.<applicationId>.<service>.<instanceNumber>
 */
@ToString
class ServiceInstance {

    public static final def DOCKER_NAME_PATTERN = /([a-zA-Z0-9][a-zA-Z0-9_-]*).([a-zA-Z0-9][a-zA-Z0-9_-]*).([a-zA-Z0-9][a-zA-Z0-9_-]*).([a-zA-Z0-9][a-zA-Z0-9_-]*).([0-9])/

    @JsonIgnore
    String tierName;

    @JsonIgnore
    String environmentName;

    String applicationId;
    String name;
    String serverName;
    Integer instanceNumber
    Status status;
    String containerStatus;
    String containerId;
    DockerTag containerImage;
    Date containerCreatedDate;

    List<PortDefinition> portDefinitions;

    public ServiceInstance setDockerName(String dockerName) {
        def matcher = dockerName =~ DOCKER_NAME_PATTERN;
        if (matcher) {
            tierName = matcher[0][1];
            environmentName = matcher[0][2];
            applicationId = matcher[0][3];
            name = matcher[0][4];
            instanceNumber = Integer.parseInt(matcher[0][5]);
        }

        return this;
    }

    public static final enum Status {
        Available, Running, Stopped
    }

}
