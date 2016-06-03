/**
 * Copyright (c) 2014 - 2016 Ventiv Technology
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
