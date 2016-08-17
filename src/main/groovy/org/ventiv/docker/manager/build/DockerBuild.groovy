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
import com.github.dockerjava.api.command.PushImageCmd
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.api.model.BuildResponseItem
import com.github.dockerjava.api.model.PushResponseItem
import com.github.dockerjava.core.command.BuildImageResultCallback
import com.github.dockerjava.core.command.PushImageResultCallback
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
import org.ventiv.docker.manager.model.configuration.EnvironmentConfiguration
import org.ventiv.docker.manager.service.DockerService
import org.ventiv.docker.manager.service.EnvironmentConfigurationService
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

    // Should we skip pushing (i.e. just for testing locally)
    public static final String CONFIG_SKIP_PUSH =                      'skipPush'

    // Should we pull the base image before building?  Default is to Always pull, set this to false to override
    public static final String CONFIG_PULL =                           'pull'

    private static final Map<String, String> lockMap = [:]

    @Resource DockerService dockerService;
    @Resource DockerManagerConfiguration props;
    @Resource SimpleTemplateService templateService;
    @Resource EnvironmentConfigurationService environmentConfigurationService;

    @Override
    Promise<Object, Exception, String> doBuild(Map<String, String> buildSettings, BuildContext buildContext) {
        Deferred<Object, Exception, String> deferred = new DeferredObject<>()

        String buildHostName = buildSettings[CONFIG_BUILD_HOSTNAME] ?: getBuildHost(buildContext) ?: props.getConfig().getBuildHost();
        File buildDirectory = new File(buildSettings[CONFIG_BUILD_DIRECTORY]);
        DockerClient docker = dockerService.getDockerClient(buildHostName);
        boolean skipPush = buildSettings[CONFIG_SKIP_PUSH] ? Boolean.parseBoolean(buildSettings[CONFIG_SKIP_PUSH]) : false;
        boolean pull = buildSettings[CONFIG_PULL] ? Boolean.parseBoolean(buildSettings[CONFIG_PULL]) : true;

        Thread.start {
            synchronized (aquireLock(buildDirectory.getAbsolutePath())) {
                log.info("#### Start to build dir: ${buildDirectory.getAbsolutePath()}")

                // Treat the Dockerfile as a template, and replace variables
                File dockerFileTemplate = new File(buildDirectory, "Dockerfile.template");
                File dockerFile = new File(buildDirectory, "Dockerfile")

                if(dockerFileTemplate.exists()) {
                    FileUtils.copyFile(dockerFileTemplate, dockerFile);
                    templateService.fillTemplate(dockerFile, [buildContext: buildContext, buildSettings: buildSettings]);
                }

                try {
                    deferred.notify("Building docker image from directory: ${buildDirectory.getAbsolutePath()}".toString())
                    log.debug("DockerBuild building from ${buildDirectory.getAbsolutePath()}");

                    // Build the Image
                    BuildImageResultCallback callback = new BuildImageResultCallback() {
                        @Override
                        void onNext(BuildResponseItem item) {
                            super.onNext(item);

                            // Alert the Promise of status
                            deferred.notify(item.getStream());
                        }
                    };
                    docker.buildImageCmd(buildDirectory).withTag(buildContext.getOutputDockerImage().toString()).withPull(pull).exec(callback);
                    String imageId = callback.awaitImageId();

                    if (!skipPush) {
                        deferred.notify("Pushing Docker Image ${buildContext.getOutputDockerImage().toString()}".toString())

                        // Push the Image
                        PushImageCmd pushCmd = docker.pushImageCmd(buildContext.getOutputDockerImage().getName()).withTag(buildContext.getOutputDockerImage().getTag())

                        // Set the auth, if needed
                        if (props.config.registry && props.config.registry.server == buildContext.getOutputDockerImage().getRegistry()) {
                            AuthConfig authConfig = pushCmd.getAuthConfig() ?: new AuthConfig();
                            authConfig.withRegistryAddress(props.config.registry.server)
                            authConfig.withUsername(props.config.registry.username)
                            authConfig.withPassword(props.config.registry.password)
                            authConfig.withEmail(props.config.registry.email)
                            pushCmd.withAuthConfig(authConfig);
                        }

                        PushImageResultCallback pushCallback = new PushImageResultCallback() {
                            @Override
                            void onNext(PushResponseItem item) {
                                super.onNext(item)

                                deferred.notify(item.getStatus());
                            }
                        };

                        pushCmd.exec(pushCallback);
                        pushCallback.awaitCompletion();
                    }

                    buildContext.setBuildingVersion(buildContext.getRequestedBuildVersion());
                    deferred.resolve(buildContext);
                } catch (Exception e) {
                    log.error("Error Building Image in dir: ${buildDirectory.getAbsolutePath()}.", e.getMessage());
                    deferred.reject(e);
                } finally {
                    if(dockerFileTemplate.exists()) dockerFile.delete();
                    log.info("#### Finished build dir: ${buildDirectory.getAbsolutePath()}")
                }
            }
        }

        return deferred.promise();
    }

    private String getBuildHost(BuildContext buildContext) {
        EnvironmentConfiguration environmentConfiguration = environmentConfigurationService.getEnvironment(buildContext.getApplicationDetails().getTierName(), buildContext.getApplicationDetails().getEnvironmentName());
        return environmentConfiguration?.getServers()?.find { it.getBuildEnabled() }?.getHostname()
    }

    private synchronized static String aquireLock(String buildDirectoryPath) {
        String lock = lockMap[buildDirectoryPath]
        if(!lock) {
            lockMap[buildDirectoryPath] = buildDirectoryPath
            lock = lockMap[buildDirectoryPath]
        }
        return lock
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
