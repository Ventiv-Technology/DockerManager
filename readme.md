# Docker Manager

This project was started to take over the Ventiv Technology Environment Manager application.  However, this one will be
more generic, and will support ANY Dockerized project.  It is heavily inspired by Fig, but is geared towards a multi-server
environment.  There are also several other configuration options (such as labelling ports / volumes) that enable certain things
like URL derivation.

## Running

In order to run this project, you'll need some configuration, see the Configuring section below.  Generally, you'll want to
simplify this process by storing the configuration in a VCS repository.  That being said, the general process for running is to do the
following:

1. Clone this repository
2. cd DockerManager
3. Prepare your configuration, generally in the 'config' directory.  There is a sample config located in sample-config.
4. When running DockerManagerApplication.groovy, be sure to add your authentication from a spring profile.  This will auto-load
   any properties from config/application-<profileName>.yml.  E.g. `--spring.profiles.active=ldap` will load config/application-ldap.yml
   Please Note, Default security is to accept ANY user / password.

## Docker Configuration Model

Docker Manager is configured to adhere to a 4 step hierarchy to organize your Environments and Applications.  From top to bottom, they are:

1. Tier - Intended to be a top level organization, often by network segment.  Examples are typically Development, Quality, UAT, Production
   this is a nice feature, since one deployment of DockerManager may not be able to communicate (network wise) to different tiers.
   This will allow you to deploy several DockerManager instances for each network segment.
2. Environment - Intended to be a grouping of Applications that are related to a given environment.  This allows you to have one Tier manage
   several different disassociated applications.  This is typically helpful if you combine tiers by logical area.  For example
   Development A, Development B, and Quality B may all be environments under the development tier.
3. Application - A grouping of services that is intended to run together to form a single application.  This will generally have
   one single URL endpoint, and there are features supporting as such.  An application definition simply defines what services,
   and how many of those services should be running.  DockerManger figures out the rest.
4. Service / Service Instance - The lowest level of configuration, representing a single running service.  This is equivalent to
   a docker container running on some server.

For illustration / testing purposes, a sample configuration has been included.  This sample only contains one server (boot2docker)
so it is not what a typical production instance would look like, but it's good enough for documentation purposes.  The structure
is as follows:

- Tier: localhost
    - Environment: development (Development Testing)
        - Application: activiti (Activiti)
            - Service: activiti (Activiti)
            - Service: mysql (MySQL Database)
            
For the best in example and documentation, these Yaml files have been carefully constructed to illustrate certain features.  These
files also have many of the lines commented so you can tell what they are for.

## Configuring

Configuration is split up into two different types of files, the Service Definition file (services.yml) and the Environment
Definition file (environment-name.yml).  The first (Service Definition) contains all the information about a Docker Image
that is intended to eventually become a Service instance (at deploy time).  The second (Environment Definition) contains all
the information about different servers and the applications.  All three pieces of information come together to actually
deploy / run an application.

There is a 'sample-config' directory to illustrate a starter configuration and some of the features of Docker Manager.  This
configuration only deals with one host (boot2docker) so it can be a runnable sample.  If you want to run with this, rename
'sample-config' to 'config', and be sure that you have 'boot2docker' in your hosts file (typically: 192.168.59.103	boot2docker).

### Service Definition File

As stated above, this file describes Docker Images so Docker Manager knows how to work with a given image.  It is also where
you can state additional information, like how to build a Docker Image, should one not exist yet.  Each service in this file
is mapped to the Groovy Bean 'org.ventiv.docker.manager.model.ServiceConfiguration'.  Please look there for further documentation,
or in the sample-config/env-config/services.yml file for examples. Here is a sample of a simple configuration:

    services:
      - name: rabbit
        description: RabbitMQ
        image: rabbitmq:3.4.4-management
        url: http://${server}:${port.http}
        containerPorts:
          - type: http
            port: 15672
          - type: amqp
            port: 5672
        environment:
          RABBITMQ_NODENAME: rabbit1

      - name: docker_manager
        description: Docker Manager
        image: ventivtech/docker_manager
        url: http://${server}:${port.http}/
        containerPorts:
          - type: http
            port: 8080

This example describes TWO services, one named 'rabbit', that is pinned to a particular Docker Image and Version (rabbitmq:3.4.4-management).
It also describes the ports that will be exposed later, and pins a type to them.  This type is important, since it's how
Docker Manager will do the port mapping later.  That being said, you should never have two ports with the same type here.  The
nice thing here is you can use these types later to fill in variables in URL's and Environment Variables...thus easily tying
services together, and avoiding linking containers together across servers.

The other service described here is for this Application, Docker Manager.  This is here to illustrate that the image is NOT
pinned to a particular version, and will then query the DockerHub registry for all available versions.

