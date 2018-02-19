/**
 * Copyright (c) 2014 - 2018 Ventiv Technology
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
package org.ventiv.docker.manager.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.ventiv.docker.manager.model.ActionDetails;
import org.ventiv.docker.manager.model.ExecResponse;
import org.ventiv.docker.manager.model.ServiceInstance;
import org.ventiv.docker.manager.security.DockerManagerPermission;
import org.ventiv.docker.manager.service.DockerService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JavaActionsPlugin implements ActionPlugin {

    private static final Logger log = LoggerFactory.getLogger(JavaActionsPlugin.class);
    private final DockerService dockerService;

    public JavaActionsPlugin(ApplicationContext applicationContext, Environment env) {
        dockerService = (DockerService) applicationContext.getBean("dockerService");
    }

    @Override
    public List<ActionDetails> getSupportedActions() {
        return Arrays.asList(
                new ActionDetails("ThreadDump", "Java Thread Dump", "/app/partials/stdOutStdErrDisplay.html", DockerManagerPermission.ACTION, this::isJavaHomePresent, this::performJavaThreadDump),
                new ActionDetails("JVMClassHistogram", "Java Class Histogram", "/app/partials/stdOutStdErrDisplay.html", DockerManagerPermission.ACTION, this::isJavaHomePresent, this::performClassHistogram)
        );
    }

    private boolean isJavaHomePresent(ServiceInstance serviceInstance) {
        return serviceInstance.getResolvedEnvironmentVariables().containsKey("JAVA_HOME");
    }

    private ExecResponse performClassHistogram(ServiceInstance serviceInstance) {
        String pid = getJavaPid(serviceInstance);
        return dockerService.exec(serviceInstance, "jcmd", pid, "GC.class_histogram", "-all");
    }

    private Object performJavaThreadDump(ServiceInstance serviceInstance) {
        String pid = getJavaPid(serviceInstance);

        // Might need to retry this one, if the server is busy
        int retryCount = 0;
        while (retryCount < 3) {
            ExecResponse response = dockerService.exec(serviceInstance, "jcmd", pid, "Thread.print", "-l");
            if (response.getExitCode() == 0)
                return response;

            retryCount++;
        }

        // Finally, try w/o extra lock info
        return dockerService.exec(serviceInstance, "jcmd", pid, "Thread.print");
    }

    private String getJavaPid(ServiceInstance serviceInstance) {
        // First, we need to find the PID of the response
        ExecResponse pidResponse = dockerService.exec(serviceInstance, "jcmd");
        if (pidResponse.getExitCode() != 0)
            throw new RuntimeException("Failed to find Java PID");

        Optional<String> pid = Arrays.stream(pidResponse.getStdout().split("\n"))
                .filter(it -> !it.contains("JCmd"))
                .map(it -> it.split(" "))
                .filter(it -> it.length > 0)
                .map(it -> it[0])
                .findFirst();

        if (!pid.isPresent())
            throw new RuntimeException("Failed to find Java PID");

        return pid.get();
    }

}
