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
import org.springframework.stereotype.Component
import org.ventiv.docker.manager.build.jenkins.BuildQueueStatus
import org.ventiv.docker.manager.build.jenkins.BuildStartedResponse
import org.ventiv.docker.manager.build.jenkins.BuildStatus
import org.ventiv.docker.manager.build.jenkins.JenkinsApi
import org.ventiv.docker.manager.build.jenkins.JenkinsApiDecoder
import org.ventiv.docker.manager.exception.JenkinsBuildFailedException
import org.ventiv.docker.manager.utils.AuthenticationRequestInterceptor

/**
 * Kicks off a Jenkins Build via the Jenkins REST Api.
 */
@Slf4j
@Component("JenkinsBuild")
class JenkinsBuild implements AsyncBuildStage {

    // Name of the Jenkins Job to Build
    public static final String CONFIG_JOB_NAME =                'jobName'

    // Server to contact Jenkins
    public static final String CONFIG_SERVER =                  'server'

    // Groovy string to evaluate the build number.  Groovy Binding is BuildQueueStatus -> queueStatus, BuildStatus -> status.
    // Defaults to "${status.number}"
    public static final String CONFIG_BUILD_NUMBER =            'buildNumber'

    // See other settings in AuthenticationRequestInterceptor

    // Used only for testing
    JenkinsApi mockJenkinsApi;

    @Override
    Promise<Object, Exception, String> doBuild(Map<String, String> buildSettings, BuildContext buildContext) {
        Deferred deferred = new DeferredObject();

        JenkinsApi api = getJenkinsApi(buildSettings, buildContext)
        BuildStartedResponse resp = api.startNewBuild(buildSettings[CONFIG_JOB_NAME]);

        buildContext.getExtraParameters()['buildStartedResponse'] = resp;

        Thread.start {
            try {
                pollForCompletion(buildSettings, buildContext, deferred, api);
            } catch (Exception e) {
                deferred.reject(e);
            }
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
            deferred.reject(new JenkinsBuildFailedException());
        else {
            buildContext.setBuildingVersion(massageBuildNumber(buildSettings, queueStatus, buildStatus))
            deferred.resolve("Build Success");
        }
    }

    private String massageBuildNumber(Map<String, String> buildSettings, BuildQueueStatus queueStatus, BuildStatus status) {
        if (buildSettings.containsKey(CONFIG_BUILD_NUMBER)) {
            Binding b = new Binding();
            b.setVariable("queueStatus", queueStatus);
            b.setVariable("status", status);
            GroovyShell sh = new GroovyShell(b);
            return sh.evaluate('return "' + buildSettings[CONFIG_BUILD_NUMBER] + '"')?.toString();
        } else
            return status.number.toString()
    }

    private JenkinsApi getJenkinsApi(Map<String, String> buildSettings, BuildContext buildContext) {
        if (mockJenkinsApi)
            return mockJenkinsApi;

        Feign.Builder feignBuilder = Feign.builder()
                .decoder(new JenkinsApiDecoder())
                .encoder(new JacksonEncoder())
                .requestInterceptor(new AuthenticationRequestInterceptor(buildSettings, buildContext.getUserAuthentication()));

        return feignBuilder.target(JenkinsApi, buildSettings[CONFIG_SERVER])
    }
}
