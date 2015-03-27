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

define(['angular-chart'], function () {

    angular.module('myApp.dashbaord.widget.serviceInstanceStatus', ['adf.provider', "chart.js"])

        .config(function (dashboardProvider) {
            dashboardProvider
                .widget('serviceInstanceStatus', {
                    title: 'Service Instance Status',
                    description: 'Displays Service Instances Running / Stopped',
                    controller: 'ServiceInstanceStatusController',
                    templateUrl: 'app/js/dashboard/widgets/serviceInstanceStatus/widget.html',
                    resolve: {
                        hostData: function(HostsService, config) {
                            return HostsService.get();
                        }
                    }
                });
        })

        .controller('ServiceInstanceStatusController', function ($scope, config, hostData) {
            $scope.hostData = hostData.data;

            $scope.serviceInstanceChartConfig = {
                data: [0,0,0],
                labels: ["Running", "Stopped", "Missing"],
                colors: ['#5BB75B', '#C7604C', '#F0AD4E']
            };

            $scope.updateChart = function() {
                $scope.serviceInstanceChartConfig.data = [0, 0, $scope.hostData.missingServices.length];
                $scope.containersCreated = {
                    data: [ [] ],
                    labels: []
                };

                _.forEach($scope.hostData.hostDetails, function(host) {
                    if (host.selected) {
                        _.forEach(host.serviceInstances, function (serviceInstance) {
                            if (serviceInstance.status === "Running")
                                $scope.serviceInstanceChartConfig.data[0] = $scope.serviceInstanceChartConfig.data[0] + 1;
                            else
                                $scope.serviceInstanceChartConfig.data[1] = $scope.serviceInstanceChartConfig.data[1] + 1;
                        });
                    }
                });
            };

            $scope.$watch('hostData.hostDetails', $scope.updateChart, true);
            $scope.updateChart();
        })
});