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
package org.ventiv.docker.manager.service

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Version
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.utils.TimingUtils

import javax.annotation.Resource

/**
 * Gets a DockerClient for a given host.  Will attempt to use default values, but will override with application properties
 * configured via spring boot.
 *
 * - docker.client.<hostName>.apiVersion: Default: Call /version to determine
 * - docker.client.<hostName>.uri: Default: https://<hostName>:2376
 * - docker.client.<hostName>.certPath: Default: ./config/certs/<hostName>
 */
@Service
class DockerService {

    @Resource PropertyService props;

    private Map<String, String> hostToApiVersionName = [:]

    public DockerClient getDockerClient(String hostName) {
        TimingUtils.time("getDockerClient") {
            String apiVersion = props["docker.client.${hostName}.apiVersion"]
            String uri = props["docker.client.${hostName}.uri"] ?: "https://${hostName}:2376"
            String certs = props["docker.client.${hostName}.certPath"] ?: "./config/certs/${hostName}"
            certs = certs.replaceAll('~', System.getProperty('user.home'))

            if (!apiVersion)
                apiVersion = getApiVersion(hostName, uri, certs);

            DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                    .withVersion(apiVersion)
                    .withUri(uri)
                    .withDockerCertPath(certs)
                    .build();

            return DockerClientBuilder.getInstance(config).build();
        }
    }

    String getApiVersion(String hostName, String uri, String certs) {
        if (!hostToApiVersionName.containsKey(hostName)) {
            DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                .withUri(uri)
                .withDockerCertPath(certs)
                .build()

            Version ver = DockerClientBuilder.getInstance(config).build().versionCmd().exec();
            hostToApiVersionName.put(hostName, ver.getApiVersion());
        }

        return hostToApiVersionName[hostName]
    }
}
