'use strict';

define(['jquery', 'angular', 'translations-en', 'ui-bootstrap-tpls', 'restangular', 'angular-translate', 'angular-ui-router', 'bootstrap', 'angular-busy'], function ($, angular, translations) {

    // Declare app level module which depends on filters, and services

    return angular.module('myApp', ['ui.bootstrap', 'restangular', 'pascalprecht.translate', 'ui.router', 'cgBusy'])
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

        .controller('MainController', function($scope, $stateParams, Restangular) {
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
        })

        .controller('HostsController', function($scope, Restangular) {
            var hostsInterface = Restangular.all('hosts');
            $scope.hosts = hostsInterface.getList().$object;
        })

        .controller('EnvironmentController', function($scope, $stateParams, $modal, Restangular) {
            $scope.$watch("tiers", function(tiers) {
                if (tiers !== undefined) {
                    $scope.environment = _.find(tiers[$stateParams.tierName], function (environment) {
                        return environment.id == $stateParams.environmentId;
                    });

                    $scope.asyncExecutionPromise = Restangular.one('environment', $stateParams.tierName).getList($stateParams.environmentId);
                    $scope.environment.applications = $scope.asyncExecutionPromise.$object;
                }
            });

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
                $scope.asyncExecutionPromise = Restangular.one('environment', $stateParams.tierName).all($stateParams.environmentId).post(buildRequest).then(
                    function success(response) {
                        applicationDetails.serviceInstances = response.serviceInstances;
                        applicationDetails.missingServiceInstances = response.missingServiceInstances;
                        applicationDetails.version = response.version;
                        applicationDetails.url = response.url;
                    },
                    function error(response) {
                        alert("Error Building Environment: " + response.data.message);
                    }
                );
            };
        })

        .controller('ServiceInstanceDetailsController', function($scope, $modalInstance, serviceInstance, $window) {
            $scope.serviceInstance = serviceInstance;

            $scope.getStdOut = function(tail) {
                if (tail === undefined)
                    tail = 0;

                $window.open("/api/hosts/" + serviceInstance.serverName + "/" + serviceInstance.containerId + "/stdout?tail=" + tail, '_blank');
            };

            $scope.getStdErr = function(tail) {
                if (tail === undefined)
                    tail = 0;

                $window.open("/api/hosts/" + serviceInstance.serverName + "/" + serviceInstance.containerId + "/stderr?tail=" + tail, '_blank');
            };

            $scope.cancel = function() {
                $modalInstance.dismiss('cancel');
            }
        })
});