#### Version Selection

There are several different ways the UI can present the list of versions allowed for a given application.  There is some logic
to bubble up the version selection logic from the Service to the Application, but that will be described later.  For any given
Service, the following are valid ways to determine which versions are allowed:

- Full Docker Tag (with version).  Example: rabbitmq:3.4.4-management
    - This will only allow that given version to be deployed.  The UI will NOT use this service to pick a version to deploy.
- Docker Tag with no version provided.  Example: ventivtech/docker_manager
    - This will query the registry associated with the image to determine which versions are available.  The UI will present this to the user.
- Supply a `build` configuration
    - If there is a `versionSelection` clause under build, it will run that module to get the list of versions. This is
      useful when the docker image does NOT exist in the registry yet, but some sort of build exists somewhere.  The example
      used by Ventiv is that we use Jenkins to build and keep ALL WAR artifacts in a repository.  We then use the versionSelection
      to give a list of the WAR's that are in there, and use Docker Manager to build the image.
    - If there is no `versionSelection` clause, the registry will be queried for available images, and the UI will present
      a new option to the user 'New Build'.  This will give the option of deploying a build that has already been created,
      or kick off a new one.  Helpful if you are not using CI, but still have a build server.

#### Build

Docker Manager will allow you to build a new image, should it not exist already.  A good example of this has been included in
the sample-config location.  Please pay attention to the Activiti service, and how it builds.  First, it utilizes the `versionSelection`
clause as stated above.  This has been configured to query the GitHub API and pull out the tags.  Next, it is configured
with a build stage of 'DockerBuild'.  This simply kicks off a build in the org.ventiv.docker.manager.build.DockerBuild class,
and detailed information can be seen there.  Very simply, it treats the specified Dockerfile as a template so you can
inject configuration from DockerManager.  For example, if you open up sample-config/dockerfiles/activiti/Dockerfile you can
see that it uses `#{buildContext.requestedBuildVersion}` to set into an Environment Variable.  This environment variable
is then used in the curl statement to download the ZIP release that has been specified from the dropdown in the UI.

You can have as many build stages as needed, but typically a single Dockerfile can do it all.  Each build stage is designed to
allow for variables to be placed in the `buildContext` for use in all subsequent stages.  Also, there are certain variables that
are always allowed:

- userAuthentication - Authentication Object from Spring
- buildingVersion - Resolved building version, after it's been determined. Example, Jenkins build number, after the build has started
- requestedBuildVersion - User specified version requested to be built, from the UI.
- outputDockerImage - Name of the image that has been output from the job
- extraParameters - This is a Map of variables specific to a Build Stage

For more details on specific build stages, see the Markdown files in docs/build.

#### Container Ports

This is the section where you will describe what ports a particular container may expose.  Often, these are described by
EXPOSE statements in the Dockerfile, but this is not descriptive enough for Docker Manager.  We also must attach a type to
the port here, so that we can pair it up later, as well as use it in variables.  These variables are very helpful
if you need to set container Environment Variables that are resolved with information from another container's
ports.  It is also important to note that the ports listed here DO NOT have to be described in the Dockerfile,
as you can still expose any ports.

Based on the container port type, if there is no URL specified for this particular service, it will be auto derived to be:
${port.getType()}://${server}:${port.http}

#### Container Volumes

This is the section where you describe what volumes a particular container may expose.  Often, these are described by
VOLUME statements in the Docker file, but this allows us to give it a type so we may tie that in at container creation time.
This is very similar to how ports are done, but there is no automatic behavior based on special types.  Also, like above,
you can expose any volume in a container, it does not have to be described in the Dockerfile.

#### Environment Variables

Here, you can describe any environment variables that should be set when the container is created.  Since this is the
service definition file, you should only put environment variables here that will be used for services in ALL containers.
You will see later that these environment variables may be overridden at an Application level.  Examples in the example config
include setting the root password of Couch / MySql, as here we want the same one no matter where the instance is created.

These environment variables have certain runtime variables exposed to them when the container is created, like ports / servers
from other services in the application they get created from.  The variables that are exposed are as follows:

- application: Instance of the org.ventiv.docker.manager.model.ApplicationDetails object.  Example: ${application.tierName}
- instance: Instance of the org.ventiv.docker.manager.model.ServiceInstance object.  Example: ${instance.serviceDescription}
- serviceInstances: All other service instance objects for this application.  A map, by service name.  NOTE: If there is more
  than one service of a given type, the last one will survive in this map.
    - server: Server name for that this instance is running under.  Example: ${serviceInstances.mysql.server}
    - port: Ports mapped by type.  Example: ${serviceInstances.mysql.port.mysql} or ${serviceInstances.couch.port.http}
    
