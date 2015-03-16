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
import org.ventiv.docker.manager.controller.EnvironmentController
import org.ventiv.docker.manager.event.BuildStatusEvent

/**
 * Created by jcrygier on 3/13/15.
 */
class BuildApplicationInfo {

    ApplicationDetails applicationDetails;
    ApplicationConfiguration applicationConfiguration;
    List<ServiceBuildInfo> serviceBuildInfoList;

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

    public void serviceBuildSuccessful(ServiceBuildInfo serviceBuildInfo, BuildContext buildContext) {
        // Update the template version for when we're done and need to build a deployRequest - TODO: Massage the buildingVersion (e.g. 234 -> b234)
        applicationDetails.getBuildServiceVersionsTemplate().put(serviceBuildInfo.getServiceName(), buildContext.getBuildingVersion());

        if (!isBuilding()) {
            // We're all done with our overall build, time to deploy this bad boy!
            DeployApplicationRequest deployRequest = new DeployApplicationRequest(name: applicationConfiguration.getId(), serviceVersions: applicationDetails.getBuildServiceVersionsTemplate());
            DockerManagerApplication.getApplicationContext().getBean(EnvironmentController).deployApplication(applicationDetails.getTierName(), applicationDetails.getEnvironmentName(), deployRequest);
        }
    }

    public void publishBuildEvent() {
        DockerManagerApplication.getApplicationContext().publishEvent(new BuildStatusEvent(getBuildStatus()));
    }

}
