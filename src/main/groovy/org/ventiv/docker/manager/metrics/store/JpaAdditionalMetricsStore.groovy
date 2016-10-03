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

import groovy.time.TimeCategory
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.ServiceInstanceThumbnail
import org.ventiv.docker.manager.model.metrics.AdditionalMetricsStorage
import org.ventiv.docker.manager.repository.AdditionalMetricsStorageRepository
import org.ventiv.docker.manager.service.ServiceInstanceService
import org.ventiv.docker.manager.utils.StringUtils

import javax.annotation.Resource

/**
 * Created by jcrygier on 4/9/15.
 */
@CompileStatic
class JpaAdditionalMetricsStore extends AbstractAdditionalMetricsStore {

    @Resource JdbcTemplate jdbcTemplate;
    @Resource AdditionalMetricsStorageRepository repo;
    @Resource ServiceInstanceService serviceInstanceService;

    @Override
    List<AdditionalMetricsStorage> getAdditionalMetricsBetween(ServiceInstance serviceInstance, Long startTime, Long endTime) {
        if (startTime == null) startTime = Long.MIN_VALUE;
        if (endTime == null) endTime = System.currentTimeMillis();

        ServiceInstanceThumbnail thumbnail = serviceInstanceService.getServiceInstanceThumbnail(serviceInstance);
        return repo.findByServiceInstanceThumbnailAndTimestampBetweenOrderByTimestampDesc(thumbnail, startTime, endTime);
    }

    @Override
    void storeAdditionalMetrics(ServiceInstance serviceInstance, AdditionalMetricsStorage additionalMetricsStorage) {
        additionalMetricsStorage.setServiceInstanceThumbnail(serviceInstanceService.getServiceInstanceThumbnail(serviceInstance));
        repo.save(additionalMetricsStorage);
    }

    @CompileDynamic
    @Override
    List<Map<String, Object>> getTimeSeries(String metricName, String serverName, String tierName, String environmentName, String applicationId, String serviceName, Integer instanceNumber, Long fromTimestamp, Long toTimestamp, String last, String groupTimeWindow) {
        // If last is populated, use that
        if (last) {
            use (TimeCategory) {
                if (toTimestamp == Long.MAX_VALUE) toTimestamp = new Date().getTime();
                fromTimestamp = Eval.me("new Date($toTimestamp) - $last").getTime()
            }
        }

        Long timeWindow = props.additionalMetricsRefreshDelay;
        if (groupTimeWindow) {
            use (TimeCategory) {
                timeWindow = Eval.me("Date now = new Date(); now.getTime() - (now - $groupTimeWindow).getTime()")
            }
        }

        Map<String, ?> serviceInstanceParameters = [serverName: serverName, instanceNumber: instanceNumber];
        Map<String, ?> applicationParameters = [tierName: tierName, environmentName: environmentName, applicationId: applicationId];
        Map<String, ?> queryParameters = [metricName: metricName, fromTimestamp: fromTimestamp, toTimestamp: toTimestamp];

        StringBuilder serviceInstanceWhere = new StringBuilder()
        serviceInstanceParameters.each { k, v ->
            if (v) {
                serviceInstanceWhere << " AND SERVICE_INSTANCE."
                serviceInstanceWhere << StringUtils.toSnakeCase(k)
                serviceInstanceWhere << " = :"
                serviceInstanceWhere << k
            }
        }

        applicationParameters.each { k, v ->
            if (v) {
                serviceInstanceWhere << " AND APPLICATION."
                serviceInstanceWhere << StringUtils.toSnakeCase(k)
                serviceInstanceWhere << " = :"
                serviceInstanceWhere << k
            }
        }

        StringBuilder queryText = new StringBuilder("""
            select TIMESTAMP, MIN(VALUE) as MIN, MAX(VALUE) as MAX, AVG(VALUE) as AVG, SUM(VALUE) as SUM, COUNT(VALUE) as COUNT FROM (
                SELECT (ADDITIONAL_METRICS_STORAGE.TIMESTAMP/$timeWindow)*$timeWindow AS TIMESTAMP, ADDITIONAL_METRICS_VALUES.VALUE
                FROM ADDITIONAL_METRICS_VALUES
                INNER JOIN ADDITIONAL_METRICS_STORAGE on ADDITIONAL_METRICS_STORAGE.ID = ADDITIONAL_METRICS_VALUES.ADDITIONAL_METRICS_STORAGE_ID
                INNER JOIN SERVICE_INSTANCE on ADDITIONAL_METRICS_STORAGE.SERVICE_INSTANCE_ID = SERVICE_INSTANCE.ID
                INNER JOIN APPLICATION on APPLICATION.ID = SERVICE_INSTANCE.APPLICATION_ID
                where ADDITIONAL_METRICS_VALUES.NAME = :metricName
                AND ADDITIONAL_METRICS_STORAGE.TIMESTAMP BETWEEN :fromTimestamp and :toTimestamp
                $serviceInstanceWhere
            ) group by TIMESTAMP order by TIMESTAMP
        """);

        return new NamedParameterJdbcTemplate(jdbcTemplate).queryForList(queryText.toString(), queryParameters + serviceInstanceParameters + applicationParameters).collect { it.collectEntries { k, v -> return [k.toLowerCase(), v]} };
    }
}
