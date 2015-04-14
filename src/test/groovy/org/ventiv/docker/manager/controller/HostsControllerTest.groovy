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
package org.ventiv.docker.manager.controller

import com.github.dockerjava.api.command.LogContainerCmd
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletResponse
import org.ventiv.docker.manager.AbstractIntegrationTest
import org.ventiv.docker.manager.mock.MockDockerClient
import org.ventiv.docker.manager.service.DockerService

/**
 * Created by jcrygier on 4/13/15.
 */
class HostsControllerTest extends AbstractIntegrationTest {

    @Autowired HostsController hostsController;
    @Autowired DockerService dockerService;

    private MockDockerClient mockClient;

    def setup() {
        mockClient = (MockDockerClient) dockerService.getDockerClient("boot2docker")
    }

    def "can get standard out copied to servlet output stream"() {
        setup:
        mockClient.getExecResponses().put(LogContainerCmd, new StringBufferInputStream("This is a test log"))

        when:
        MockHttpServletResponse response = new MockHttpServletResponse();
        hostsController.getStdOutLog("boot2docker", "aasdf", 0, response);

        then:
        response.getContentAsString() == "This is a test log"
    }

}
