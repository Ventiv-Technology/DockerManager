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
package org.ventiv.docker.manager.build.jenkins;

import feign.Param;
import feign.RequestLine;

/**
 * Created by jcrygier on 3/12/15.
 */

public interface JenkinsApi {

    /**
     * Gets all of the tags for a given repository.  Returns a Map of Tag Name -> Image Hash ID
     *
     * @param jobName
     * @return
     */
    @RequestLine("POST /job/{jobName}/build")
    public BuildStartedResponse startNewBuild(@Param("jobName") String jobName);

    /**
     * Gets the status of an item that is queued to build.
     *
     * @param queueId
     * @return
     */
    @RequestLine("GET /queue/item/{queueId}/api/json")
    public BuildQueueStatus getBuildQueueStatus(@Param("queueId") Integer queueId);

    /**
     * Gets the status of a build.
     *
     * @param jobName from BuildQueueStatus.task.name
     * @param buildNumber from BuildQueueStatus.executable.number
     * @return
     */
    @RequestLine("GET /job/{jobName}/{buildNumber}/api/json")
    public BuildStatus getBuildStatus(@Param("jobName") String jobName, @Param("buildNumber") Integer buildNumber);

}
