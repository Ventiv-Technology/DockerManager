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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.BuildImageCmd
import com.github.dockerjava.api.command.PushImageCmd
import com.github.dockerjava.api.model.EventStreamItem
import com.github.dockerjava.api.model.PushEventStreamItem
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.LineIterator
import org.jdeferred.Deferred
import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject
import org.springframework.stereotype.Component
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.service.DockerService
import org.ventiv.docker.manager.service.SimpleTemplateService

import javax.annotation.Resource

/**
 * Created by jcrygier on 3/18/15.
 */
@CompileStatic
@Slf4j
@Component("DockerBuild")
class DockerBuild implements AsyncBuildStage {

    // Directory that contains the Dockerfile.  This may have been created in an earlier stage
    public static final String CONFIG_BUILD_DIRECTORY =                'buildDirectory'

    // Hostname that has a docker daemon running to perform this build.  If this setting is not present, it will grab from properties (config.buildHost)
    public static final String CONFIG_BUILD_HOSTNAME =                 'buildHostName'

    @Resource DockerService dockerService;
    @Resource DockerManagerConfiguration props;
    @Resource SimpleTemplateService templateService;

    @Override
    Promise<Object, Exception, String> doBuild(Map<String, String> buildSettings, BuildContext buildContext) {
        Deferred<Object, Exception, String> deferred = new DeferredObject<>()

        String buildHostName = buildSettings[CONFIG_BUILD_HOSTNAME] ?: props.getConfig().getBuildHost();
        File buildDirectory = new File(buildSettings[CONFIG_BUILD_DIRECTORY]);
        DockerClient docker = dockerService.getDockerClient(buildHostName);

        // Treat the Dockerfile as a template, and replace variables
        File dockerFile = new File(buildDirectory, "Dockerfile");
        templateService.fillTemplate(dockerFile, [buildContext: buildContext, buildSettings: buildSettings], ".orig");

        Thread.start {
            try {
                deferred.notify("Building docker image from directory: ${buildDirectory.getAbsolutePath()}".toString())
                log.debug("DockerBuild building from ${buildDirectory.getAbsolutePath()}");

                // Build the Image
                BuildImageCmd.Response buildResponse = docker.buildImageCmd(buildDirectory).withTag(buildContext.getOutputDockerImage().toString()).exec();
                deserializeStream(buildResponse, EventStreamItem) { EventStreamItem event ->
                    deferred.notify(event.getStream().trim());
                }

                deferred.notify("Pushing Docker Image ${buildContext.getOutputDockerImage().toString()}".toString())

                // Push the Image
                PushImageCmd.Response pushResponse = docker.pushImageCmd(buildContext.getOutputDockerImage().getName()).withTag(buildContext.getOutputDockerImage().getTag()).exec()
                deserializeStream(pushResponse, PushEventStreamItem) { PushEventStreamItem event ->
                    deferred.notify((event.getProgress() ? event.getProgress() + ": " : "") + event.getStatus())
                }

                buildContext.setBuildingVersion(buildContext.getRequestedBuildVersion());
                deferred.resolve(buildContext);
            } catch (Exception e) {
                deferred.reject(e);
            } finally {
                File backupOriginalFile = new File(dockerFile.getAbsolutePath() + ".orig")
                dockerFile.delete();
                FileUtils.copyFile(backupOriginalFile, dockerFile);
                backupOriginalFile.delete();
            }
        }

        return deferred.promise();
    }

    public <T> void deserializeStream(InputStream inputStream, Class<T> serializedType, Closure<?> callback) {
        try {
            LineIterator itr = IOUtils.lineIterator(inputStream, "UTF-8");
            while (itr.hasNext()) {
                String line = itr.next();

                ObjectMapper mapper = new ObjectMapper();
                // we'll be reading instances of MyBean
                ObjectReader reader = mapper.reader(serializedType);
                // and then do other configuration, if any, and read:
                T item = reader.readValue(line);

                callback(item);
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
}
