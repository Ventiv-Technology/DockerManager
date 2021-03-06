/*
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
