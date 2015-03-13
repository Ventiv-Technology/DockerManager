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

import feign.Feign
import feign.jackson.JacksonEncoder
import groovy.util.logging.Slf4j
import org.jdeferred.Deferred
import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.ventiv.docker.manager.api.HeaderRequestInterceptor
import org.ventiv.docker.manager.build.jenkins.BuildQueueStatus
import org.ventiv.docker.manager.build.jenkins.BuildStartedResponse
import org.ventiv.docker.manager.build.jenkins.BuildStatus
import org.ventiv.docker.manager.build.jenkins.JenkinsApi
import org.ventiv.docker.manager.build.jenkins.JenkinsApiDecoder

/**
 * Created by jcrygier on 3/11/15.
 */
@Slf4j
@Component("JenkinsBuild")
class JenkinsBuild implements AsyncBuildStage {

    // Used only for testing
    JenkinsApi mockJenkinsApi;

    @Override
    Promise doBuild(Map<String, String> buildSettings, BuildContext buildContext) {
        Deferred deferred = new DeferredObject();

        JenkinsApi api = getJenkinsApi(buildSettings, buildContext)
        BuildStartedResponse resp = api.startNewBuild(buildSettings['jobName']);

        buildContext.getExtraParameters()['buildStartedResponse'] = resp;

        Thread.start {
            pollForCompletion(buildSettings, buildContext, deferred, api);
        }

        return deferred.promise();
    }

    void pollForCompletion(Map<String, String> buildSettings, BuildContext buildContext, Deferred deferred, JenkinsApi api) {
        BuildQueueStatus queueStatus = api.getBuildQueueStatus(buildContext.extraParameters.buildStartedResponse.queueId);
        long sleepTime = 1000;

        deferred.notify("Jenkins job queued")

        // Poll until we have enough information in the queue status
        while (queueStatus.getExecutable()?.getNumber() == null) {
            sleep(sleepTime);
            queueStatus = api.getBuildQueueStatus(buildContext.extraParameters.buildStartedResponse.queueId);
        }

        deferred.notify("Jenkins job ${queueStatus.getTask().getName()} #${queueStatus.getExecutable().getNumber()} building...");

        BuildStatus buildStatus = api.getBuildStatus(queueStatus.getTask().getName(), queueStatus.getExecutable().getNumber());
        while (buildStatus.isBuilding()) {
            sleep(sleepTime);

            // Intelligently calculate sleep by (estimatedDuration - duration) / 2, or 1s, whichever is longer
            sleepTime = Math.max(1000L, (buildStatus.getEstimatedDuration() - buildStatus.getDuration()) / 2 as Long);

            deferred.notify("Jenkins job ${queueStatus.getTask().getName()} #${queueStatus.getExecutable().getNumber()} building...Expected to be done in ${(buildStatus.getEstimatedDuration() - buildStatus.getDuration()) / 1000} seconds")

            buildStatus = api.getBuildStatus(queueStatus.getTask().getName(), queueStatus.getExecutable().getNumber());
        }

        deferred.notify("Jenkins job ${queueStatus.getTask().getName()} #${queueStatus.getExecutable().getNumber()} finished with result: ${buildStatus.getResult()}");

        if (buildStatus.getResult() == BuildStatus.BuildResult.FAILURE)
            deferred.reject("Build Failure");
        else {
            buildContext.setBuildingVersion(queueStatus.getExecutable().getNumber().toString())
            deferred.resolve("Build Success");
        }
    }

    private JenkinsApi getJenkinsApi(Map<String, String> buildSettings, BuildContext buildContext) {
        if (mockJenkinsApi)
            return mockJenkinsApi;

        AuthenticationType authType = buildSettings['authentication'] ? AuthenticationType.valueOf(buildSettings['authentication']) : AuthenticationType.None;

        Feign.Builder feignBuilder = Feign.builder()
                .decoder(new JenkinsApiDecoder())
                .encoder(new JacksonEncoder())

        if (authType == AuthenticationType.CurrentUser) {
            Authentication auth = (Authentication) buildContext.userAuthentication;

            String userName = auth.getPrincipal() instanceof String ? auth.getPrincipal() : auth.getPrincipal().username;
            String authHeader = "${userName}:${auth.getCredentials()}".bytes.encodeBase64().toString()

            feignBuilder.requestInterceptor(new HeaderRequestInterceptor([Authorization: "Basic " + authHeader]))
        } else if (authType == AuthenticationType.ProvidedUserPassword) {
            String authHeader = "${buildSettings['user']}:${buildSettings['password']}".bytes.encodeBase64().toString()
            feignBuilder.requestInterceptor(new HeaderRequestInterceptor([Authorization: "Basic " + authHeader]))
        }

        return feignBuilder.target(JenkinsApi, buildSettings['server'])
    }

    public static final enum AuthenticationType {
        None, CurrentUser, ProvidedUserPassword
    }
}