#### Additional Metrics

Sometimes the built in statistics for a given service are not enough.  This is where additional metrics comes into play.
In the example configuration, the Rabbit service has been configured with additional metrics.  The metrics simply hit
the REST endpoint /api/overview of the server, and pass that information back to the UI.  The UI will then ask for the
HTML template as it's configured in the ui section.

The UI is divided up into two sections: Button and Details.  The button will display inline in the Environment view,
next to the button you would click on for service instance details.  This button can be configured to show any data you want,
such as "Number of Messages Queued" in Rabbit.  If you've sepecified a 'Template', then the html may be inline in the YAML configuration.  
This is generally helpful for the button, since the HTML is minimal.  However, for the details, the HTML is typically much larger
so you would want to specify it in a 'Partial'.  

The HTML here is integrated fully into AngularJS and the controllers of the full application.  This allows you to access several variables
from within your html.

- Button
    - environment: Environment Details Object
    - application: Application Details Object
    - serviceInstance: Service Instance Object
    - metricName: Text Name of the metric, specified in the configuration
    - metricValue: Value of the metric retrieved by it's component.  Example for Rabbit would be the response from /api/overview
- Details
    - serviceInstance: Service Instance Object
    - data: Value of the metric retrieved by it's component.  Example for Rabbit would be the response from /api/overview

### Environment Description File

In a directory structure described above (under tiers) is a file for each individual environment.  This is where we describe
the environment's servers and applications.  Furthermore, you will see an application's service requirements, and further
configuration of them (like environment variables).

#### Server Configuration

The first section of an environment configuration file is describing the servers (or hosts) that may be part of it.  These
must include fully qualified host names, and these hosts MUST be running the Docker daemon, otherwise they will be
discarded on application startup.  For full documentation, see comments in org.ventiv.docker.manager.model.ServerConfiguration.
The most important section in here is the eligibleServices portion, which describes what services are allowed to run on this
host.

Each eligible service will have it's ports described.  It's important when you're describing the ports to use either
the 'port' or 'ports' section, but not both.  The rule of thumb is that if you want to expose multiple services, use the ports...and
if you want to expose one service, use port.  For example, if I want to allow 'Docker Manager' to run just once on this server,
I would do something like this:

    - type: docker_manager
      portMappings:
      - type: http
        port: 8080
        
However, if I want to allow 'Docker Manager' to run 5 times on this server (on ports 8080, 8081, 8082, 8083, 9000), I might do the following:

    - type: docker_manager
      portMappings:
      - type: http
        port: 8080-8083,9000
        
NOTE: The port listed here are the ports that will be exposed on the physical host, and tied back to a port in the container.
This mapping is done by using the port type.

#### Application Configuration

The last remaining part of configuration is for the application, where you describe the application and all of it's running
services.  For full documentation, please see: org.ventiv.docker.manager.model.ApplicationConfiguration.  At the top level there
are things like description and name, which are solely used for UI and API purposes.  Also there is a section here to describe
the URL of this application.  You have the opportunity to specify it manually (via the 'url' attribute), or to use one
of the service's url (via the 'serviceInstanceUrl' attribute).

Most importantly, you configure the serviceInstance's that will be part of this application, and how many of them you need.
For instance, you may say the following if you wanted 5 of a given service to be running:

    - type: awesome-server
      count: 5
      
Docker manager will then attempt to find 5 'slots' for the 'awesome-server' instance that can run on hosts configured in
the section above.  By default, it will attempt to spread these 5 across physical hosts first, then back down to running
on whatever is provided an available.  If you want to change this algorithm, do so via the serviceSelectionAlgorithm
attribute.  The following algorithms are available:

    - org.ventiv.docker.manager.service.selection.DistributedServerServiceSelectionAlgorithm: Algorithm to spread across physical
      hosts as much as possible
    - org.ventiv.docker.manager.service.selection.NextAvailableServiceSelectionAlgorithm: Simple algorithm to pick the next
      instance that has not yet been allocated.
      
Also in this section is the volume mapping.  Similar to the port mapping, you will specify the type here, and this is how
the system ties back to the services configuration file.  Again, as with the port mapping, the volumes listed here are the
ones that will exist on the docker host, and be tied back to the container via the type.  This is a very handy way to 
ensure the data from a given container sticks around between deployments.
      
Finally, underneath the service instance configuration section, you have the opportunity to specify environment variables.
This is the exact same as documented above, but will allow you to specify environment variables that are specific to
a given application.  This is the perfect place to tie servers together (e.g. Activiti -> MySql) or to specify what environment
this application is running in (e.g. Development).