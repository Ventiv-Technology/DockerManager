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

define(['jquery', 'angular', 'translations-en', 'ui-bootstrap-tpls', 'restangular', 'angular-translate', 'angular-ui-router', 'bootstrap', 'angular-busy', 'statusService', 'select2', 'dashboard/dashboard', 'controller/imageController'], function ($, angular, translations) {

    // Declare app level module which depends on filters, and services

    return angular.module('myApp', ['ui.bootstrap', 'restangular', 'pascalprecht.translate', 'ui.router', 'cgBusy', 'myApp.statusService', 'myApp.dashbaord', 'myApp.image'])
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
                    templateUrl: '/app/js/dashboard/dashboard.html',
                    controller: 'DashboardController'
                })
                .state('environment', {
                    url: '/env/{tierName}/{environmentId}',
                    templateUrl: '/app/partials/environments.html',
                    controller: 'EnvironmentController'
                })
                .state('application', {
                    url: '/env/{tierName}/{environmentId}/{applicationId}',
                    templateUrl: '/app/partials/environments.html',
                    controller: 'EnvironmentController'
                })
                .state('images', {
                    url: '/images/{hostName}',
                    templateUrl: '/app/partials/images.html',
                    controller: 'ImageController'
                })
                .state('imageDetails', {
                    url: '/images/{hostName}/{imageName}',
                    templateUrl: '/app/partials/imageDetails.html',
                    controller: 'ImageDetailsController'
                });
        })

        .controller('MainController', function($scope, $stateParams, Restangular, $http, $modal) {
            $scope.asyncExecutionPromise = Restangular.one('environment').get().then(function(environments) {
                $scope.tiers = environments.plain();

                $scope.environments = [];
                _.forOwn($scope.tiers, function(environmentList, tierName) {
                    _.each(environmentList, function(environment) {
                        $scope.environments.push(environment);
                    });
                });
            });

            $http.get("/health")
                .success(function(data) {
                    $scope.$root.userDetails = data.user.user;
                }).error(function(data) {
                    $scope.$root.userDetails = data.user.user;
                });

            $scope.isPermissionGranted = function(applicationDetails, desiredPermission) {
                var grantingPermissionIdx = _.findIndex($scope.userDetails.effectivePermissions, function(permission) {
                    return permission.tierName == applicationDetails.tierName && permission.environmentId == applicationDetails.environmentName && permission.applicationId == applicationDetails.id && permission.grantedPermission == desiredPermission;
                });

                return grantingPermissionIdx !== -1;
            };

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
                        effectivePermissions: function() { return [] },
                        serviceInstance: function() { return serviceInstance }
                    }
                });

                serviceInstanceDetailsModal.result.then(function () {

                });
            };

            $scope.additionalMetricsDetails = function(application, serviceInstance, metricName, metricValue) {
                if ($scope.isPermissionGranted(application, 'METRICS_DETAILS')) {
                    var additionalMetricsDetailsModal = $modal.open({
                        templateUrl: '/api/service/' + serviceInstance.name + '/metrics/' + metricName + '/details',
                        controller: 'AdditionalMetricsDetailsController',
                        windowClass: 'additional-metrics-details',
                        size: 'lg',
                        resolve: {
                            serviceInstance: function () { return serviceInstance },
                            data: function () { return metricValue }
                        }
                    });

                    additionalMetricsDetailsModal.result.then(function () {

                    });
                }
            };

            $scope.refreshDockerState = function() {
                $http.post("/api/service/refresh")
                    .success(function() {
                        alert("Successful Refresh")
                    })
                    .error(function(data) {
                        alert("Error Refreshing: " + data);
                    })
            }
        })

        .controller('EnvironmentController', function($scope, $stateParams, $modal, Restangular, $http, StatusService) {
            $scope.workflowProcesses = [];

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

                $http.get("/api/process/" + tierName + "/" + environmentId)
                    .success(function(data) {
                        $scope.workflowProcesses = data;
                    });

                $scope.startProcess = function(workflowProcess) {
                    if (workflowProcess.startForm) {
                        var startFormModal = $modal.open({
                            templateUrl: '/api/process/startForm/' + workflowProcess.id,
                            controller: 'ProcessFormController',
                            windowClass: 'process-form',
                            size: 'lg',
                            resolve: {
                                formDetails: function() { return $http.get('/api/process/startFormVariables/' + workflowProcess.id) },
                                tierName: function() { return tierName },
                                environmentId: function() { return environmentId },
                                workflowProcess: function() { return workflowProcess }
                            }
                        });
                    } else {
                        $http.post("/api/process/" + tierName + "/" + environmentId + "/" + workflowProcess.key)
                    }
                };

                return $scope.asyncExecutionPromise;
            };

            $scope.deployApplication = function(applicationDetails) {
                console.log("Deploying build:", applicationDetails.selectedVersion);
                $scope.asyncExecutionPromise = Restangular.one('environment', $stateParams.tierName).one($stateParams.environmentId).one("app", applicationDetails.id).all(applicationDetails.selectedVersion).post()
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

            $scope.serviceInstanceButtonSizeClass = function(application) {
                if (!$scope.isPermissionGranted(application, 'METRICS_OVERVIEW'))
                    return "col-md-12";

                // Get number of metrics for each instance, then get the max
                var metricsSize = _.max(_.map(application.serviceInstances, function(instance) { return _.keys(instance.additionalMetrics).length; }));
                return "col-md-" + (12 - (metricsSize * 2));
            };

            $scope.serviceInstanceButtonSizeClassByInstance = function(serviceInstance) {
                var metricsSize = _.keys(serviceInstance.additionalMetrics).length;
                return "col-md-" + (12 - (metricsSize * 2));
            };

            $scope.showUserAudit = function(application) {
                var userAuditDetailsModal = $modal.open({
                    templateUrl: '/app/partials/applicationHistory.html',
                    controller: 'ApplicationHistoryController',
                    windowClass: 'application-history-details',
                    size: 'lg',
                    resolve: {
                        userAuditDetails: function() { return $http.get("/userAudits/search/findUserAuditsForApplication" +
                                    "?tierName=" + application.tierName +
                                    "&environmentName=" + application.environmentName +
                                    "&applicationId=" + application.id +
                                    "&sort=permissionEvaluated,desc")
                        }
                    }
                });
            };

            $scope.getImageUrl = function(workflowProcess) {
                if (workflowProcess)
                    return "/activiti/processes/" + workflowProcess.key;
                else
                    return "#"
            };

            $scope.$watch("environment.applications", function(applications) {
                if (applications && $stateParams.applicationId) {
                    var application = _.find(applications, function(application) {
                        return application.id == $stateParams.applicationId;
                    });

                    if (application)
                        application.opened = true;
                }
            }, true)
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

            $scope.isServiceInstancePermissionGranted = function(desiredPermission) {
                if (serviceInstance.tierName == null && serviceInstance.environmentName == null && serviceInstance.applicationId == null)
                    return true;

                var grantingPermissionIdx = _.findIndex($scope.userDetails.effectivePermissions, function(permission) {
                    return permission.tierName == serviceInstance.tierName && permission.environmentId == serviceInstance.environmentName && permission.applicationId == serviceInstance.applicationId && permission.grantedPermission == desiredPermission;
                });

                return grantingPermissionIdx !== -1;
            };

            if ($scope.isServiceInstancePermissionGranted('READ_USER_AUDIT')) {
                $http.get("/userAudits/search/findUserAuditsForServiceInstance?serverName=" + serviceInstance.serverName +
                          "&tierName=" + serviceInstance.tierName +
                          "&environmentName=" + serviceInstance.environmentName +
                          "&applicationId=" + serviceInstance.applicationId +
                          "&name=" + serviceInstance.name +
                          "&instanceNumber=" + serviceInstance.instanceNumber +
                          "&sort=permissionEvaluated,desc").then(function(data) {
                    $scope.auditHistory = data.data;
                });
            }
        })

        .controller('AdditionalMetricsDetailsController', function($scope, $modalInstance, serviceInstance, data) {
            $scope.serviceInstance = serviceInstance;
            $scope.data = data;

            $scope.dismiss = function(dismissObj) {
                $modalInstance.dismiss(dismissObj);
            };
        })

        .controller('ApplicationHistoryController', function($scope, $modalInstance, userAuditDetails) {
            $scope.auditHistory = userAuditDetails.data;

            $scope.dismiss = function(dismissObj) {
                $modalInstance.dismiss(dismissObj);
            };
        })

        .controller('ProcessFormController', function($scope, $http, $modalInstance, tierName, environmentId, workflowProcess, formDetails) {
            $scope.formDetails = formDetails.data;
            $scope.formVariables = {};

            $scope.start = function() {
                var formToSubmit = {};
                _.each($scope.formDetails, function(formVariable) {
                    formToSubmit[formVariable.id] = formVariable.value;
                });

                $modalInstance.dismiss(
                    $http.post("/api/process/" + tierName + "/" + environmentId + "/" + workflowProcess.key, formToSubmit)
                );

            };

            $scope.dismiss = function(dismissObj) {
                $modalInstance.dismiss(dismissObj);
            };
        })

        .directive('versionSelection', function() {
            return {
                restrict: 'C',
                link: function (scope, element, attrs, controller) {
                    var application = scope[attrs.application];
                    var url = "/api/environment/" + application.tierName + "/" + application.environmentName + "/app/" + application.id + "/versions";
                    var el = $(element);

                    el.select2({
                        ajax: {
                            url: url,
                            dataType: 'json',
                            delay: 250,
                            processResults: function (data, page) {
                                return {
                                    results: data
                                };
                            },
                            cache: true
                        }
                    });

                    el.on('select2:select', function(e) {
                        application.selectedVersion = e.params.data.id;
                    });
                }
            };
        })
});