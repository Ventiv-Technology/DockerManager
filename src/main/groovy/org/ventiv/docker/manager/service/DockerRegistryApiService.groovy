package org.ventiv.docker.manager.service

import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.api.DockerRegistry
import org.ventiv.docker.manager.api.HeaderRequestInterceptor
import org.ventiv.docker.manager.model.DockerTag

/**
 * Created by jcrygier on 2/25/15.
 */
@Service
class DockerRegistryApiService {

    private Map<String, DockerRegistry> privateRegistryMap = [:]        // Key is registry
    private Map<String, DockerRegistry> dockerHubMap = [:]              // Key is namespace/repository

    public DockerRegistry getRegistry(DockerTag tag) {
        if (tag.isDockerHub()) {
            return getDockerHubRegistry(tag);
        } else {
            return getPrivateRegistry(tag.getRegistry());
        }
    }

    private DockerRegistry getDockerHubRegistry(DockerTag tag) {
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
                            .target(DockerRegistry, "https://$registryEndpoint")
            )
        }

        return dockerHubMap[key];
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
                            .target(DockerRegistry, "https://$registryName")
            )
        }

        return privateRegistryMap[registryName];
    }

}
