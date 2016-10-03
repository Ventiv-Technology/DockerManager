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

import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.metrics.AdditionalMetricsStorage

/**
 * In Memory implementation of an Additional Metrics Store.  WARNING, this currently will chew through memory.
 */
class InMemoryAdditionalMetricsStore extends AbstractAdditionalMetricsStore {

    Map<String, List<AdditionalMetricsStorage>> store = [:];

    @Override
    List<AdditionalMetricsStorage> getAdditionalMetricsBetween(ServiceInstance serviceInstance, Long startTime, Long endTime) {
        if (startTime == null) startTime = Long.MIN_VALUE;
        if (endTime == null) endTime = System.currentTimeMillis();

        store[getKey(serviceInstance)]?.findAll { AdditionalMetricsStorage additionalMetricsStorage ->
            return startTime <= additionalMetricsStorage.getTimestamp() && additionalMetricsStorage.getTimestamp() <= endTime
        }?.sort { it.getTimestamp() }?.reverse()
    }

    @Override
    void storeAdditionalMetrics(ServiceInstance serviceInstance, AdditionalMetricsStorage additionalMetricsStorage) {
        if (!store.containsKey(getKey(serviceInstance)))
            store.put(getKey(serviceInstance), []);

        store[getKey(serviceInstance)] << additionalMetricsStorage;
    }

    @Override
    List<Map<String, Object>> getTimeSeries(String metricName, String serverName, String tierName, String environmentName, String applicationId, String serviceName, Integer instanceNumber, Long fromTimestamp, Long toTimestamp, String last, String groupTimeWindow) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    private String getKey(ServiceInstance serviceInstance) {
        return serviceInstance.getServerName() + "-" + serviceInstance.toString();
    }
}
