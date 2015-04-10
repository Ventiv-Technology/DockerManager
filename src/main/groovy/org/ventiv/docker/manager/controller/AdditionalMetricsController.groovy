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

import com.google.common.net.MediaType
import groovy.time.TimeCategory
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.metrics.store.AbstractAdditionalMetricsStore
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.metrics.AdditionalMetricsStorage
import org.ventiv.docker.manager.service.ServiceInstanceService

import javax.annotation.Resource
import javax.persistence.EntityManager
import javax.persistence.Query
import javax.servlet.http.HttpServletResponse

/**
 * Created by jcrygier on 4/9/15.
 */
@Slf4j
@CompileStatic
@RestController
@RequestMapping("/api/metrics")
class AdditionalMetricsController {

    @Resource ServiceInstanceService serviceInstanceService;
    @Resource AbstractAdditionalMetricsStore additionalMetricsStore;
    @Resource EntityManager em;

    @RequestMapping("/widgets")
    public void additionalMetricsWidgets(HttpServletResponse response) {
        response.setContentType(MediaType.TEXT_JAVASCRIPT_UTF_8.toString())
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

    /**
     * Gets Time series for a given additional metric.
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
                                                   @RequestParam(value = "last", required = false) String last) {
        // If last is populated, use that
        if (last) {
            use (TimeCategory) {
                if (toTimestamp == Long.MAX_VALUE) toTimestamp = new Date().getTime();
                fromTimestamp = Eval.me("new Date($toTimestamp) - $last").getTime()
            }
        }

        StringBuilder queryText = new StringBuilder("select m.timestamp, min(value(m.additionalMetrics)), max(value(m.additionalMetrics)), avg(value(m.additionalMetrics)), sum(value(m.additionalMetrics)), count(m) from AdditionalMetricsStorage m where key(m.additionalMetrics) = :metricName and m.timestamp between :fromTimestamp and :toTimestamp ");
        Map<String, ?> queryParameters = [metricName: metricName, fromTimestamp: fromTimestamp, toTimestamp: toTimestamp];

        setVariableIfPopulated("serverName", serverName, queryParameters, queryText);
        setVariableIfPopulated("tierName", tierName, queryParameters, queryText);
        setVariableIfPopulated("environmentName", environmentName, queryParameters, queryText);
        setVariableIfPopulated("applicationId", applicationId, queryParameters, queryText);
        setVariableIfPopulated("name", serviceName, queryParameters, queryText);
        setVariableIfPopulated("instanceNumber", instanceNumber, queryParameters, queryText);

        queryText << "group by m.timestamp order by m.timestamp"
        Query query = em.createQuery(queryText.toString());
        queryParameters.each { k, v -> query.setParameter(k, v)}

        List result = query.getResultList();

        return result.collect { values ->
            return [
                    timestamp:  values[0],
                    min:        values[1],
                    max:        values[2],
                    avg:        values[3],
                    sum:        values[4],
                    count:      values[5]
            ]
        }
    }

    private void setVariableIfPopulated(String variableName, Object variableValue, Map<String, ?> queryParameters, StringBuilder queryText) {
        if (variableValue) {
            queryText << "and m.serviceInstanceThumbnail.$variableName = :$variableName "
            queryParameters.put(variableName, variableValue);
        }
    }

}
