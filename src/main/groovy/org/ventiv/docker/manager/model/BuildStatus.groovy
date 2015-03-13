package org.ventiv.docker.manager.model

/**
 * Created by jcrygier on 3/13/15.
 */
class BuildStatus {

    String tierName;
    String environmentName;
    String applicationId;
    boolean building;
    Map<String, String> serviceBuildStatus;

}
