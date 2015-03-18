/**
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
