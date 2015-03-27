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

    angular.module('myApp.dashbaord.widget.serviceStatus', ['adf.provider', "chart.js"])

        .config(function (dashboardProvider) {
            dashboardProvider
                .widget('serviceStatus', {
                    title: 'Service Status',
                    description: 'Shows services missing / available chart',
                    controller: 'ServiceStatusController',
                    templateUrl: 'app/js/dashboard/widgets/serviceStatus/widget.html',
                    resolve: {
                        hostData: function(HostsService, config) {
                            return HostsService.get();
                        }
                    }
                });
        })

        .controller('ServiceStatusController', function ($scope, config, hostData) {
            $scope.hostData = hostData.data;

            $scope.serviceChartConfig = {
                data: [0, $scope.hostData.missingServices.length],
                labels: ["Available", "Missing"],
                colors: ['#5BB75B', '#F0AD4E']
            };

            $scope.updateChart = function() {
                $scope.serviceChartConfig.data = [0, $scope.hostData.missingServices.length];

                _.forEach($scope.hostData.hostDetails, function(host) {
                    if (host.selected) {
                        $scope.serviceChartConfig.data[0] += host.availableServices.length;
                    }
                });
            };

            $scope.$watch('hostData.hostDetails', $scope.updateChart, true);
            $scope.updateChart();
        })
});