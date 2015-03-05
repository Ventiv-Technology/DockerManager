package org.ventiv.docker.manager.controller

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Ports
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.config.PropertyTypes
import org.ventiv.docker.manager.model.ApplicationConfiguration
import org.ventiv.docker.manager.model.ApplicationDetails
import org.ventiv.docker.manager.model.BuildApplicationRequest
import org.ventiv.docker.manager.model.DockerTag
import org.ventiv.docker.manager.model.EligibleServiceConfiguration
import org.ventiv.docker.manager.model.EnvironmentConfiguration
import org.ventiv.docker.manager.model.MissingService
import org.ventiv.docker.manager.model.PortDefinition
import org.ventiv.docker.manager.model.ServerConfiguration
import org.ventiv.docker.manager.model.ServiceConfiguration
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.service.DockerService
import org.ventiv.docker.manager.service.selection.ServiceSelectionAlgorithm
import org.yaml.snakeyaml.Yaml

import javax.annotation.Resource

/**
 * Created by jcrygier on 2/27/15.
 */
@Slf4j
@RequestMapping("/api/environment")
@RestController
class EnvironmentController {

    @Resource DockerService dockerService;
    @Resource DockerServiceConfiguration dockerServiceConfiguration;
    @Resource DockerServiceController dockerServiceController;

    @RequestMapping
    public Map<String, List<EnvironmentConfiguration>> getTiers() {
        List<String> activeTiers = PropertyTypes.Active_Tiers.getStringListValue()
        getAllEnvironments().findAll { k, v -> activeTiers.contains(k) };
    }

    @RequestMapping("/{tierName}")
    public Collection<String> getEnvironments(@PathVariable("tierName") String tierName) {
        getAllEnvironments()[tierName];
    }

    @CompileStatic
    @RequestMapping("/{tierName}/{environmentName}")
    public List<ApplicationDetails> getEnvironmentDetails(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName) {
        EnvironmentConfiguration envConfiguration = getTiers()[tierName].find { it.getId() == environmentName }
        List<ServiceInstance> serviceInstances = getServiceInstances(tierName, environmentName);

        return envConfiguration.getApplications().collect { ApplicationConfiguration applicationConfiguration ->
            Collection<ServiceInstance> applicationInstances = serviceInstances.findAll { it.getApplicationId() == applicationConfiguration.getId() };

            // Now make ServiceInstance objects for each one defined
            List<String> requiredServices = (List<String>) applicationConfiguration.getServiceInstances().collect { [it.getType()] * it.getCount() }.flatten()
            List<String> missingServices = new ArrayList<String>(requiredServices)
            applicationInstances.each { missingServices.remove(it.getName())  }  // Remove any that actually exist

            // Derive the URL
            String url = applicationConfiguration.getUrl();
            if (applicationConfiguration.getServiceInstanceUrl()) {
                url = applicationInstances.find { it.getName() == applicationConfiguration.getServiceInstanceUrl() }?.getUrl()
            }

            return populateVersions(new ApplicationDetails([
                    tierName: tierName,
                    environmentName: environmentName,
                    url: url,
                    serviceInstances: applicationInstances,
                    missingServiceInstances: missingServices.collect { new MissingService([serviceName: it, serviceDescription: dockerServiceConfiguration.getServiceConfiguration(it).getDescription()]) }
            ]).withApplicationConfiguration(applicationConfiguration))
        }
    }

