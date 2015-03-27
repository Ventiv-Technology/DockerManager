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

    angular.module('myApp.dashbaord.widget.containersCreated', ['adf.provider', "chart.js"])

        .config(function (dashboardProvider) {
            dashboardProvider
                .widget('containersCreated', {
                    title: 'Containers Created',
                    description: 'Shows when containers were created over time (Deployment Time)',
                    controller: 'ContainersCreatedController',
                    templateUrl: 'app/js/dashboard/widgets/containersCreated/widget.html',
                    resolve: {
                        hostData: function(HostsService, config) {
                            return HostsService.get();
                        }
                    }
                });
        })

        .controller('ContainersCreatedController', function ($scope, config, hostData) {
            $scope.hostData = hostData.data;

            $scope.containersCreated = {
                data: [ [] ],
                labels: []
            };

            $scope.updateChart = function() {
                $scope.containersCreated = {
                    data: [ [] ],
                    labels: []
                };

                var containerCreatedDateToCount = {};

                _.forEach($scope.hostData.hostDetails, function(host) {
                    if (host.selected) {
                        _.forEach(host.serviceInstances, function (serviceInstance) {
                            var createdDate = new Date(serviceInstance.containerCreatedDate).toLocaleDateString();
                            containerCreatedDateToCount[createdDate] = containerCreatedDateToCount[createdDate] === undefined ? 1 : containerCreatedDateToCount[createdDate] + 1;
                        });
                    }
                });

                var dteOrder = _.sortBy(_.keys(containerCreatedDateToCount), function(dte) {
                    return new Date(dte);
                });

                _.forEach(dteOrder, function(dte) {
                    var dateFormatted = new Date(dte).toLocaleDateString();
                    _.last($scope.containersCreated.data).push(containerCreatedDateToCount[dateFormatted]);
                    $scope.containersCreated.labels.push(dateFormatted);
                });
            };

            $scope.$watch('hostData.hostDetails', $scope.updateChart, true);
            $scope.updateChart();
        })
});