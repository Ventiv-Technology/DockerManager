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

import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.build.BuildContext
import org.ventiv.docker.manager.event.BuildStatusEvent
import org.ventiv.docker.manager.model.configuration.ApplicationConfiguration

/**
 * Created by jcrygier on 3/13/15.
 */
class BuildApplicationInfo {

    ApplicationDetails applicationDetails;
    ApplicationConfiguration applicationConfiguration;
    List<ServiceBuildInfo> serviceBuildInfoList;
    Closure<ApplicationDetails> successfulBuildCallback;

    public BuildApplicationInfo(ApplicationDetails applicationDetails) {
        this.applicationDetails = applicationDetails;
        this.applicationConfiguration = applicationDetails.getApplicationConfiguration()
        this.serviceBuildInfoList = applicationConfiguration.getServiceInstances()*.getType().collect { String serviceName ->
            return new ServiceBuildInfo(this, serviceName);
        }
    }

    public ServiceBuildInfo getServiceBuildInfo(String serviceName) {
        return serviceBuildInfoList.find { it.getServiceName() == serviceName }
    }

    public List<String> getServiceNames() {
        return serviceBuildInfoList.collect { it.getServiceName() }
    }

    public boolean isBuilding() {
        return serviceBuildInfoList.find { it.isBuilding() } != null
    }

    public BuildStatus getBuildStatus() {
        new BuildStatus([
                tierName: applicationDetails.getTierName(),
                environmentName: applicationDetails.getEnvironmentName(),
                applicationId: applicationDetails.getId(),
                building: isBuilding(),
                serviceBuildStatus: serviceBuildInfoList.collectEntries {
                    [it.getServiceName(), it.getLastStatus()]
                }
        ])
    }

    public void onSuccessfulBuild(Closure<ApplicationDetails> callback) {
        this.successfulBuildCallback = callback;
        if (!isBuilding())
            successfulBuildCallback(applicationDetails);
    }

    public void serviceBuildSuccessful(ServiceBuildInfo serviceBuildInfo, BuildContext buildContext) {
        // Update the template version for when we're done and need to build a deployRequest
        applicationDetails.getBuildServiceVersionsTemplate().put(serviceBuildInfo.getServiceName(), buildContext.getBuildingVersion());

        if (!isBuilding() && successfulBuildCallback) {
            successfulBuildCallback(applicationDetails);
        }
    }

    public void publishBuildEvent() {
        DockerManagerApplication.getApplicationContext().publishEvent(new BuildStatusEvent(getBuildStatus()));
    }

}
