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

define(['jquery', 'angular'], function ($, angular) {

    // Declare app level module which depends on filters, and services

    return angular.module('myApp.image', [])
        .controller("ImageController", function ($scope, $stateParams, $http) {
            $scope.hostName = $stateParams.hostName;

            $scope.asyncExecutionPromise = $http.get("/api/image/" + $stateParams.hostName)
                .then(function(response) {
                    $scope.images = response.data;
                });
        })

        .controller("ImageDetailsController", function($scope, $stateParams, $http) {
            $scope.hostName = $stateParams.hostName;

            $scope.asyncExecutionPromise = $http.get("/api/image/" + $stateParams.hostName + "/" + $stateParams.imageName)
                .then(function(response) {
                    $scope.imageDetails = response.data;
                });

            $scope.showHistory = function(imageDetails) {
                $scope.fineGrainImageDetails = imageDetails;
                $scope.history = imageDetails.history;
                $scope.serviceInstances = null;
            };

            $scope.showServiceInstances = function(imageDetails) {
                $scope.fineGrainImageDetails = imageDetails;
                $scope.history = null;
                $scope.serviceInstances = imageDetails.serviceInstanceList;
            }
        })

        .filter('dockerSize', function() {
            return function(bytes, precision) {
                if (bytes == 0)
                    return "0 B";

                if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) return '-';
                if (typeof precision === 'undefined') precision = 1;
                var units = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'],
                    number = Math.floor(Math.log(bytes) / Math.log(1000));
                return (bytes / Math.pow(1000, Math.floor(number))).toFixed(precision) +  ' ' + units[number];
            }
        });;
});