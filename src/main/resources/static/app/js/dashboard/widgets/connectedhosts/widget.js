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

    angular.module('myApp.dashbaord.widget.connectedhosts', ['adf.provider'])

        .config(function (dashboardProvider) {
            dashboardProvider
                .widget('connectedhosts', {
                    title: 'Connected hosts',
                    description: 'Displays the list of connected hosts',
                    controller: 'ConnectedHostsController',
                    templateUrl: 'app/js/dashboard/widgets/connectedhosts/widget.html',
                    resolve: {
                        hostData: function(HostsService, config) {
                            return HostsService.get();
                        }
                    }
                });
        })

        .controller('ConnectedHostsController', function ($scope, config, hostData) {
            $scope.hostData = hostData.data;

            _.forEach($scope.hostData.hostDetails, function(host) {
                host.selected = true;
            });
        });
});