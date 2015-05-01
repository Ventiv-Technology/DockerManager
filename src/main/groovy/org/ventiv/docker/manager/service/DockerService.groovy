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
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.dockerjava.ImageHistoryCmd
import org.ventiv.docker.manager.dockerjava.ImageHistoryCmdExec
import org.ventiv.docker.manager.dockerjava.ImageHistoryCmdImpl
import org.ventiv.docker.manager.dockerjava.RenameContainerCmd
import org.ventiv.docker.manager.dockerjava.RenameContainerCmdExec
import org.ventiv.docker.manager.dockerjava.RenameContainerCmdImpl
import org.ventiv.docker.manager.utils.TimingUtils

import javax.annotation.Resource
import javax.ws.rs.client.WebTarget

/**
 * Gets a DockerClient for a given host.  Will attempt to use default values, but will override with application properties
 * configured via spring boot.
 *
 * - docker.client.<hostName>.apiVersion: Default: Call /version to determine
 * - docker.client.<hostName>.uri: Default: https://<hostName>:2376
 * - docker.client.<hostName>.certPath: Default: ./config/certs/<hostName>
 */
@Service
@Profile("!integrationTest")
class DockerService {

    @Resource PropertyService props;
    @Resource DockerManagerConfiguration dockerManagerConfiguration;

    private Map<String, String> hostToApiVersionName = [:]

    public DockerClient getDockerClient(String hostName) {
        TimingUtils.time("getDockerClient") {
            // Get the Certs path
            String certs = props["docker.client.${hostName}.certPath"] ?: "./config/certs/${hostName}"
            certs = certs.replaceAll('~', System.getProperty('user.home'))
            boolean certsExist = new File(certs).exists()

            String apiVersion = props["docker.client.${hostName}.apiVersion"]
            String uri = props["docker.client.${hostName}.uri"] ?: certsExist ? "https://${hostName}:2376" : "http://${hostName}:2375"

            if (!apiVersion)
                apiVersion = getApiVersion(hostName, uri, certs);
            else
                hostToApiVersionName.put(hostName, apiVersion);

            DockerClientConfig.DockerClientConfigBuilder builder = DockerClientConfig.createDefaultConfigBuilder()
                    .withVersion(apiVersion)
                    .withUri(uri);

            if (certsExist)
                builder.withDockerCertPath(certs)
            else
                builder.withSSLConfig(null);

            return DockerClientBuilder.getInstance(builder.build()).build();
        }
    }

    private String getApiVersion(String hostName, String uri, String certs) {
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

    Integer getMajorApiVersion(String hostName) {
        return Integer.parseInt(hostToApiVersionName[hostName].split('\\.')[0])
    }

    Integer getMinorApiVersion(String hostName) {
        return Integer.parseInt(hostToApiVersionName[hostName].split('\\.')[1])
    }

    WebTarget getBaseResource(String hostName) {
        return getDockerClient(hostName).getDockerCmdExecFactory().getBaseResource()
    }

    RenameContainerCmd getRenameContainerCmd(String hostName, String containerId, String newName) {
        return new RenameContainerCmdImpl(new RenameContainerCmdExec(getBaseResource(hostName)), containerId, newName);
    }

    ImageHistoryCmd getImageHistoryCmd(String hostName, String imageName) {
        return new ImageHistoryCmdImpl(new ImageHistoryCmdExec(getBaseResource(hostName)), imageName);
    }

}
