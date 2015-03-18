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
package org.ventiv.docker.manager.build

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.BuildImageCmd
import com.github.dockerjava.api.command.PushImageCmd
import groovy.util.logging.Slf4j
import org.jdeferred.Deferred
import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject
import org.springframework.stereotype.Component
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.service.DockerService

import javax.annotation.Resource

/**
 * Created by jcrygier on 3/18/15.
 */
@Slf4j
@Component("DockerBuild")
class DockerBuild implements AsyncBuildStage {

    // Directory that contains the Dockerfile.  This may have been created in an earlier stage
    public static final String CONFIG_BUILD_DIRECTORY =                'buildDirectory'

    // Hostname that has a docker daemon running to perform this build.  If this setting is not present, it will grab from properties (config.buildHost)
    public static final String CONFIG_BUILD_HOSTNAME =                 'buildHostName'

    @Resource DockerService dockerService;
    @Resource DockerManagerConfiguration props;

    @Override
    Promise<Object, Exception, String> doBuild(Map<String, String> buildSettings, BuildContext buildContext) {
        Deferred<Object, Exception, String> deferred = new DeferredObject<>()

        String buildHostName = buildSettings[CONFIG_BUILD_HOSTNAME] ?: props.getConfig().getBuildHost();
        File buildDirectory = new File(buildSettings[CONFIG_BUILD_DIRECTORY]);
        DockerClient docker = dockerService.getDockerClient(buildHostName);

        // TODO: Treat Dockerfile as a groovy template

        Thread.start {
            try {
                log.debug("DockerBuild building from ${buildDirectory.getAbsolutePath()}");

                // Build the Image
                BuildImageCmd.Response buildResponse = docker.buildImageCmd(buildDirectory).withTag(buildContext.getOutputDockerImage().toString()).exec();
                if (log.isDebugEnabled())
                    buildResponse.getItems().each { log.debug("Build Response: ${it.getStream().trim()}") }

                // Push the Image
                PushImageCmd.Response pushResponse = docker.pushImageCmd(buildContext.getOutputDockerImage().getName()).withTag(buildContext.getOutputDockerImage().getTag()).exec()
                if (log.isDebugEnabled())
                    pushResponse.getItems().each { log.debug("Push Response: ${it.getStatus().trim()}") }

                buildContext.setBuildingVersion(buildContext.getRequestedBuildVersion());
                deferred.resolve(buildContext);
            } catch (Exception e) {
                deferred.reject(e);
            }
        }

        return deferred.promise();
    }

}
