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
