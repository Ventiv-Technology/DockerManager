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
package org.ventiv.docker.manager

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.ListContainersCmd
import com.github.dockerjava.api.model.Container
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.boot.test.WebIntegrationTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ContextConfiguration
import org.ventiv.docker.manager.mock.MockDockerClient
import org.ventiv.docker.manager.service.DockerService
import spock.lang.Specification

/**
 * Created by jcrygier on 4/13/15.
 */
@WebIntegrationTest([
        "spring.profiles.active=integrationTest",
        "config.location=file:./sample-config/env-config"
])
@Configuration
@ContextConfiguration(loader = SpringApplicationContextLoader, classes = DockerManagerApplication)
class AbstractIntegrationTest extends Specification {

    Map<String, MockDockerClient> mockedDockerClients;

    @Bean
    public DockerService dockerService() {
        return new DockerService() {
            public DockerClient getDockerClient(String hostName) {
                return getMockedClient(hostName);
            }
        }
    }

    public MockDockerClient getMockedClient(String hostName) {
        if (mockedDockerClients == null)
            mockedDockerClients = [:]

        if (!mockedDockerClients.containsKey(hostName))
            mockedDockerClients.put(hostName, new MockDockerClient(execResponses: [(ListContainersCmd): getContainersForStartup(hostName)]))

        return mockedDockerClients[hostName];
    }

    public void setUser(String userName, String password, Collection<GrantedAuthority> authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userName, password, authorities))
    }

    public List<Container> getContainersForStartup(String hostName) {
        return []
    }

}
