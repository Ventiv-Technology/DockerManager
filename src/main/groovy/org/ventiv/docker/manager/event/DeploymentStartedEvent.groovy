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

import org.springframework.context.ApplicationEvent

/**
 * Created by jcrygier on 3/16/15.
 */
class DeploymentStartedEvent extends ApplicationEvent {

    /**
     * Create a new ApplicationEvent.
     * @param source the component that published the event (never {@code null})
     */
    DeploymentStartedEvent(String tierName, String environmentName, String applicationId, Map<String, String> serviceVersions) {
        super([
                tierName: tierName,
                environmentName: environmentName,
                applicationId: applicationId,
                serviceVersions: serviceVersions
        ])
    }

}
