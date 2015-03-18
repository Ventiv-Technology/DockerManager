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
package org.ventiv.docker.manager.model

import feign.FeignException
import groovy.util.logging.Slf4j
import org.jdeferred.FailCallback
import org.jdeferred.ProgressCallback
import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject
import org.springframework.security.core.context.SecurityContextHolder
import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.build.BuildContext
import org.ventiv.docker.manager.service.DockerRegistryApiService

/**
 * Created by jcrygier on 3/4/15.
 */
@Slf4j
class ServiceBuildConfiguration {

    public static final String BUILD_NEW_VERSION = "BuildNewVersion"

    List<ServiceBuildStage> stages;
    VersionSelectionConfiguration versionSelection;

    public Promise<BuildContext, Exception, String> execute(ServiceConfiguration serviceConfiguration, String requestedBuildVersion) {
        DeferredObject<BuildContext, Exception, String> deferred = new DeferredObject<>();

        // The tag of the desired docker image after the build is done
        DockerTag tag = new DockerTag(serviceConfiguration.getImage());
        tag.setTag(requestedBuildVersion);

        BuildContext buildContext = new BuildContext([
                userAuthentication: SecurityContextHolder.getContext().getAuthentication(),
                requestedBuildVersion: requestedBuildVersion,
                outputDockerImage: tag
        ])

        // First, we need to determine if this build exists in the docker registry, if it does, we're not going to bother building
        String registryImageId = null;
        if (requestedBuildVersion != BUILD_NEW_VERSION) {
            try {
                registryImageId = DockerManagerApplication.getApplicationContext().getBean(DockerRegistryApiService).getRegistry(tag).listRepositoryTags(tag.getNamespace(), tag.getRepository())[requestedBuildVersion];
            } catch (FeignException ignored) {}     // This can happen if the registry has never seen this image type before
        }

        if (!registryImageId) {
            Thread.start {
                getStages().eachWithIndex { ServiceBuildStage buildStage, Integer idx ->
                    if (deferred.isPending()) {
                        deferred.notify("Currently building ${idx + 1} of ${getStages().size()}")
                        try {
                            Promise buildPromise = buildStage.execute(buildContext);

                            buildPromise.progress({ String progress ->
                                deferred.notify("Progress for ${idx + 1} of ${getStages().size()}: ${progress}");
                            } as ProgressCallback<String>)

                            buildPromise.fail({ Exception e ->
                                deferred.reject(e);
                            } as FailCallback<Exception>)

                            // Wait till this stage is done, then move on
                            buildPromise.waitSafely();

                            deferred.notify("Finished building ${idx + 1} of ${getStages().size()}")
                        } catch (Exception e) {
                            if (deferred.isPending())
                                deferred.reject(e);
                        }
                    }
                }

                if (deferred.isPending())
                    deferred.resolve(buildContext);
            }
        } else {
            // We found an image id in the registry, let's use that one.
            log.debug("Build not needed for ${serviceConfiguration.getName()}.  Image found in registry: ${registryImageId}");
            buildContext.setBuildingVersion(requestedBuildVersion);
            deferred.resolve(buildContext);
        }

        return deferred.promise();
    }

}
