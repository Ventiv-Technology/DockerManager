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

import org.jdeferred.ProgressCallback
import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject
import org.springframework.security.core.context.SecurityContextHolder
import org.ventiv.docker.manager.build.BuildContext

/**
 * Created by jcrygier on 3/4/15.
 */
class ServiceBuildConfiguration {

    String vcs;
    String url;
    String dockerFile;

    List<ServiceBuildStage> stages;
    VersionSelectionConfiguration versionSelection;

    public Promise<BuildContext, Exception, String> execute() {
        DeferredObject<BuildContext, Exception, String> deferred = new DeferredObject<>();
        BuildContext buildContext = new BuildContext([
                userAuthentication: SecurityContextHolder.getContext().getAuthentication()
        ])

        Thread.start {
            getStages().eachWithIndex { ServiceBuildStage buildStage, Integer idx ->
                if (deferred.isPending()) {
                    deferred.notify("Currently building ${idx + 1} of ${getStages().size()}")
                    try {
                        Promise buildPromise = buildStage.execute(buildContext);

                        buildPromise.progress({ String progress ->
                            deferred.notify("Progress for ${idx + 1} of ${getStages().size()}: ${progress}");
                        } as ProgressCallback<String>)

                        // Wait till this stage is done, then move on
                        buildPromise.waitSafely();

                        deferred.notify("Finished building ${idx + 1} of ${getStages().size()}")
                    } catch (Exception e) {
                        deferred.reject(e);
                    }
                }
            }

            if (deferred.isPending())
                deferred.resolve(buildContext);
        }

        return deferred.promise();
    }

}
