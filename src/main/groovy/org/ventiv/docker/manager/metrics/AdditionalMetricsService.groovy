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
package org.ventiv.docker.manager.metrics

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.event.UpdatedAdditionalMetricsEvent
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.configuration.AdditionalMetricsConfiguration
import org.ventiv.docker.manager.model.configuration.ServiceConfiguration
import org.ventiv.docker.manager.service.ServiceInstanceService

import javax.annotation.PostConstruct
import javax.annotation.Resource
import java.util.concurrent.ScheduledFuture

/**
 * Service to periodically get Additional Metrics from all Service Instances that are running.
 */
@Slf4j
@CompileStatic
@Service
class AdditionalMetricsService implements Runnable {

    @Resource ServiceInstanceService serviceInstanceService;
    @Resource ApplicationEventPublisher eventPublisher;
    @Resource DockerServiceConfiguration dockerServiceConfiguration;
    @Resource TaskScheduler taskScheduler;
    @Resource DockerManagerConfiguration props;

    private ScheduledFuture scheduledTask;

    @PostConstruct
    public void start() {
        if (props.additionalMetricsRefreshDelay > 0)
            scheduledTask = taskScheduler.scheduleWithFixedDelay(this, props.additionalMetricsRefreshDelay);
    }

    @Override
    void run() {
        serviceInstanceService.getServiceInstances().each { ServiceInstance serviceInstance ->
            if (serviceInstance.getStatus() == ServiceInstance.Status.Running) {
                Map<String, Object> additionalMetrics = getServiceInstanceAdditionalMetrics(serviceInstance);
                serviceInstance.setAdditionalMetrics(additionalMetrics);
                if (additionalMetrics)
                    eventPublisher.publishEvent(new UpdatedAdditionalMetricsEvent(serviceInstance, additionalMetrics));
            }
        }
    }

    public Map<String, Object> getServiceInstanceAdditionalMetrics(ServiceInstance serviceInstance) {
        ServiceConfiguration serviceConfiguration = dockerServiceConfiguration.getServiceConfiguration(serviceInstance.getName());
        Map<String, Object> additionalMetrics = serviceConfiguration?.getAdditionalMetrics()?.collectEntries { AdditionalMetricsConfiguration metricsConfiguration ->
            AdditionalMetrics additionalMetrics = DockerManagerApplication.getApplicationContext()?.getBean(metricsConfiguration.getType(), AdditionalMetrics)
            if (additionalMetrics)
                return [metricsConfiguration.getName(), additionalMetrics.getAdditionalMetrics(serviceInstance, metricsConfiguration.getSettings())];
            else
                return [metricsConfiguration.getName(), null];
        }

        serviceInstance.setAdditionalMetrics(additionalMetrics);
        return additionalMetrics;
    }


}
