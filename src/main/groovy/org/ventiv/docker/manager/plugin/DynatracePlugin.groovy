package org.ventiv.docker.manager.plugin

import org.ventiv.docker.manager.model.ServiceInstance

/**
 * A plugin that will modify a Service Instance's Environment before creating the container.  It will look for certain
 * environment variables that already exist, and move them over to the correct format within JAVA_OPTS.
 */
class DynatracePlugin implements CreateContainerPlugin {

    public static final DYNATRACE_PATH          = "DYNATRACE_PATH";
    public static final DYNATRACE_SERVER        = "DYNATRACE_SERVER";
    public static final DYNATRACE_NAME          = "DYNATRACE_NAME";
    public static final JAVA_OPTS               = "JAVA_OPTS";

    @Override
    void doWithServiceInstance(ServiceInstance serviceInstance) {
        Map<String, String> env = serviceInstance.getResolvedEnvironmentVariables();

        if (env.containsKey(DYNATRACE_PATH) && env.containsKey(DYNATRACE_SERVER) && env.containsKey(DYNATRACE_NAME)) {
            String dynaTraceAgent = " -agentpath:${env[DYNATRACE_PATH]}=name=${env[DYNATRACE_NAME]},server=${env[DYNATRACE_SERVER]}"
            env.put(JAVA_OPTS, env[JAVA_OPTS] ? env[JAVA_OPTS] + dynaTraceAgent : dynaTraceAgent);

            // Now, we don't need the DYNATRACE_*** variables, lets clean em up
            env.remove(DYNATRACE_PATH)
            env.remove(DYNATRACE_SERVER)
            env.remove(DYNATRACE_NAME)
        }
    }

}
