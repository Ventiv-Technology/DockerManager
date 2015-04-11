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
package org.ventiv.docker.manager.service

import groovy.transform.CompileStatic
import org.springframework.core.io.Resource
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.config.DockerManagerConfiguration

import javax.annotation.PostConstruct
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture

/**
 * Simple service to watch for when a Resource changes
 */
@CompileStatic
@Service
class ResourceWatcherService implements Runnable {

    @javax.annotation.Resource TaskScheduler taskScheduler;
    @javax.annotation.Resource DockerManagerConfiguration props;

    private ScheduledFuture scheduledTask;
    private ConcurrentHashMap<Resource, Closure<?>> watchServiceCallbacks = new ConcurrentHashMap<>();
    private Map<Resource, Long> lastUpdatedTimestamps = [:];


    @PostConstruct
    public void startWatcherThread() {
        if (props.config.refreshPeriod > 0)
            scheduledTask = taskScheduler.scheduleAtFixedRate(this, props.config.refreshPeriod);
    }

    public void watchResource(Resource resource, Closure<?> callback) {
        watchServiceCallbacks.put(resource, callback);
    }

    @Override
    void run() {
        watchServiceCallbacks.each { Resource toCheck, Closure<?> callback ->
            Long lastUpdatedTimestamp = lastUpdatedTimestamps[toCheck] ?: 0L;

            if (toCheck.lastModified() != lastUpdatedTimestamp) {
                lastUpdatedTimestamps[toCheck] = toCheck.lastModified();

                if (lastUpdatedTimestamp != 0)
                    callback(toCheck);
            }
        }
    }

    void stop() {
        scheduledTask.cancel(false);
    }
}
