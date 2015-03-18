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

import groovy.util.logging.Slf4j
import org.ventiv.docker.manager.model.ServiceInstance

/**
 * Created by jcrygier on 3/16/15.
 */
@Slf4j
class CreateContainerEvent extends AbstractServiceInstanceEvent {

    Map<String, String> environmentVariables;

    CreateContainerEvent(ServiceInstance serviceInstance, Map<String, String> environmentVariables) {
        super(serviceInstance)

        this.environmentVariables = environmentVariables;

        log.info("Creating new Docker Container on Host: '${serviceInstance.getServerName()}' " +
                "with image: '${serviceInstance.getContainerImage().toString()}', " +
                "name: '${serviceInstance.toString()}', " +
                "env: ${environmentVariables.collect {k, v -> "$k=$v"}}")
    }

}