    /**
     * Builds out an application.  Ensures the docker environment of two things:
     * 1.) Any missingServiceInstances (see getEnvironmentDetails) will be built out according to the requested versions
     * 2.) Each serviceInstance (see getEnvironmentDetails) is on the proper version.  If not, it will destroy the container and rebuild.
     */
    @RequestMapping(value = "/{tierName}/{environmentName}", method = RequestMethod.POST)
    public def buildApplication(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName, @RequestBody BuildApplicationRequest buildRequest) {
        List<ApplicationDetails> environmentDetails = getEnvironmentDetails(tierName, environmentName);
        ApplicationDetails applicationDetails = environmentDetails.find { it.getId() == buildRequest.getName() }
        List<ServiceInstance> allServiceInstances = getServiceInstances(tierName, environmentName)

        // First, let's find any missing services
        applicationDetails.getMissingServiceInstances().each { MissingService missingService ->
            // Find an 'Available' Service Instance
            ServiceInstance toUse = ServiceSelectionAlgorithm.Util.getAvailableServiceInstance(missingService.getServiceName(), allServiceInstances, applicationDetails);
            toUse.setApplicationId(buildRequest.getName());

            // Create (and start) the container
            createDockerContainer(applicationDetails, toUse, buildRequest.getServiceVersions().get(missingService.getServiceName()));

            // Mark this service instance as 'Running' so it won't get used again
            toUse.setStatus(ServiceInstance.Status.Running);
        }

        // Verify all running serviceInstances to ensure they're the correct version
        new ArrayList(applicationDetails.getServiceInstances()).each { ServiceInstance anInstance ->
            if (anInstance.getStatus() != ServiceInstance.Status.Available && anInstance.getContainerImage() != null) {
                String expectedVersion = buildRequest.getServiceVersions()[anInstance.getName()];
                String runningVersion = anInstance.getContainerImage().getTag();

                // We have a version mismatch...destroy the container and rebuild it
                if (expectedVersion != runningVersion) {
                    // First, let's destroy the container
                    destroyDockerContainer(applicationDetails, anInstance);

                    // Now, create a new one
                    createDockerContainer(applicationDetails, anInstance, buildRequest.getServiceVersions().get(anInstance.getName()));
                }
            }
        }
    }

    @CompileStatic
    @RequestMapping("/{tierName}/{environmentName}/serviceInstances")
    public List<ServiceInstance> getServiceInstances(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName) {
        EnvironmentConfiguration envConfiguration = getTiers()[tierName].find { it.getId() == environmentName }

        List<ServiceInstance> definedServiceInstances = [];
        envConfiguration.getServers().each { ServerConfiguration serverConf ->
            List<Container> containers = dockerService.getDockerClient(serverConf.getHostname()).listContainersCmd().withShowAll(true).exec();

            Map<String, Integer> instanceNumbers = [:]
            serverConf.getEligibleServices().each { EligibleServiceConfiguration serviceConf ->
                String serviceName = serviceConf.getType();
                ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(serviceName);
                instanceNumbers.put(serviceName, (instanceNumbers[serviceName] ?: 0) + 1);      // Increment/populate the service instance number
                Integer instanceNumber = instanceNumbers[serviceName];
                Container dockerContainer = containers.find { it.getNames()[0].startsWith("/${tierName}.${environmentName}.") && it.getNames()[0].endsWith(".${serviceName}.${instanceNumber}") }

                // Create a shell of an instance, just in case there is no docker container to get the info from.
                ServiceInstance serviceInstance = new ServiceInstance([
                        tierName: tierName,
                        environmentName: environmentName,
                        name: serviceName,
                        serverName: serverConf.getHostname(),
                        instanceNumber: instanceNumber,
                        status: ServiceInstance.Status.Available,
                        portDefinitions: serviceConf.portMappings.collect { portMapping ->
                            return new PortDefinition([
                                    portType: portMapping.type,
                                    hostPort: portMapping.port,
                                    containerPort: serviceConfiguration.containerPorts.find { it.type == portMapping.type }.port
                            ])
                        }
                ])

                // Now, if the docker container exists, overwrite all the info with the real info
                if (dockerContainer) {
                    serviceInstance.withDockerContainer(dockerContainer);
                }

                definedServiceInstances << serviceInstance;
            }
        }

        return definedServiceInstances;
    }

