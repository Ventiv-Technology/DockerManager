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

define(['jquery', 'angular', 'translations-en', 'ui-bootstrap-tpls', 'restangular', 'angular-translate', 'angular-ui-router', 'bootstrap', 'angular-busy', 'statusService'], function ($, angular, translations) {

    // Declare app level module which depends on filters, and services

    return angular.module('myApp', ['ui.bootstrap', 'restangular', 'pascalprecht.translate', 'ui.router', 'cgBusy', 'myApp.statusService'])
        .config(function (RestangularProvider, $translateProvider, $stateProvider, $urlRouterProvider) {
            // Configure RESTAngular
            RestangularProvider.setBaseUrl("/api");
            RestangularProvider.setResponseExtractor(function (response, operation, what) {
                if (response['_embedded'])
                    return response['_embedded'][what];
                else
                    return response;
            });

            // Configure Translations
            $translateProvider.translations('en', translations).preferredLanguage('en');

            // Configure UI-Router
            $urlRouterProvider.otherwise('/');
            $stateProvider
                .state('landing', {
                    url: '/',
                    templateUrl: '/app/partials/landing.html'
                })
                .state('hosts', {
                    url: '/hosts',
                    templateUrl: '/app/partials/hosts.html',
                    controller: 'HostsController'
                })
                .state('environment', {
                    url: '/env/{tierName}/{environmentId}',
                    templateUrl: '/app/partials/environments.html',
                    controller: 'EnvironmentController'
                });
        })

        .controller('MainController', function($scope, $stateParams, Restangular, $http, $modal) {
            $scope.asyncExecutionPromise = Restangular.one('environment').get().then(function(environments) {
                $scope.tiers = environments.plain();

                $scope.environments = [];
                _.forOwn($scope.tiers, function(environmentList, tierName) {
                    _.each(environmentList, function(environment) {
                        environment.tierName = tierName;
                        $scope.environments.push(environment);
                    });
                });
            });

            $http.get("/health").then(function(response) {
                $scope.userDetails = response.data.user.user;
            });

            $scope.isMultipleTiers = function() {
                if ($scope.tiers)
                    return Object.keys($scope.tiers).length > 1;
                else
                    return false;
            };

            $scope.getTierClass = function(tierName) {
                if ($stateParams.tierName == tierName)
                    return "active";
                else
                    return "inactive";
            };

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

                serviceInstanceDetailsModal.result.then(function () {

                });
            };
        })

        .controller('HostsController', function($scope, Restangular, StatusService) {
            var hostsInterface = Restangular.all('hosts');
            $scope.hosts = hostsInterface.getList().$object;

            // Listen to Start / Stop events
            var serviceInstanceStatusChangeCallback = function(eventObject) {
                var eventServiceInstance = eventObject.serviceInstance;
                var allExistingServiceInstances = _.compact(_.flatten(_.pluck($scope.hosts, 'serviceInstances')));
                var existingServiceInstance = _.find(allExistingServiceInstances, function(si) { return si.containerId == eventServiceInstance.containerId });

                if (existingServiceInstance) {
                    eventServiceInstance.containerImage = existingServiceInstance.containerImage;
                    angular.extend(existingServiceInstance, eventServiceInstance);
                }

                $scope.$digest();
            };

            StatusService.subscribe("ContainerStoppedEvent", serviceInstanceStatusChangeCallback);
            StatusService.subscribe("ContainerStartedEvent", serviceInstanceStatusChangeCallback);
        })

        .controller('EnvironmentController', function($scope, $stateParams, $modal, Restangular, $http, StatusService) {
            $scope.$watch("tiers", function(tiers) {
                if (tiers !== undefined) {
                    $scope.environment = _.find(tiers[$stateParams.tierName], function (environment) {
                        return environment.id == $stateParams.environmentId;
                    });

                    $scope.refreshEnvironment($stateParams.tierName, $stateParams.environmentId);
                }
            });

            $scope.refreshEnvironment = function(tierName, environmentId) {
                $scope.asyncExecutionPromise = Restangular.one('environment', tierName).getList(environmentId);
                $scope.environment.applications = $scope.asyncExecutionPromise.$object;

                // Listen to Build Status events, and update our copy
                StatusService.subscribeForApplication("BuildStatusEvent", $scope.environment.applications, function(application, eventObject, eventSource) {
                    application.buildStatus = eventSource;
                    $scope.$digest();
                });

                // Listen to Start / Stop events
                var serviceInstanceStatusChangeCallback = function(application, serviceInstance, eventObject, eventSource) {
                    serviceInstance.containerStatus = eventObject.serviceInstance.containerStatus;
                    serviceInstance.status = eventObject.serviceInstance.status;

                    $scope.$digest();
                };

                StatusService.subscribeForServiceInstance("ContainerStoppedEvent", $scope.environment.applications, serviceInstanceStatusChangeCallback);
                StatusService.subscribeForServiceInstance("ContainerStartedEvent", $scope.environment.applications, serviceInstanceStatusChangeCallback);
                StatusService.subscribeForServiceInstance("ContainerRemovedEvent", $scope.environment.applications, function(application, serviceInstance, eventObject, eventSource) {
                    _.remove(application.serviceInstances, serviceInstance);
                    application.missingServiceInstances.push({
                        availableVersions: null,                                        // TODO: Where to get this information?
                        serviceDescription: serviceInstance.serviceDescription,
                        serviceName: serviceInstance.name
                    });

                    $scope.$digest();
                });
                StatusService.subscribeForApplication("CreateContainerEvent", $scope.environment.applications, function(application, eventObject) {
                    var eventServiceInstance = eventObject.serviceInstance;
                    var idx = _.lastIndexOf(application.missingServiceInstances, function(missingService) { return missingService.serviceName == eventServiceInstance.name });
                    var removedMissingService = application.missingServiceInstances.splice(idx, 1);

                    application.serviceInstances.push(eventServiceInstance);
                    $scope.$digest();
                });

                StatusService.subscribeForApplication("DeploymentStartedEvent", $scope.environment.applications, function(application) {
                    application.deploymentInProgress = true;
                    $scope.$digest();
                });

                StatusService.subscribeForApplication("DeploymentFinishedEvent", $scope.environment.applications, function(application) {
                    application.deploymentInProgress = false;
                    $scope.$digest();
                });

                return $scope.asyncExecutionPromise;
            };

            $scope.deployApplication = function(applicationDetails) {
                var buildRequest = {
                    name: applicationDetails.id,
                    serviceVersions: applicationDetails.buildServiceVersionsTemplate
                };

                _.forIn(buildRequest.serviceVersions, function(value, key) {
                    if (!value) {
                        if (!applicationDetails.selectedVersion) {
                            alert("Please Select a Version from the Dropdown before Deploying");
                            throw "Please Select a Version from the Dropdown before Deploying";
                        }
                        buildRequest.serviceVersions[key] = applicationDetails.selectedVersion;
                    }
                });

                console.log("Deploying build request:", buildRequest);
                $scope.asyncExecutionPromise = Restangular.one('environment', $stateParams.tierName).all($stateParams.environmentId).post(buildRequest);
            };

            $scope.statusChangeApplication = function(applicationDetails, status) {
                $scope.asyncExecutionPromise = Restangular.one('environment', $stateParams.tierName).one($stateParams.environmentId).one("app", applicationDetails.id).all(status).post().then(
                    function success(response) {
                        applicationDetails.serviceInstances = response.serviceInstances;
                    },
                    function error(response) {
                        alert("Problems " + status + "ing Application...");
                        throw "Problems " + status + "ing Application...";
                    }
                );
            };

            $scope.buildApplication = function(applicationDetails) {
                $scope.asyncExecutionPromise = Restangular.one('environment', $stateParams.tierName).one($stateParams.environmentId).one("app", applicationDetails.id).all("buildApplication").post().then(
                    function success(response) {
                        // There actually is no response, you have to GET the status
                    }
                )
            };

            $scope.getRunning = function(application) {
                return _.filter(application.serviceInstances, function(serviceInstance) {
                    return serviceInstance.status == 'Running';
                });
            };

            $scope.getStopped = function(application) {
                return _.filter(application.serviceInstances, function(serviceInstance) {
                    return serviceInstance.status == 'Stopped';
                });
            };
        })

        .controller('ServiceInstanceDetailsController', function($scope, $modalInstance, serviceInstance, $window, $http) {
            var rootHostsUrl = "/api/hosts/" + serviceInstance.serverName + "/" + serviceInstance.containerId;
            $scope.serviceInstance = serviceInstance;

            $scope.getStdOut = function(tail) {
                if (tail === undefined)
                    tail = 0;

                $window.open(rootHostsUrl + "/stdout?tail=" + tail, '_blank');
            };

            $scope.getStdErr = function(tail) {
                if (tail === undefined)
                    tail = 0;

                $window.open(rootHostsUrl + "/stderr?tail=" + tail, '_blank');
            };

            $scope.cancel = function() {
                $modalInstance.dismiss('cancel');
            };

            $scope.postContainerOperation = function(operation) {
                $scope.asyncExecutionPromise = $http.post(rootHostsUrl + "/" + operation).then(
                    function success() {
                        // Do nothing, our WebSocket will be listening to service instance changes
                    },
                    function error() {
                        alert("Problems performing " + operation + " operation on container...");
                        throw "Problems performing " + operation + " operation on container...";
                    }
                );
            };
        })
});