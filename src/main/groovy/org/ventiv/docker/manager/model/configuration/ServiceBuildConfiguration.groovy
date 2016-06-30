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
package org.ventiv.docker.manager.model.configuration

import groovy.util.logging.Slf4j
import org.jdeferred.FailCallback
import org.jdeferred.ProgressCallback
import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.build.BuildContext
import org.ventiv.docker.manager.model.ApplicationDetails
import org.ventiv.docker.manager.model.DockerTag
import org.ventiv.docker.manager.service.DockerRegistryApiService

/**
 * Created by jcrygier on 3/4/15.
 */
@Slf4j
class ServiceBuildConfiguration {

    public static final String BUILD_NEW_VERSION = "BuildNewVersion"

    List<ServiceBuildStageConfiguration> stages;
    VersionSelectionConfiguration versionSelection;

    public Promise<BuildContext, Exception, String> execute(ApplicationDetails applicationDetails, ServiceConfiguration serviceConfiguration, String branch, String requestedBuildVersion) {
        Authentication buildRequestor = SecurityContextHolder.getContext().getAuthentication();
        DeferredObject<BuildContext, Exception, String> deferred = new DeferredObject<>();

        // The tag of the desired docker image after the build is done
        DockerTag tag = new DockerTag(serviceConfiguration.getImage());
        String tagStr = serviceConfiguration.buildPossible && branch ? branch + "-" + requestedBuildVersion : requestedBuildVersion
        tag.setTag(tagStr);

        BuildContext buildContext = new BuildContext([
                userAuthentication: SecurityContextHolder.getContext().getAuthentication(),
                requestedBranch: branch,
                requestedBuildVersion: requestedBuildVersion,
                outputDockerImage: tag,
                applicationDetails: applicationDetails
        ])

        // First, we need to determine if this build exists in the docker registry, if it does, we're not going to bother building
        String registryImageId = null;
        if (requestedBuildVersion != BUILD_NEW_VERSION) {
            try {
                registryImageId = DockerManagerApplication.getApplicationContext().getBean(DockerRegistryApiService).getTagsForImage(tag).find { it == tagStr };
            } catch (Exception ignored) {}     // This can happen if the registry has never seen this image type before
        }

        if (!registryImageId) {
            Thread.start {
                SecurityContextHolder.getContext().setAuthentication(buildRequestor);
                getStages().eachWithIndex { ServiceBuildStageConfiguration buildStage, Integer idx ->
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
            log.info("Build not needed for ${serviceConfiguration.getName()}.  Image found in registry: ${registryImageId}");
            buildContext.setBuildingVersion(requestedBuildVersion);
            deferred.resolve(buildContext);
        }

        return deferred.promise();
    }

}
