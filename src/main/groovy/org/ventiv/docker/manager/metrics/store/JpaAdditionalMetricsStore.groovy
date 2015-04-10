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

import groovy.transform.CompileStatic
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.ServiceInstanceThumbnail
import org.ventiv.docker.manager.model.metrics.AdditionalMetricsStorage
import org.ventiv.docker.manager.repository.AdditionalMetricsStorageRepository
import org.ventiv.docker.manager.repository.ServiceInstanceThumbnailRepository

import javax.annotation.Resource

/**
 * Created by jcrygier on 4/9/15.
 */
@CompileStatic
class JpaAdditionalMetricsStore extends AbstractAdditionalMetricsStore {

    @Resource AdditionalMetricsStorageRepository repo;
    @Resource ServiceInstanceThumbnailRepository serviceInstanceThumbnailRepository;

    @Override
    List<AdditionalMetricsStorage> getAdditionalMetricsBetween(ServiceInstance serviceInstance, Long startTime, Long endTime) {
        if (startTime == null) startTime = Long.MIN_VALUE;
        if (endTime == null) endTime = System.currentTimeMillis();

        return repo.findByServiceInstanceThumbnailAndTimestampBetweenOrderByTimestampDesc(getServiceInstanceThumbnail(serviceInstance), startTime, endTime);
    }

    @Override
    void storeAdditionalMetrics(ServiceInstance serviceInstance, AdditionalMetricsStorage additionalMetricsStorage) {
        additionalMetricsStorage.setServiceInstanceThumbnail(getServiceInstanceThumbnail(serviceInstance));
        repo.save(additionalMetricsStorage);
    }

    private ServiceInstanceThumbnail getServiceInstanceThumbnail(ServiceInstance serviceInstance) {
        ServiceInstanceThumbnail thumbnail = serviceInstance.getServiceInstanceThumbnail();
        if (thumbnail == null)
            thumbnail = serviceInstanceThumbnailRepository.findByServerNameAndTierNameAndEnvironmentNameAndApplicationIdAndNameAndInstanceNumber(serviceInstance.getServerName(), serviceInstance.getTierName(), serviceInstance.getEnvironmentName(), serviceInstance.getApplicationId(), serviceInstance.getName(), serviceInstance.getInstanceNumber())

        if (thumbnail == null) {
            thumbnail = new ServiceInstanceThumbnail(serverName: serviceInstance.getServerName(), tierName: serviceInstance.getTierName(), environmentName: serviceInstance.getEnvironmentName(), applicationId: serviceInstance.getApplicationId(), name: serviceInstance.getName(), instanceNumber: serviceInstance.getInstanceNumber())
            serviceInstanceThumbnailRepository.save(thumbnail);
        }

        serviceInstance.setServiceInstanceThumbnail(thumbnail);

        return thumbnail;
    }

}
