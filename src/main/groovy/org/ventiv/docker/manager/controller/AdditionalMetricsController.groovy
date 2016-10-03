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
package org.ventiv.docker.manager.controller

import groovy.time.TimeCategory
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.config.DockerServiceConfiguration
import org.ventiv.docker.manager.metrics.store.AbstractAdditionalMetricsStore
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.configuration.AdditionalMetricsConfiguration
import org.ventiv.docker.manager.model.configuration.ApplicationConfiguration
import org.ventiv.docker.manager.model.configuration.EnvironmentConfiguration
import org.ventiv.docker.manager.model.configuration.ServerConfiguration
import org.ventiv.docker.manager.model.configuration.ServiceConfiguration
import org.ventiv.docker.manager.model.metrics.AdditionalMetricsStorage
import org.ventiv.docker.manager.service.EnvironmentConfigurationService
import org.ventiv.docker.manager.service.ServiceInstanceService

import javax.annotation.Resource
import javax.servlet.http.HttpServletResponse

/**
 * Created by jcrygier on 4/9/15.
 */
@Slf4j
@CompileStatic
@RestController
@RequestMapping("/api/metrics")
class AdditionalMetricsController {

    @Resource DockerManagerConfiguration props;
    @Resource ServiceInstanceService serviceInstanceService;
    @Resource AbstractAdditionalMetricsStore additionalMetricsStore;
    @Resource JdbcTemplate jdbcTemplate;
    @Resource DockerServiceConfiguration dockerServiceConfiguration;
    @Resource EnvironmentConfigurationService environmentConfigurationService;

    @RequestMapping
    public Map<String, Object> getEditOptions() {
        List<Map<String, String>> availableMetrics = []
        dockerServiceConfiguration.getConfiguration().each { ServiceConfiguration serviceConfiguration ->
            serviceConfiguration.getAdditionalMetrics()?.each { AdditionalMetricsConfiguration additionalMetricsConfiguration ->
                additionalMetricsConfiguration.getStorage()?.keySet()?.each { String metricName ->
                    availableMetrics << [
                            service: serviceConfiguration.getName(),
                            serviceDescription: serviceConfiguration.getDescription(),
                            metricName: additionalMetricsConfiguration.getName() + "." + metricName,
                            metricDescription: additionalMetricsConfiguration.getStorageDescriptions()?.get(metricName)
                    ]
                }
            }
        }

        List<Map<String, String>> availableServers = []
        List<Map<String, String>> availableTiers = []
        List<Map<String, String>> availableEnvironments = []
        List<Map<String, String>> availableApplications = []
        environmentConfigurationService.getActiveEnvironments().each { EnvironmentConfiguration environmentConfiguration ->
            environmentConfiguration.getServers()?.each { ServerConfiguration serverConfiguration ->
                availableServers << [
                        hostName: serverConfiguration.getHostname(),
                        hostDescription: serverConfiguration.getDescription(),
                        environmentId: environmentConfiguration.getId(),
                        environmentDescription: environmentConfiguration.getDescription()
                ]
            }

            environmentConfiguration.getApplications()?.each { ApplicationConfiguration applicationConfiguration ->
                availableApplications << [
                        applicationId: applicationConfiguration.getId(),
                        applicationDescription: applicationConfiguration.getDescription(),
                        environmentId: environmentConfiguration.getId(),
                        environmentDescription: environmentConfiguration.getDescription()
                ]
            }

            availableTiers << [
                    tierName: environmentConfiguration.getTierName()
            ]

            availableEnvironments << [
                    environmentId: environmentConfiguration.getId(),
                    environmentDescription: environmentConfiguration.getDescription(),
                    tierName: environmentConfiguration.getTierName()
            ]
        }

        return [
                availableMetrics: availableMetrics,
                availableServers: availableServers,
                availableTiers: availableTiers.unique(),
                availableEnvironments: availableEnvironments,
                availableApplications: availableApplications
        ];
    }

