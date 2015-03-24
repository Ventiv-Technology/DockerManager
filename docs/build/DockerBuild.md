# Docker Build

- Name: DockerBuild
- Properties:
    - buildDirectory: Directory that contains the Dockerfile.  This may have been created in an earlier stage.  A nice pattern would be to have once stage fetch the build files from a git repo, then hand off to here.
    - buildHostName: Hostname that has a docker daemon running to perform this build.  If this setting is not present, it will grab from properties (config.buildHost)
    - skipPush: Should we skip pushing (i.e. just for testing locally)
- Outputs:
    - outputDockerImage: The image that has been created

Starts a proper docker build on the buildHostname.  This is very similar to building directly from docker (i.e. docker build -t <imageName> <buildDirectory>)
except that the Dockerfile is treated as a template.  This allows for variable replacement within the dockerfile before building.
Current variables include:

- buildContext: See org.ventiv.docker.manager.build.BuildContext
- buildSettings: Any of the settings from above

Currently, the default is to use variable replacement like so: `#{buildContext.requestedBuildVersion}`.  However, the characters
that denote the placeholders for a variable may be changed by setting the `template.startToken` and `template.endToken` configuration
settings.