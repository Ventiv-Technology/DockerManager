/**
 * Copyright (c) 2014 - 2016 Ventiv Technology
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
package org.ventiv.docker.manager.metrics.store;

import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.data.influxdb.InfluxDBTemplate;
import org.ventiv.docker.manager.model.ServiceInstance;
import org.ventiv.docker.manager.model.metrics.AdditionalMetricsStorage;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Additional Metrics Store backed by InfluxDB (A Time Series Database)
 */
public class InfluxDbAdditionalMetricsStore extends AbstractAdditionalMetricsStore {

    private InfluxDBTemplate<Point> template;

    public InfluxDbAdditionalMetricsStore(InfluxDBTemplate<Point> template) {
        this.template = template;
    }

    @Override
    public List<AdditionalMetricsStorage> getAdditionalMetricsBetween(ServiceInstance serviceInstance, Long startTime, Long endTime) {
        StringBuilder queryStr = new StringBuilder("select time, value from /.*/ where ");

        if (startTime != null)
            queryStr.append("time > ").append(startTime).append("ms AND");

        if (endTime != null)
            queryStr.append("time < ").append(endTime).append("ms AND");

        queryStr
                .append("tierName = '").append(serviceInstance.getTierName()).append("' ")
                .append("AND environmentName = '").append(serviceInstance.getEnvironmentName()).append("' ")
                .append("AND applicationId = '").append(serviceInstance.getApplicationId()).append("' ")
                .append("AND \"name\" = '").append(serviceInstance.getName()).append("' ")
                .append("AND instanceNumber = '").append(serviceInstance.getInstanceNumber()).append("' ");

        Query q = new Query(queryStr.toString(), template.getDatabase());
        QueryResult result = template.query(q, TimeUnit.MILLISECONDS);

        Map<Date, AdditionalMetricsStorage> additionalMetrics = new HashMap<>();
        for (QueryResult.Result queryResult : result.getResults()) {
            for (QueryResult.Series series : queryResult.getSeries()) {
                String additionalMetricName = series.getName();

                for (List<Object> values : series.getValues()) {
                    Date time = new Date(((Number)values.get(0)).longValue());
                    BigDecimal value = new BigDecimal((Double) values.get(1));

                    AdditionalMetricsStorage additionalMetric = additionalMetrics.get(time);
                    if (additionalMetric == null) {
                        additionalMetric = new AdditionalMetricsStorage();
                        additionalMetric.setId(time.getTime());
                        additionalMetric.setTimestamp(time.getTime());
                        additionalMetric.setAdditionalMetrics(new HashMap<String, BigDecimal>());
                        additionalMetrics.put(time, additionalMetric);
                    }

                    additionalMetric.getAdditionalMetrics().put(additionalMetricName, value);
                }
            }
        }

        return new ArrayList<>(additionalMetrics.values());
    }

    @Override
    public void storeAdditionalMetrics(ServiceInstance serviceInstance, AdditionalMetricsStorage additionalMetricsStorage) {
        List<Point> points = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> e : additionalMetricsStorage.getAdditionalMetrics().entrySet()) {
            Point.Builder p = Point
                    .measurement(e.getKey())
                    .time(additionalMetricsStorage.getTimestamp(), TimeUnit.MILLISECONDS)
                    .field("value", e.getValue());

            if (serviceInstance.getTierName() != null)
                p.tag("tierName", serviceInstance.getTierName());

            if (serviceInstance.getEnvironmentName() != null)
                p.tag("environmentName", serviceInstance.getEnvironmentName());

            if (serviceInstance.getEnvironmentDescription() != null)
                p.tag("environmentDescription", serviceInstance.getEnvironmentDescription());

            if (serviceInstance.getApplicationId() != null)
                p.tag("applicationId", serviceInstance.getApplicationId());

            if (serviceInstance.getApplicationDescription() != null)
                p.tag("applicationDescription", serviceInstance.getApplicationDescription());

            if (serviceInstance.getName() != null)
                p.tag("name", serviceInstance.getName());

            if (serviceInstance.getServiceDescription() != null)
                p.tag("serviceDescription", serviceInstance.getServiceDescription());

            if (serviceInstance.getServerName() != null)
                p.tag("serverName", serviceInstance.getServerName());

            if (serviceInstance.getInstanceNumber() != null)
                p.tag("instanceNumber", serviceInstance.getInstanceNumber().toString());

            if (serviceInstance.getContainerId() != null)
                p.tag("containerId", serviceInstance.getContainerId());

            if (serviceInstance.getContainerImage() != null && serviceInstance.getContainerImage().getRegistry() != null)
                p.tag("image.registry", serviceInstance.getContainerImage().getRegistry());

            if (serviceInstance.getContainerImage() != null && serviceInstance.getContainerImage().getNamespace() != null)
                p.tag("image.namespace", serviceInstance.getContainerImage().getNamespace());

            if (serviceInstance.getContainerImage() != null && serviceInstance.getContainerImage().getRepository() != null)
                p.tag("image.repository", serviceInstance.getContainerImage().getRepository());

            if (serviceInstance.getContainerImage() != null && serviceInstance.getContainerImage().getTag() != null)
                p.tag("image.tag", serviceInstance.getContainerImage().getTag());

            points.add(p.build());
        }

