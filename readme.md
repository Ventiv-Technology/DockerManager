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
4. Service Instance - The lowest level of configuration, representing a single running service.  This is equivalent to
   a docker container running on some server.

// TODO: Show the hierarchy that is set up in sample-config

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