    @RequestMapping("/container/{containerId}")
    public List<AdditionalMetricsStorage> getMetricsForContainer(@PathVariable("containerId") String containerId,
                                                            @RequestParam(value = "startDate", required = false) Date startDate,
                                                            @RequestParam(value = "endDate", required = false) Date endDate) {
        ServiceInstance serviceInstance = serviceInstanceService.getServiceInstance(containerId);
        if (serviceInstance)
            return additionalMetricsStore.getAdditionalMetricsBetween(serviceInstance, startDate?.getTime(), endDate?.getTime())
        else
            return null;
    }

    @RequestMapping("/application/{tierName}/{environmentId}/{applicationId}/{serviceType}/{metricName}")
    public Collection<Object> getApplicationMetrics(@PathVariable("tierName") String tierName,
                                              @PathVariable("environmentId") String environmentId,
                                              @PathVariable("applicationId") String applicationId,
                                              @PathVariable("serviceType") String serviceType,
                                              @PathVariable("metricName") String metricName) {
        Collection<ServiceInstance> eligibleServices = serviceInstanceService.getServiceInstances().findAll {
            it.getTierName() == tierName && it.getEnvironmentName() == environmentId && it.getApplicationId() == applicationId && it.getName() == serviceType
        }

        return eligibleServices ? eligibleServices.collect { it?.getAdditionalMetrics()?.get(metricName) }.findAll { it }.flatten() : []
    }

    /**
     * Gets Time series for a given additional metric.
     *
     * Note: Unfortunately, we couldn't use JPA queries for this, as they do not allow for subqueries in the FROM clause.
     *
     * @param metricName Required, name of the metric to get
     * @param serverName Optional, filter by Service Instance Server Name
     * @param tierName Optional, filter by Service Instance
     * @param environmentName Optional, filter by Service Instance
     * @param applicationId Optional, filter by Service Instance
     * @param serviceName Optional, filter by Service Instance
     * @param instanceNumber Optional, filter by Service Instance
     * @param fromTimestamp Optional, filter by Events that occurred AFTER the given time
     * @param toTimestamp Optional, filter by Events that occurred BEFORE the given time
     * @param last Optional, filter by Events that occurred within the last x units of time.  Calculated from 'toTimestamp' if provided.  Uses Groovy's TimeCategory (e.g. 5.minutes or 30.seconds)
     * @param groupTimeWindow Optional, group events by a time window.  Defaults to configuration property: additionalMetricsRefreshDelay.  Uses Groovy's TimeCategory (e.g. 5.minutes or 30.seconds)
     * @return
     */
    @CompileDynamic
    @RequestMapping("/timeseries/{metricName:.*}")
    public List<Map<String, Object>> getTimeSeries(@PathVariable("metricName") String metricName,
                                                   @RequestParam(value = "serverName", required = false) String serverName,
                                                   @RequestParam(value = "tierName", required = false) String tierName,
                                                   @RequestParam(value = "environmentName", required = false) String environmentName,
                                                   @RequestParam(value = "applicationId", required = false) String applicationId,
                                                   @RequestParam(value = "serviceName", required = false) String serviceName,
                                                   @RequestParam(value = "instanceNumber", required = false) Integer instanceNumber,
                                                   @RequestParam(value = "fromTimestamp", required = false, defaultValue = "-9223372036854775808") Long fromTimestamp,
                                                   @RequestParam(value = "toTimestamp", required = false, defaultValue = "9223372036854775807") Long toTimestamp,
                                                   @RequestParam(value = "last", required = false) String last,
                                                   @RequestParam(value = "groupTimeWindow", required = false) String groupTimeWindow,
                                                   HttpServletResponse response) {
        Long timeWindow = props.additionalMetricsRefreshDelay;
        if (groupTimeWindow) {
            use (TimeCategory) {
                timeWindow = Eval.me("Date now = new Date(); now.getTime() - (now - $groupTimeWindow).getTime()")
            }
        }

        response.setHeader("X-Refresh-Period", timeWindow.toString());

        return additionalMetricsStore.getTimeSeries(metricName, serverName, tierName, environmentName, applicationId, serviceName, instanceNumber, fromTimestamp, toTimestamp, last, groupTimeWindow);
    }

}
