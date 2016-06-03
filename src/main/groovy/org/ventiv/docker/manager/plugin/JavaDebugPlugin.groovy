package org.ventiv.docker.manager.plugin

import org.ventiv.docker.manager.model.PortDefinition
import org.ventiv.docker.manager.model.ServiceInstance

/**
 * A plugin that will modify a Service Instance's Environment before creating the container.  It will look for certain
 * environment variables that already exist, and move them over to the correct format within JAVA_OPTS.
 *
 * Specifically, if JAVA_DEBUG_PORT is set already in the environment, it will append (or create) the proper Java Debug
 * Line. (e.g. -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005).
 *
 * Environment Variables:
 * JAVA_DEBUG_PORT [required] - Port to use to expose on the server.  Also enables this plugin
 * JAVA_DEBUG_CONTAINER_PORT [optional - default 5005] - Port to use for 'address' in agentlib line
 * JAVA_DEBUG_SUSPEND [optional - default false] - Boolean if the startup of the container should be suspended until someone connects
 */
class JavaDebugPlugin implements CreateContainerPlugin {

    public static final JAVA_DEBUG_PORT             = "JAVA_DEBUG_PORT";
    public static final JAVA_DEBUG_CONTAINER_PORT   = "JAVA_DEBUG_CONTAINER_PORT";
    public static final JAVA_DEBUG_SUSPEND          = "JAVA_DEBUG_SUSPEND";
    public static final JAVA_OPTS                   = "JAVA_OPTS";

    @Override
    void doWithServiceInstance(ServiceInstance serviceInstance) {
        Map<String, String> env = serviceInstance.getResolvedEnvironmentVariables();

        if (env.containsKey(JAVA_DEBUG_PORT)) {
            int exposedPort = Integer.parseInt(env[JAVA_DEBUG_PORT]);
            int containerPort = env.containsKey(JAVA_DEBUG_CONTAINER_PORT) ? Integer.parseInt(env[JAVA_DEBUG_CONTAINER_PORT]) : 5005;
            String suspend = env.containsKey(JAVA_DEBUG_SUSPEND) ? Boolean.parseBoolean(env[JAVA_DEBUG_SUSPEND]) ? "y" : "n" : "n";
            String agentLib = " -agentlib:jdwp=transport=dt_socket,server=y,suspend=$suspend,address=$containerPort"

            // First, add the actual line to the JAVA_OPTS
            env.put(JAVA_OPTS, env[JAVA_OPTS] ? env[JAVA_OPTS] + agentLib : agentLib);

            // Now, we need to expose the actual port
            PortDefinition portDefinition = new PortDefinition();
            portDefinition.setHostPort(exposedPort);
            portDefinition.setContainerPort(containerPort);
            portDefinition.setPortType("JavaDebugPort");
            serviceInstance.getPortDefinitions().add(portDefinition);

            // Finally, remove the fields that don't need to be in the Environment
            env.remove(JAVA_DEBUG_PORT)
            env.remove(JAVA_DEBUG_CONTAINER_PORT)
            env.remove(JAVA_DEBUG_SUSPEND)
        }

    }

}
