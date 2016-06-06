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
package org.ventiv.docker.manager.event

import org.ventiv.docker.manager.model.ApplicationDetails

/**
 * Created by jcrygier on 3/16/15.
 */
class DeploymentStartedEvent extends AbstractApplicationEvent {

    ApplicationDetails applicationDetails;
    String branch;
    Map<String, String> serviceVersions;
    DeploymentStatus status = DeploymentStatus.None;
    String requestedVersion;

    /**
     * Create a new ApplicationEvent.
     * @param source the component that published the event (never {@code null})
     */
    DeploymentStartedEvent(ApplicationDetails applicationDetails, String branch, Map<String, String> serviceVersions, String requestedVersion) {
        super(applicationDetails.getTierName(), applicationDetails.getEnvironmentName(), applicationDetails.getId())

        this.applicationDetails = applicationDetails;
        this.branch = branch;
        this.serviceVersions = serviceVersions;
        this.requestedVersion = requestedVersion;
    }

    public static enum DeploymentStatus {
        Started, AlreadyRunning, None
    }

}
