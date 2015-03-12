package org.ventiv.docker.manager.build.jenkins;

/**
 * Created by jcrygier on 3/12/15.
 */
public class BuildQueueStatus {

    List<Map<String, Object>> actions;
    boolean blocked;
    boolean buildable;
    Integer id;
    Date inQueueSince;
    String params;
    boolean stuck;
    TaskInformation task;
    String url;
    String why;
    boolean cancelled;
    ExecutableInformation executable;

    public static class TaskInformation{
        String name;
        String url;
        String color;
    }

    public static class ExecutableInformation {
        Integer number;
        String url;
    }

}