    public Map<String, List<EnvironmentConfiguration>> getAllEnvironments() {
        // Search for all YAML files under /data/env-config/tiers
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        def allEnvironments = resolver.getResources(PropertyTypes.Environment_Configuration_Location.getValue() + "/tiers/**/*.yml")

        // Group by Directory, then Massage the ClassPathResource elements into the filename minus .yml
        return allEnvironments.groupBy { new File(it.path).getParentFile().getName() }.collectEntries { String tierName, List<org.springframework.core.io.Resource> resources ->
            [tierName, resources.collect { org.springframework.core.io.Resource yamlConfig ->
                String environmentId = yamlConfig.getFilename().replaceAll("\\.yml", "")
                EnvironmentConfiguration environmentConfiguration = new Yaml().loadAs(yamlConfig.getInputStream(), EnvironmentConfiguration)
                environmentConfiguration?.setId(environmentId);

                return environmentConfiguration;
            }]
        }
    }

    /**
     * Aggregate the versions of a given ApplicationDetail's ServiceInstance's into one version, if possible.
     *
     * @param applicationDetails
     * @return
     */
    private ApplicationDetails populateVersions(ApplicationDetails applicationDetails) {
        // Get all the services that are part of this application, and find the available versions for those services
        Collection<String> allServices = applicationDetails.getApplicationConfiguration().getServiceInstances()*.getType();
        Map<String, List<String>> availableServiceVersions = allServices.collectEntries { String serviceName ->
            List<String> availableVersions = dockerServiceController.getAvailableVersions(serviceName)
            return [(serviceName): availableVersions]
        }

        // Populate the available versions for each Service Instance
        applicationDetails.getServiceInstances().each { ServiceInstance serviceInstance ->
            serviceInstance.setAvailableVersions(availableServiceVersions[serviceInstance.getName()]);
        }

        // Populate the available versions for each missing Service
        applicationDetails.getMissingServiceInstances().each { MissingService missingService ->
            missingService.setAvailableVersions(availableServiceVersions[missingService.getServiceName()]);
        }

        // Build out buildServiceVersionsTemplate for services with one option
        allServices.each { String serviceName ->
            if (availableServiceVersions[serviceName].size() == 1)
                applicationDetails.getBuildServiceVersionsTemplate().put(serviceName, availableServiceVersions[serviceName].first());
            else
                applicationDetails.getBuildServiceVersionsTemplate().put(serviceName, null);
        }

        // Filter out any services that only have 1 available version, since we have no control over it anyway
        availableServiceVersions = availableServiceVersions.findAll { serviceName, availableVersions ->
            return availableVersions.size() > 1;
        }

        // Determine the Available Aggregated Versions - check all unique service's version list, use it if there's just one
        List<String> availableAggregatedVersions = null;
        if (availableServiceVersions.size() == 0)
            availableAggregatedVersions = [];
        else if (availableServiceVersions.values().unique(false).size() == 1)
            availableAggregatedVersions = availableServiceVersions.values().unique(false).first()

        String deployedVersion = "Multiple"
        if (!applicationDetails.getServiceInstances())                                      // We have nothing running!
            deployedVersion = "Nothing Deployed"
        else if (applicationDetails.getServiceInstances().size() == 1)                      // There's only 1 thing running!
            deployedVersion = applicationDetails.getServiceInstances().first().getContainerImage().getTag();
        else {                                                                              // Well Crap...we have multiple things running....do the aggregation logic
            // Find only service instances that are in the filtered availableServiceVersions
            Collection<ServiceInstance> uniqueServiceInstances = applicationDetails.getServiceInstances().findAll { availableServiceVersions.containsKey(it.getName()) }
            if (uniqueServiceInstances.size() == 1)
                deployedVersion = uniqueServiceInstances.first().getContainerImage().getTag();
            else if (uniqueServiceInstances*.getContainerImage()*.getTag().flatten().unique().size() == 1)
                deployedVersion = uniqueServiceInstances*.getContainerImage()*.getTag().flatten().unique();
        }

        applicationDetails.setVersion(deployedVersion)
        applicationDetails.setAvailableVersions(availableAggregatedVersions);

        return applicationDetails
    }

