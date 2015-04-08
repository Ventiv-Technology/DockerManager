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
package org.ventiv.docker.manager.metrics.store

import org.springframework.context.ApplicationListener
import org.ventiv.docker.manager.event.UpdatedAdditionalMetricsEvent
import org.ventiv.docker.manager.model.ServiceInstance

/**
 * Base class for any Additional Metrics Store.  This will store Additional Metrics when they are retrieved, so we
 * can operate on them later.
 */
abstract class AbstractAdditionalMetricsStore implements ApplicationListener<UpdatedAdditionalMetricsEvent> {

    @Override
    void onApplicationEvent(UpdatedAdditionalMetricsEvent event) {
        storeAdditionalMetrics(event.getServiceInstance(), event.getAdditionalMetrics(), event.getTimestamp());
    }

    /**
     * Retrieves the latest Additional Metrics object for this Service Instance.
     *
     * @param serviceInstance
     * @return
     */
    Map<String, Object> getLatestAdditionalMetrics(ServiceInstance serviceInstance) {
        List<Map<String, Object>> allMetrics = getAdditionalMetricsBetween(serviceInstance, null, null);
        if (allMetrics)
            return allMetrics.last();
        else
            return null;
    }

    /**
     * Gets all of the Additional Metrics that have been recorded between the two time stamps.
     *
     * @param serviceInstance
     * @param startTime Starting time, if null, get all since the start of time (inclusive)
     * @param endTime Ending time, if null, get all until now (inclusive)
     * @return All metrics between the two timestamps, sorted by time
     */
    public abstract List<Map<String, Object>> getAdditionalMetricsBetween(ServiceInstance serviceInstance, Long startTime, Long endTime);

    public abstract void storeAdditionalMetrics(ServiceInstance serviceInstance, Map<String, Object> additionalMetrics, Long timestamp);

}
