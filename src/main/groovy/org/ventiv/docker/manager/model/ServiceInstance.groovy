package org.ventiv.docker.manager.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.dockerjava.api.model.Container
import groovy.transform.ToString
import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.config.DockerServiceConfiguration

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
    String serviceDescription;
    String serverName;

    /**
     * Derived URL for this service instance, if the ServiceConfiguration has a 'url' defined.
     */
    String url;
    Integer instanceNumber
    Status status;
    String containerStatus;
    String containerId;
    DockerTag containerImage;
    Date containerCreatedDate;
    Collection<String> availableVersions;

    List<PortDefinition> portDefinitions;

    Map<String, String> resolvedEnvironmentVariables;

    public ServiceInstance setDockerName(String dockerName) {
        def matcher = dockerName =~ DOCKER_NAME_PATTERN;
        if (matcher) {
            tierName = matcher[0][1];
            environmentName = matcher[0][2];
            applicationId = matcher[0][3];
            name = matcher[0][4];
            instanceNumber = Integer.parseInt(matcher[0][5]);
        } else {
            name = dockerName.substring(1);
        }

        return this;
    }

    public ServiceInstance withDockerContainer(Container dockerContainer) {
        this.setDockerName(dockerContainer.getNames()[0])
        this.status = dockerContainer.getStatus().startsWith("Up") ? ServiceInstance.Status.Running : ServiceInstance.Status.Stopped;
        this.containerStatus = dockerContainer.getStatus();
        this.containerId = dockerContainer.getId();
        this.containerImage = new DockerTag(dockerContainer.getImage());
        this.containerCreatedDate = new Date(dockerContainer.getCreated() * 1000);

        // Get the service configuration
        def serviceConfig = DockerManagerApplication.getApplicationContext().getBean(DockerServiceConfiguration).getServiceConfiguration(name)
        this.serviceDescription = serviceConfig?.description ?: containerImage.getRepository();

        // Determine the Port Definitions
        this.portDefinitions = dockerContainer.getPorts().collect { Container.Port port ->
            return new PortDefinition([
                    portType: serviceConfig?.containerPorts?.find { it.port == port.getPrivatePort() }?.type,
                    hostPort: port.getPublicPort(),
                    containerPort: port.getPrivatePort()
            ])
        }

        // Determine the URL
        if (serviceConfig?.getUrl()) {
            if (this.status == Status.Running) {
                Map<String, Integer> ports = getPortDefinitions()?.collectEntries { PortDefinition portDefinition ->
                    [portDefinition.getPortType(), portDefinition.getHostPort()]
                }

                Binding b = new Binding([server: getServerName(), port: ports]);
                GroovyShell sh = new GroovyShell(b);
                setUrl(sh.evaluate('"' + serviceConfig.getUrl() + '"').toString());
            } else if (getStatus() == ServiceInstance.Status.Stopped)
                setUrl(getServiceDescription() + " is Stopped")
            else
                setUrl(getServiceDescription() + " is Missing")
        }

        return this;
    }

    @Override
    public String toString() {
        return "${tierName}.${environmentName}.${applicationId}.${name}.${instanceNumber}"
    }


    public static final enum Status {
        Available, Running, Stopped
    }

}
