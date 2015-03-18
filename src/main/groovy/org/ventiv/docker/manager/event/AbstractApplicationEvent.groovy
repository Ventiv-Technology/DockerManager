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
abstract class AbstractApplicationEvent extends ApplicationEvent {

    String tierName;
    String environmentName;
    String applicationId;

    AbstractApplicationEvent(String tierName, String environmentName, String applicationId) {
        super([
                tierName: tierName,
                environmentName: environmentName,
                applicationId: applicationId
        ])

        this.tierName = tierName;
        this.environmentName = environmentName;
        this.applicationId = applicationId;
    }

}
