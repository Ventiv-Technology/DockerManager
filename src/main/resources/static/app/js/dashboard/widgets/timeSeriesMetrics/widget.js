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
'use strict';

define(['angular-chart', 'translations-en', 'c3'], function (chart, translations, c3) {

    angular.module('myApp.dashbaord.widget.timeSeriesMetrics', ['adf.provider', "chart.js"])

        .config(function (dashboardProvider) {
            dashboardProvider
                .widget('timeSeriesMetrics', {
                    title: 'Time Series Metrics',
                    description: 'Shows stored metrics over time',
                    controller: 'TimeSeriesMetricsController',
                    templateUrl: 'app/js/dashboard/widgets/timeSeriesMetrics/widget.html',
                    resolve: {
                        hostData: function(HostsService, config) {
                            return HostsService.get();
                        },
                        timeSeriesData: function($http, config) {
                            config.uuid = Math.floor(Math.random() * 1000000);

                            if (config.metric) {
                                var url = "/api/metrics/timeseries/" + config.metric.metricName + "?dummy=dummy";

                                if (config.groupTimeWindow) url = url + "&groupTimeWindow=" + config.groupTimeWindow;
                                if (config.chartTimeFrame) url = url + "&last=" + config.chartTimeFrame;

                                return $http.get(url);
                            } else
                                return null;
                        }
                    },
                    edit: {
                        templateUrl: 'app/js/dashboard/widgets/timeSeriesMetrics/edit.html',
                        reload: true,
                        controller: 'TimeSeriesMetricsEditController'
                    }
                });
        })

        .controller('TimeSeriesMetricsController', function ($scope, $timeout, config, hostData, timeSeriesData) {
            if (timeSeriesData) {
                $scope.data = timeSeriesData.data;

                $scope.model.title = config.metric.serviceDescription + " " + config.metric.metricDescription;

                $scope.updateChart = function () {
                    var columns = [
                        ['x'],
                        [translations.min],
                        [translations.max],
                        [translations.avg],
                        [translations.sum],
                        [translations.count]
                    ];

                    _.forEach($scope.data, function (dataPoint) {
                        columns[0].push(new Date(dataPoint.timestamp));
                        columns[1].push(dataPoint.min);
                        columns[2].push(dataPoint.max);
                        columns[3].push(dataPoint.avg);
                        columns[4].push(dataPoint.sum);
                        columns[5].push(dataPoint.count);
                    });

                    var chart = c3.generate({
                        bindto: '#time-series-metrics-' + config.uuid,
                        data: {
                            x: 'x',
                            columns: columns,
                            type: config.chartType
                        },
                        axis: {
                            x: {
                                type: 'timeseries',
                                tick: {
                                    count: 3,
                                    format: '%Y-%m-%d %H:%M'
                                }
                            }
                        }/*,
                        legend: {
                            item: {
                                onclick: function(id) {
                                    // TODO: keep track of turning legend items on / off in the config
                                }
                            }
                        }*/
                    });
                };

                // Need to wait for digest to be done, so the uuid in the HTML gets populated
                $scope.$watch("config.uuid", function(uuid) {
                    if (uuid)
                        $timeout($scope.updateChart, 10);
                });
            }
        })

        .controller('TimeSeriesMetricsEditController', function($scope, $http) {
            $http.get("/api/metrics").then(function(data) {
                $scope.availableMetrics = data.data;
            })
        })
});