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

import com.github.dockerjava.api.command.CreateContainerCmd;
import org.apache.commons.lang.SerializationUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.ventiv.docker.manager.config.DockerServiceConfiguration;
import org.ventiv.docker.manager.controller.EnvironmentController;
import org.ventiv.docker.manager.controller.LoginController;
import org.ventiv.docker.manager.model.ActionDetails;
import org.ventiv.docker.manager.model.ApplicationDetails;
import org.ventiv.docker.manager.model.CreateContainerDryRunContext;
import org.ventiv.docker.manager.model.ExecResponse;
import org.ventiv.docker.manager.model.ServiceInstance;
import org.ventiv.docker.manager.model.configuration.PropertiesConfiguration;
import org.ventiv.docker.manager.model.configuration.ServiceConfiguration;
import org.ventiv.docker.manager.model.configuration.ServiceInstanceConfiguration;
import org.ventiv.docker.manager.security.DockerManagerPermission;

import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptCreationPlugin implements ActionPlugin {

    private final EnvironmentController environmentController;
    private final LoginController loginController;
    private final DockerServiceConfiguration dockerServiceConfiguration;

    public ScriptCreationPlugin(ApplicationContext applicationContext, Environment env) {
        loginController = applicationContext.getBean(LoginController.class);
        environmentController = applicationContext.getBean(EnvironmentController.class);
        dockerServiceConfiguration = applicationContext.getBean(DockerServiceConfiguration.class);
    }

    @Override
    public List<ActionDetails> getSupportedActions() {
        return Arrays.asList(
                new ActionDetails("CreateDockerScript", "Create Docker Script", "/app/partials/stdOutStdErrDisplay.html", DockerManagerPermission.ACTION, si -> true, this::createDockerScript)
        );
    }

    private ExecResponse createDockerScript(ServiceInstance serviceInstance) {
        ServiceInstance cloned = (ServiceInstance) SerializationUtils.clone(serviceInstance);

        ExecResponse response = new ExecResponse();
        response.setExitCode(0);

        StringBuilder script = new StringBuilder("#!/bin/bash\n\n");

        // Pull
        script.append("docker pull ")
                .append(cloned.getContainerImage())
                .append("\n");

        // Create Container
        CreateContainerDryRunContext dryRunContext = new CreateContainerDryRunContext();
        ApplicationDetails applicationDetails = environmentController.getApplicationDetails(cloned.getTierName(), cloned.getEnvironmentName(), cloned.getApplicationId());
        environmentController.createDockerContainer(applicationDetails, cloned, null, cloned.getContainerImage().getTag(), dryRunContext);
        script.append(convertToBashCmd(dryRunContext.getCreateContainerCmd()))
                .append("\n");

        // Inject Properties
        ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(cloned.getName());
        String token = loginController.generateToken(1, ChronoUnit.DAYS, SecurityContextHolder.getContext().getAuthentication());

        ServiceInstanceConfiguration serviceInstanceConfiguration = applicationDetails.getApplicationConfiguration().getServiceInstances().stream()
                .filter(it -> it.getType().equals(serviceInstance.getName()))
                .findFirst().get();

        if (applicationDetails.getApplicationConfiguration().shouldPropertiesFileBeCreated() || serviceInstanceConfiguration.shouldPropertiesFileBeCreated()) {
            serviceConfiguration.getProperties().forEach(propertyConfig -> {
                if (propertyConfig.getMethod() == PropertiesConfiguration.PropertiesMethod.File) {
                    script.append("curl -o dockerManagerProperties.txt -H 'Accept: text/plain' -H 'Authorization: Bearer ")
                            .append(token)
                            .append("' \"")
                            .append(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getRequestURL().toString().replaceAll("/api/.*", ""))
                            .append("/api/properties/")
                            .append(cloned.getTierName())
                            .append("/")
                            .append(cloned.getEnvironmentName())
                            .append("/")
                            .append(cloned.getApplicationId())
                            .append("/")
                            .append(cloned.getName())
                            .append("?instanceNumber=").append(cloned.getInstanceNumber())
                            .append("&serverName=").append(cloned.getServerName())
                            .append("&propertySet=").append(propertyConfig.getSetId());

                    if (propertyConfig.getOverrideServiceName() != null)
                        script.append("&overrideServiceName=").append(propertyConfig.getOverrideServiceName());

                    script.append("\"\n");
                    script.append("docker cp dockerManagerProperties.txt ")
                            .append(cloned.toString())
                            .append(":")
                            .append(propertyConfig.getLocation())
                            .append("\n");
                } else if (propertyConfig.getMethod() == PropertiesConfiguration.PropertiesMethod.Template) {
                    script.append("curl -o dockerManagerProperties.txt -H 'Accept: text/plain' -H 'Authorization: Bearer ")
                            .append(token)
                            .append("' \"")
                            .append(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getRequestURL().toString().replaceAll("/api/.*", ""))
                            .append("/api/properties/")
                            .append(cloned.getTierName())
                            .append("/")
                            .append(cloned.getEnvironmentName())
                            .append("/")
                            .append(cloned.getApplicationId())
                            .append("/")
                            .append(cloned.getName())
                            .append("/template?templateLocation=")
                            .append(URLEncoder.encode(propertyConfig.getTemplateLocation()))
                            .append("&instanceNumber=").append(cloned.getInstanceNumber())
                            .append("&serverName=").append(cloned.getServerName())
                            .append("&propertySet=").append(propertyConfig.getSetId());

                    if (propertyConfig.getOverrideServiceName() != null)
                        script.append("&overrideServiceName=").append(propertyConfig.getOverrideServiceName());

                    script.append("\"\n");
                    script.append("docker cp dockerManagerProperties.txt ")
                            .append(cloned.toString())
                            .append(":")
                            .append(propertyConfig.getLocation())
                            .append("\n");
                }
            });
        }

        // Start
        script.append("docker start ")
                .append(cloned.toString());

        response.setStdout(script.toString());
        return response;
    }

    private String convertToBashCmd(CreateContainerCmd cmd) {
        StringBuilder sb = new StringBuilder("docker create ");

        // Environment Variables
        if (cmd.getEnv() != null && cmd.getEnv().length > 0) {
            sb.append("-e \"")
                    .append(Arrays.stream(cmd.getEnv()).collect(Collectors.joining("\" -e \"")))
                    .append("\" ");
        }

        // Exposed Ports
        cmd.getHostConfig().getPortBindings().getBindings().forEach((exposedPort, bindings) -> {
            Arrays.stream(bindings).forEach(binding -> {
                sb.append("-p ")
                        .append(binding.getHostPortSpec())
                        .append(":")
                        .append(exposedPort.getPort())
                        .append(" ");
            });
        });

        // Exposed Volumes
        if (cmd.getBinds() != null) {
            Arrays.stream(cmd.getBinds())
                    .filter(binding -> binding.getVolume() != null)
                    .forEach(binding -> {
                        sb.append("-v ")
                                .append(binding.getPath())
                                .append(":")
                                .append(binding.getVolume().getPath())
                                .append(" ");
            });
        }

        // Memory Limits
        if (cmd.getMemory() != null && cmd.getMemory() > 0) {
            sb.append("-m ")
                    .append(cmd.getMemory())
                    .append("b ");
        }

        if (cmd.getMemorySwap() != null && cmd.getMemorySwap() > 0) {
            sb.append("--memory-swap ")
                    .append(cmd.getMemorySwap())
                    .append("b ");
        }

        // Name
        sb.append("--name ")
                .append(cmd.getName())
                .append(" ");

        sb.append(cmd.getImage());

        return sb.toString();
    }


}
