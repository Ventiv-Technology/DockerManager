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

define(['angular', 'vendor/angular-dashboard-framework/0.7.0/angular-dashboard-framework.min', 'dashboard/widgets/widgets'], function () {

    angular.module('myApp.dashbaord', ['adf', 'myApp.dashbaord.widgets'])
        .controller("DashboardController", function($scope, HostsService) {
            var model = window.localStorage && window.localStorage.getItem('dashboard') ? JSON.parse(window.localStorage.getItem('dashboard')) : {
                title: 'Docker Manager Dashboard',
                structure: '4-12',
                rows: [{
                    columns: [{
                        styleClass: 'col-md-4',
                        widgets: [{
                            type: 'connectedhosts'
                        }]
                    },{
                        styleClass: 'col-md-4',
                        widgets: [{
                            type: 'serviceInstanceStatus'
                        }]
                    },{
                        styleClass: 'col-md-4',
                        widgets: [{
                            type: 'containersCreated'
                        }]
                    }]
                }]
            };

            // Automatically append Service Instance Details for each host, if model didn't come from localStorage
            if (!window.localStorage || !window.localStorage.getItem('dashboard')) {
                HostsService.get().then(function(hostData) {
                    _.forEach(hostData.data.hostDetails, function(host) {
                        model.rows.push({
                            columns: [{
                                styleClass: 'col-md-12',
                                widgets: [{
                                    type: 'serviceInstanceList',
                                    config: {
                                        hostId: host.id
                                    }
                                }]
                            }]
                        });
                    });
                });
            }

            $scope.dashboardSettings = {
                name: 'dashboard',
                model: model,
                collapsible: true
            };

            $scope.$on('adfDashboardChanged', function (event, name, model) {
                if (window.localStorage)
                    window.localStorage.setItem(name, JSON.stringify(model));
            });
        })

        .service('HostsService', function($q, $http, StatusService, $rootScope) {
            var cachedPromise;
            var retrievedData;

            // Listen for changes
            var serviceInstanceStatusChangeCallback = function(eventObject) {
                var eventServiceInstance = eventObject.serviceInstance;
                var allExistingServiceInstances = _.compact(_.flatten(_.pluck(retrievedData.hostDetails, 'serviceInstances')));
                var existingServiceInstance = _.find(allExistingServiceInstances, function(si) { return si.containerId == eventServiceInstance.containerId });

                if (existingServiceInstance) {
                    eventServiceInstance.containerImage = existingServiceInstance.containerImage;
                    angular.extend(existingServiceInstance, eventServiceInstance);
                }

                $rootScope.$digest();
            };

            StatusService.subscribe("ContainerStoppedEvent", serviceInstanceStatusChangeCallback);
            StatusService.subscribe("ContainerStartedEvent", serviceInstanceStatusChangeCallback);

            return {
                get: function() {
                    if (cachedPromise === undefined) {
                        cachedPromise = $http.get("/api/hosts");

                        cachedPromise.then(function(data) {
                            retrievedData = data.data;

                            // Default all hosts to be selected, just in case we don't have the "Connected Hosts" Dashboard up.
                            _.forEach(retrievedData.hostDetails, function(host) {
                                host.selected = true;
                            });
                        })
                    }

                    return cachedPromise;
                }
            }
        })

        .config(function(dashboardProvider){
            dashboardProvider
                .structure('6-6', {
                    rows: [{
                        columns: [{
                            styleClass: 'col-md-6'
                        }, {
                            styleClass: 'col-md-6'
                        }]
                    }]
                })
                .structure('4-8', {
                    rows: [{
                        columns: [{
                            styleClass: 'col-md-4',
                            widgets: []
                        }, {
                            styleClass: 'col-md-8',
                            widgets: []
                        }]
                    }]
                })
                .structure('12/4-4-4', {
                    rows: [{
                        columns: [{
                            styleClass: 'col-md-12'
                        }]
                    }, {
                        columns: [{
                            styleClass: 'col-md-4'
                        }, {
                            styleClass: 'col-md-4'
                        }, {
                            styleClass: 'col-md-4'
                        }]
                    }]
                })
                .structure('12/6-6', {
                    rows: [{
                        columns: [{
                            styleClass: 'col-md-12'
                        }]
                    }, {
                        columns: [{
                            styleClass: 'col-md-6'
                        }, {
                            styleClass: 'col-md-6'
                        }]
                    }]
                })
                .structure('12/6-6/12', {
                    rows: [{
                        columns: [{
                            styleClass: 'col-md-12'
                        }]
                    }, {
                        columns: [{
                            styleClass: 'col-md-6'
                        }, {
                            styleClass: 'col-md-6'
                        }]
                    }, {
                        columns: [{
                            styleClass: 'col-md-12'
                        }]
                    }]
                })
                .structure('3-9 (12/6-6)', {
                    rows: [{
                        columns: [{
                            styleClass: "col-md-3"
                        }, {
                            styleClass: "col-md-9",
                            rows: [{
                                columns: [{
                                    styleClass: "col-md-12"
                                }]
                            }, {
                                columns: [{
                                    styleClass: "col-md-6"
                                }, {
                                    styleClass: "col-md-6"
                                }]
                            }]
                        }]
                    }]
                });

        });

});