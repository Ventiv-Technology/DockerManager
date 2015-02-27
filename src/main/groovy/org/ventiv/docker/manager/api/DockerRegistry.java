package org.ventiv.docker.manager.api;

import feign.Param;
import feign.RequestLine;
import org.ventiv.docker.manager.model.ImageLayerInformation;

import java.util.Map;

/**
 * Docker Registry API, built on Feign.  API docs:
 * https://docs.docker.com/reference/api/registry_api
 *
 * Created by jcrygier on 2/25/15.
 */
public interface DockerRegistry {

    /**
     * Gets all of the tags for a given repository.  Returns a Map of Tag Name -> Image Hash ID
     *
     * @param namespace
     * @param repository
     * @return
     */
    @RequestLine("GET /v1/repositories/{namespace}/{repository}/tags")
    public Map<String, String> listRepositoryTags(@Param("namespace") String namespace, @Param("repository") String repository);

    /**
     * Gets a particular Image Layer's information.  Contains all of the information that built this image.
     *
     * @param imageId
     * @return
     */
    @RequestLine("GET /v1/images/{imageId}/json")
    public ImageLayerInformation getImageLayer(@Param("imageId") String imageId);

}
