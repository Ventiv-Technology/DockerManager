/**
 * Copyright (c) 2014 - 2017 Ventiv Technology
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
package org.ventiv.docker.manager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jcrygier on 8/11/17.
 */
@Component
public class DockerHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    private DockerService dockerService;

    @Autowired
    private ServiceInstanceService serviceInstanceService;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        // Determine if any Event Callbacks are closed
        List<ServiceInstanceService.DockerEventCallback> closedCallbacks = serviceInstanceService.getEventCallbacks().values().stream()
                .filter(callback -> !callback.isConnected())
                .collect(Collectors.toList());

        List<ServiceInstanceService.DockerEventCallback> openCallbacks = serviceInstanceService.getEventCallbacks().values().stream()
                .filter(ServiceInstanceService.DockerEventCallback::isConnected)
                .collect(Collectors.toList());

        if (closedCallbacks.isEmpty()) {
            builder.up()
                    .withDetail("servers", openCallbacks.stream().map(it -> it.getServerConfiguration().getHostname()).collect(Collectors.toList()));
        } else {
            builder.outOfService()
                    .withDetail("connectedServers", openCallbacks.stream().map(it -> it.getServerConfiguration().getHostname()).collect(Collectors.toList()))
                    .withDetail("disconnectedServers", closedCallbacks.stream().collect(Collectors.toMap(it -> it.getServerConfiguration().getHostname(), it -> it.getException().getLocalizedMessage())));
        }
    }

}
