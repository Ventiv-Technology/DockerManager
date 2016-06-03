package org.ventiv.docker.manager.api;

import feign.Param;
import feign.RequestLine;

import java.util.List;
import java.util.Map;

/**
 * TODO: Hmmm, this really shouldn't extend DockerRegistry....they're not really related
 * Created by jcrygier on 6/2/16.
 */
public interface DockerRegistryV2 extends DockerRegistry {

    /**
     * Gets all of the tags for a given repository.  Returns a Map of Tag Name -> Image Hash ID
     *
     * @param repository
     * @return
     */
    @RequestLine("GET /v2/{repository}/tags/list")
    public RepositoryTagList listRepositoryTags(@Param("repository") String repository);

    public static final class RepositoryTagList {
        public String name;
        public List<String> tags;
    }

}
