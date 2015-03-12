package org.ventiv.docker.manager.build.jenkins;

/**
 * Created by jcrygier on 3/12/15.
 */
public class BuildStatus {

    List<Map<String, Object>> actions;
    boolean building;
    String description;
    Long duration;
    Long estimatedDuration;
    String fullDisplayName;
    String id;
    boolean keepLog;
    Integer number;
    BuildResult result;
    Date timestamp;
    String url;
    String builtOn;

    public static enum BuildResult {
        SUCCESS, FAILURE
    }

}
