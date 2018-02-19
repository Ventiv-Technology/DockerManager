/**
 * Copyright (c) 2014 - 2018 Ventiv Technology
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
package org.ventiv.docker.manager.plugin;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.ventiv.docker.manager.model.ActionDetails;
import org.ventiv.docker.manager.model.ServiceInstance;
import org.ventiv.docker.manager.security.DockerManagerPermission;
import org.ventiv.docker.manager.service.DockerService;

import java.util.Arrays;
import java.util.List;

public class OSActionsPlugin implements ActionPlugin {

    private final DockerService dockerService;

    public OSActionsPlugin(ApplicationContext applicationContext, Environment env) {
        dockerService = (DockerService) applicationContext.getBean("dockerService");
    }

    @Override
    public List<ActionDetails> getSupportedActions() {
        return Arrays.asList(
                new ActionDetails("OSThreadDump", "OS Thread Dump", "/app/partials/stdOutStdErrDisplay.html", DockerManagerPermission.PRIVILEGED_ACTION, (si) -> true, this::performOsThreadDump)
        );
    }

    private Object performOsThreadDump(ServiceInstance serviceInstance) {
        return dockerService.exec(serviceInstance, "ps", "-eLf");
    }
}
