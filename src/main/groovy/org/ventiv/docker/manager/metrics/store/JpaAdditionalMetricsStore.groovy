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
import org.ventiv.docker.manager.repository.AdditionalMetricsStorageRepository

import javax.annotation.Resource

/**
 * Created by jcrygier on 4/9/15.
 */
class JpaAdditionalMetricsStore extends AbstractAdditionalMetricsStore {

    @Resource AdditionalMetricsStorageRepository repo;

    @Override
    List<AdditionalMetricsStorage> getAdditionalMetricsBetween(ServiceInstance serviceInstance, Long startTime, Long endTime) {
        if (startTime == null) startTime = Long.MIN_VALUE;
        if (endTime == null) endTime = System.currentTimeMillis();

        return repo.findByServerNameAndServiceInstanceAndTimestampBetweenOrderByTimestampDesc(serviceInstance.getServerName(), serviceInstance.toString(), startTime, endTime);
    }

    @Override
    void storeAdditionalMetrics(ServiceInstance serviceInstance, AdditionalMetricsStorage additionalMetricsStorage) {
        repo.save(additionalMetricsStorage);
    }

}
