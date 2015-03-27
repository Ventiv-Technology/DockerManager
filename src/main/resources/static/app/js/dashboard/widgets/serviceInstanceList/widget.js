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

define(['angular'], function () {

    angular.module('myApp.dashbaord.widget.serviceInstanceList', ['adf.provider'])

        .config(function (dashboardProvider) {
            dashboardProvider
                .widget('serviceInstanceList', {
                    title: 'Service Instance List',
                    description: 'Displays list of service instances running on a host',
                    controller: 'ServiceInstanceListController',
                    templateUrl: 'app/js/dashboard/widgets/serviceInstanceList/widget.html',
                    resolve: {
                        hostData: function(HostsService, config) {
                            return HostsService.get();
                        }
                    },
                    edit: {
                        templateUrl: 'app/js/dashboard/widgets/serviceInstanceList/edit.html',
                        reload: false,
                        controller: 'ServiceInstanceListEditController'
                    }
                });
        })

        .controller('ServiceInstanceListController', function ($scope, $modal, config, hostData) {
            $scope.hostData = hostData.data;
            config.hostOptions = _.map($scope.hostData.hostDetails, function(host) { return { id: host.id, description: host.description} });

            var selectHost = function(hostId) {
                $scope.host = _.find($scope.hostData.hostDetails, function(host) { return host.id == hostId });
                $scope.model.title = $scope.host.description + " (" + $scope.host.hostname + ")";
            };

            $scope.$watch('config.hostId', selectHost);

            $scope.serviceInstanceDetails = function(serviceInstance) {
                var serviceInstanceDetailsModal = $modal.open({
                    templateUrl: '/app/partials/serviceInstanceDetails.html',
                    controller: 'ServiceInstanceDetailsController',
                    windowClass: 'service-instance-details',
                    size: 'lg',
                    resolve: {
                        serviceInstance: function() { return serviceInstance }
                    }
                });
            };
        })

        .controller('ServiceInstanceListEditController', function($scope) {
            $scope.$watch('host', function(newValue) {
                if (newValue)
                    $scope.config.hostId = newValue.id;
            });
        })

});