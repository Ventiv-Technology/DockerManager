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

    angular.module('myApp.dashbaord.widget.linklist', ['adf.provider'])

        .config(function (dashboardProvider) {
            dashboardProvider
                .widget('linklist', {
                    title: 'Links',
                    description: 'Displays a list of links',
                    controller: 'linklistCtrl',
                    controllerAs: 'list',
                    templateUrl: 'app/js/dashboard/widgets/linklist/widget.html',
                    edit: {
                        templateUrl: 'app/js/dashboard/widgets/linklist/edit.html',
                        reload: false,
                        controller: 'linklistEditCtrl'
                    }
                });
        })

        .controller('linklistCtrl', function ($scope, config) {
            if (!config.links) {
                config.links = [];
            }
            this.links = config.links;
        })

        .controller('linklistEditCtrl', function ($scope) {
            function getLinks() {
                if (!$scope.config.links) {
                    $scope.config.links = [];
                }
                return $scope.config.links;
            }

            $scope.addLink = function () {
                getLinks().push({});
            };
            $scope.removeLink = function (index) {
                getLinks().splice(index, 1);
            };
        });
});