        if (points.size() > 0)
            template.write(points);
    }

    @Override
    public List<Map<String, Object>> getTimeSeries(String metricName, String serverName, String tierName, String environmentName, String applicationId, String serviceName, Integer instanceNumber, Long fromTimestamp, Long toTimestamp, String last, String groupTimeWindow) {
        StringBuilder queryString = new StringBuilder("select min(value), max(value), mean(value), sum(value), count(value) from ").append('"').append(metricName).append('"').append(" WHERE ");

        if (serverName != null)
            queryString.append("serverName = '").append(serverName).append("' AND ");

        if (tierName != null)
            queryString.append("tierName = '").append(tierName).append("' AND ");

        if (environmentName != null)
            queryString.append("environmentName = '").append(environmentName).append("' AND ");

        if (applicationId != null)
            queryString.append("applicationId = '").append(applicationId).append("' AND ");

        if (serviceName != null)
            queryString.append("serviceName = '").append(serviceName).append("' AND ");

        if (instanceNumber != null)
            queryString.append("instanceNumber = '").append(instanceNumber).append("' AND ");

        // If 'Last' is populated, take it over 'fromTimestamp' / 'toTimeStamp'
        if (last != null)
            queryString.append("time > now() - ").append(groovyTimeCategoryToInfluxTime(last)).append(" ");
        else if (fromTimestamp != null && toTimestamp != null)
            queryString.append("time > ").append(fromTimestamp).append("ms AND time < ").append(toTimestamp).append("ms ");
        else if (fromTimestamp != null)
            queryString.append("time > ").append(fromTimestamp).append("ms ");

        queryString.append("group by time(").append(groovyTimeCategoryToInfluxTime(groupTimeWindow)).append(")");

        Query q = new Query(queryString.toString(), template.getDatabase());
        QueryResult result = template.query(q, TimeUnit.MILLISECONDS);

        List<Map<String, Object>> answer = new ArrayList<>();
        for (QueryResult.Result queryResult : result.getResults()) {
            for (QueryResult.Series series : queryResult.getSeries()) {
                for (List<Object> values : series.getValues()) {
                    Map<String, Object> point = new HashMap<>();
                    answer.add(point);

                    point.put("timestamp", ((Number)values.get(0)).longValue());
                    point.put("min", values.get(1));
                    point.put("max", values.get(2));
                    point.put("avg", values.get(3));
                    point.put("sum", values.get(4));
                    point.put("count", values.get(5));
                }
            }
        }

        return answer;
    }

    private String groovyTimeCategoryToInfluxTime(String groovyTimeCategory) {
        assert !groovyTimeCategory.contains("years");
        assert !groovyTimeCategory.contains("months");

        return groovyTimeCategory
                .replaceAll("weeks", "w")
                .replaceAll("days", "d")
                .replaceAll("hours", "h")
                .replaceAll("minutes", "m")
                .replaceAll("seconds", "s")
                .replaceAll("millis", "ms")
                .replaceAll("micros", "u")
                .replaceAll("\\.", "");
    }

}
