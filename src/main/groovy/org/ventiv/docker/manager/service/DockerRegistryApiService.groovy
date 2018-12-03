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

import feign.Feign
import feign.auth.BasicAuthRequestInterceptor
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.ventiv.docker.manager.api.DockerRegistry
import org.ventiv.docker.manager.api.DockerRegistryV1
import org.ventiv.docker.manager.api.DockerRegistryV2
import org.ventiv.docker.manager.api.HeaderRequestInterceptor
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.model.DockerTag

import javax.annotation.Resource

/**
 * Created by jcrygier on 2/25/15.
 */
@Service
class DockerRegistryApiService {

    private Map<String, DockerRegistry> privateRegistryMap = [:]        // Key is registry
    private Map<String, DockerRegistry> dockerHubMap = [:]              // Key is namespace/repository
    @Resource DockerManagerConfiguration props;

    public DockerRegistry getRegistry(DockerTag tag) {
        if (tag.isDockerHub()) {
            return getDockerHubRegistryV2(tag);
        } else {
            return getPrivateRegistry(tag.getRegistry());
        }
    }

    public List<String> getTagsForImage(DockerTag tag) {
        DockerRegistry registry = getRegistry(tag);
        if (registry instanceof DockerRegistryV2)
            return registry.listRepositoryTags(tag.repository).tags as List<String>;
        else if (registry instanceof DockerRegistryV1)
            return registry.listRepositoryTags(tag.namespace, tag.repository).keySet() as List<String>;
    }

    private DockerRegistryV1 getDockerHubRegistry(DockerTag tag) {
        String key = tag.getNamespace() + "/" + tag.getRepository();
        if (!dockerHubMap.containsKey(key)) {
            // Make our restful call to the docker hub to get the registry server + token
            final RESTClient client =  new RESTClient("https://index.docker.io")
            HttpResponseDecorator response = client.get([
                    path: "/v1/repositories/$key/images",
                    headers: [ "X-Docker-Token": "true" ]
            ])

            String dockerToken = response.getFirstHeader("X-Docker-Token").getValue()
            String registryEndpoint = response.getFirstHeader("X-Docker-Endpoints").getValue()

            dockerHubMap.put(key,
                    Feign.builder()
                            .decoder(new JacksonDecoder())
                            .encoder(new JacksonEncoder())
                            .requestInterceptor(new HeaderRequestInterceptor([Authorization: "Token " + dockerToken]))
                            .target(DockerRegistryV1, "https://$registryEndpoint")
            )
        }

        return dockerHubMap[key];
    }

    private DockerRegistryV2 getDockerHubRegistryV2(DockerTag tag) {
        // Make our restful call to the docker hub to get the registry server + token
        final RESTClient client =  new RESTClient("https://auth.docker.io/")
        HttpResponseDecorator response = client.get([
                path: "token",
                query: [
                        service: 'registry.docker.io',
                        scope: "repository:$tag.namespace/$tag.repository:pull"
                ]
        ])

        String dockerToken = response.getData().token;

        return Feign.builder()
                        .decoder(new JacksonDecoder())
                        .encoder(new JacksonEncoder())
                        .requestInterceptor(new HeaderRequestInterceptor([Authorization: "Bearer " + dockerToken]))
                        .target(DockerRegistryV2, "https://index.docker.io")
    }

    /**
     * Gets a DockerRegistry for a non-docker hub request.
     *
     * TODO: Authentication?
     *
     * @param registryName
     * @return
     */
    private DockerRegistry getPrivateRegistry(String registryName) {

        if (!privateRegistryMap.containsKey(registryName)) {
            privateRegistryMap.put(registryName,
                    Feign.builder()
                            .decoder(new JacksonDecoder())
                            .encoder(new JacksonEncoder())
                            .requestInterceptor(new BasicAuthRequestInterceptor(props.config.registry.username, props.config.registry.password))
                            .target(getDockerRegistryClass(registryName), "https://$registryName")
            )
        }

        return privateRegistryMap[registryName];
    }

    private Class<DockerRegistry> getDockerRegistryClass(String registryName) {
        RestTemplate restTemplate = new RestTemplate();

        // Try making an HTTPS call to the registry /v2/.  If this comes back fine, it's a V2 Registry, otherwise it's a V1
        try {
            restTemplate.getForEntity("https://$registryName/v2/", Map);
            return DockerRegistryV2
        } catch (HttpClientErrorException ignored) {
            try {
                restTemplate.getForEntity("https://$registryName/v2", Map);
                return DockerRegistryV2
            } catch (HttpClientErrorException ignored2) {
                return DockerRegistryV1
            }
        }
    }

}