    private String createDockerContainer(ApplicationDetails applicationDetails, ServiceInstance instance, String desiredVersion) {
        ServerConfiguration serverConfiguration = getTiers()[applicationDetails.getTierName()].find { it.getId() == applicationDetails.getEnvironmentName() }.getServers().find { it.getHostname() == instance.getServerName() }

        // Get the image name, so we can build out a DockerTag with the proper version
        String imageName = dockerServiceConfiguration.getServiceConfiguration(instance.getName()).image
        DockerTag toDeploy = new DockerTag(imageName)
        toDeploy.setTag(desiredVersion);

        // Build out port bindings
        Ports portBindings = new Ports();
        instance.getPortDefinitions().each {
            portBindings.bind(new ExposedPort(it.getContainerPort()), new Ports.Binding("0.0.0.0", it.getHostPort()));
        }

        HostConfig hostConfig = new HostConfig();
        hostConfig.setPortBindings(portBindings);

        if (serverConfiguration.getResolveHostname()) {
            InetAddress address = InetAddress.getByName(instance.getServerName());
            hostConfig.setExtraHosts(["${address.getHostName()}:${address.getHostAddress()}"] as String[]);
        }

        // Get the environment variables
        Map<String, String> env = [:]
        if (dockerServiceConfiguration.getServiceConfiguration(instance.getName()).getEnvironment())
            env.putAll(dockerServiceConfiguration.getServiceConfiguration(instance.getName()).getEnvironment())
        if (applicationDetails.getApplicationConfiguration().getServiceInstances().find { it.getType() == instance.getName() }.getEnvironment())
            env.putAll(applicationDetails.getApplicationConfiguration().getServiceInstances().find { it.getType() == instance.getName() }.getEnvironment())

        // Resolve them
        def resolutionVariables = [
                application: applicationDetails,
                serviceInstances: applicationDetails.getServiceInstances().collectEntries { ServiceInstance serviceInstance ->
                    return [serviceInstance.getName(), [
                            server: serviceInstance.getServerName(),
                            port: serviceInstance.getPortDefinitions().collectEntries {
                                return [it.getPortType(), it.getHostPort()]
                            }
                    ]]
                }
        ]

        Binding b = new Binding(resolutionVariables);
        GroovyShell sh = new GroovyShell(b);
        env.each { k, v ->
            if (v.contains('$'))
                env.put(k, sh.evaluate('"' + v + '"').toString())
        }

        instance.setResolvedEnvironmentVariables(env);

        // Do a docker pull, just to ensure we have the image locally
        DockerClient docker = dockerService.getDockerClient(instance.getServerName())
        log.info("Pulling docker image: '$toDeploy");
        InputStream pullIn = docker.pullImageCmd(toDeploy.toString()).exec();
        pullIn.eachLine {
            log.debug(it);
        }

        // Create the actual container
        log.info("Creating new Docker Container on Host: '${instance.getServerName()}' " +
                "with image: '${toDeploy.toString()}', " +
                "name: '${instance.toString()}', " +
                "ports: ${portBindings.getBindings().collect { ExposedPort p, Ports.Binding[] bnd -> bnd.collect { it.getHostIp() + ":" + it.getHostPort() }.join(",")  + '->' + p.getPort() } }," +
                "env: ${env.collect {k, v -> "$k=$v"}}")
        CreateContainerResponse resp = docker.createContainerCmd(toDeploy.toString())
                .withName(instance.toString())
                .withEnv(instance.getResolvedEnvironmentVariables()?.collect {k, v -> "$k=$v"} as String[])
                .withHostConfig(hostConfig).exec();

        log.info("Created container with ID: '${resp.id}'.  Starting...")
        docker.startContainerCmd(resp.id).exec();

        // Create a ServiceInstance out of this Container
        Container container = docker.listContainersCmd().withShowAll(true).exec().find { it.getId() == resp.id }
        if (container) applicationDetails.getServiceInstances() << instance.withDockerContainer(container);

        return resp.id;
    }

    private void destroyDockerContainer(ApplicationDetails applicationDetails, ServiceInstance instance) {
        log.info("Destroying docker container: '${instance.toString()}")
        dockerService.getDockerClient(instance.getServerName()).removeContainerCmd(instance.toString()).withForce().exec();

        // Remove this ServiceInstance from the Application
        applicationDetails.getServiceInstances().remove(instance);
    }


}
