/*
 * Copyright (c) 2014 - 2015 Ventiv Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.ventiv.docker.manager.plugin

import org.ventiv.docker.manager.model.ServiceInstance

/**
 * A plugin that will modify a Service Instance's Environment before creating the container.  It will look for certain
 * environment variables that already exist, and move them over to the correct format within JAVA_OPTS.
 */
class DynatracePlugin implements CreateContainerPlugin {

    public static final DYNATRACE_PATH                  = "DYNATRACE_PATH";
    public static final DYNATRACE_SERVER                = "DYNATRACE_SERVER";
    public static final DYNATRACE_NAME                  = "DYNATRACE_NAME";
    public static final DYNATRACE_OVERRIDE_HOST_NAME    = "DYNATRACE_OVERRIDE_HOST_NAME";
    public static final JAVA_OPTS                       = "JAVA_OPTS";

    public static final CONTAINER_NAME                  = "CONTAINER_NAME";

    @Override
    void doWithServiceInstance(ServiceInstance serviceInstance) {
        Map<String, String> env = serviceInstance.getResolvedEnvironmentVariables();

        if (env.containsKey(DYNATRACE_PATH) && env.containsKey(DYNATRACE_SERVER) && env.containsKey(DYNATRACE_NAME)) {
            // Derive the host name - First use DYNATRACE_OVERRIDE_HOST_NAME Env variable, then the server name of teh service instance.  If either of those are 'CONTAINER_NAME' - change to 12 characters of the ID
            String overrideHostName = ",overridehostname=" + (env[DYNATRACE_OVERRIDE_HOST_NAME] ?: serviceInstance.serverName);
            if (env[DYNATRACE_OVERRIDE_HOST_NAME] == CONTAINER_NAME)
                overrideHostName = "";

            String dynaTraceAgent = " -agentpath:${env[DYNATRACE_PATH]}=name=${env[DYNATRACE_NAME]},server=${env[DYNATRACE_SERVER]}${overrideHostName}"
            env.put(JAVA_OPTS, env[JAVA_OPTS] ? env[JAVA_OPTS] + dynaTraceAgent : dynaTraceAgent);

            // Now, we don't need the DYNATRACE_*** variables, lets clean em up
            env.remove(DYNATRACE_PATH)
            env.remove(DYNATRACE_SERVER)
            env.remove(DYNATRACE_NAME)
            env.remove(DYNATRACE_OVERRIDE_HOST_NAME)
        }
    }

}
