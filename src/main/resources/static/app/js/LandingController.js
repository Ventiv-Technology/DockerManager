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

    // Declare app level module which depends on filters, and services

    return angular.module('myApp.landingController', ["chart.js"])
        .controller('LandingController', function($scope, Restangular, StatusService) {
            $scope.serviceInstanceChartConfig = {
                data: [0,0],
                labels: ["Running", "Stopped"],
                colors: ['#5BB75B', '#C7604C']
            };

            var hostsInterface = Restangular.all('hosts');
            $scope.asyncExecutionPromise = hostsInterface.getList().then(
                function(data) {
                    $scope.hosts = data;

                    // Mark each of them selected by default
                    _.forEach(data, function(host) {
                        host.selected = true;
                    });

                    // Refresh our chart
                    $scope.updateCharts();
                }
            );

            $scope.updateCharts = function() {
                $scope.serviceInstanceChartConfig.data = [0,0];
                $scope.containersCreated = {
                    data: [ [] ],
                    labels: []
                };

                var containerCreatedDateToCount = {};

                _.forEach($scope.hosts, function(host) {
                    if (host.selected) {
                        _.forEach(host.serviceInstances, function (serviceInstance) {
                            var createdDate = new Date(serviceInstance.containerCreatedDate).toLocaleDateString();
                            containerCreatedDateToCount[createdDate] = containerCreatedDateToCount[createdDate] === undefined ? 1 : containerCreatedDateToCount[createdDate] + 1;

                            if (serviceInstance.status === "Running")
                                $scope.serviceInstanceChartConfig.data[0] = $scope.serviceInstanceChartConfig.data[0] + 1;
                            else
                                $scope.serviceInstanceChartConfig.data[1] = $scope.serviceInstanceChartConfig.data[1] + 1;
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

            $scope.$watch('hosts', $scope.updateCharts, true);

            // Listen to Start / Stop events
            var serviceInstanceStatusChangeCallback = function(eventObject) {
                var eventServiceInstance = eventObject.serviceInstance;
                var allExistingServiceInstances = _.compact(_.flatten(_.pluck($scope.hosts, 'serviceInstances')));
                var existingServiceInstance = _.find(allExistingServiceInstances, function(si) { return si.containerId == eventServiceInstance.containerId });

                if (existingServiceInstance) {
                    eventServiceInstance.containerImage = existingServiceInstance.containerImage;
                    angular.extend(existingServiceInstance, eventServiceInstance);
                }

                $scope.updateCharts();
                $scope.$digest();
            };

            StatusService.subscribe("ContainerStoppedEvent", serviceInstanceStatusChangeCallback);
            StatusService.subscribe("ContainerStartedEvent", serviceInstanceStatusChangeCallback);
        })


});
