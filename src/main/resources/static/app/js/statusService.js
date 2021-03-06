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

define(['jquery', 'angular', 'stomp-websocket', 'sockjs-client'], function ($, angular) {

    /**
     * A service that will allow you to listen to Spring ApplicationEvent objects, by it's Class SimpleName.  For example
     * you can listen to 'AuthenticationSuccessEvent' and get those messages when they occur.  Takes in two parameters
     *
     * eventType: The name of the Application Event
     * callback: function(eventSource, eventMessage, stompData)
     */
    return angular.module('myApp.statusService', [])
        .service('StatusService', function($timeout) {

            var initialized = false;
            var subscriptions = {};

            var service = {
                RECONNECT_TIMEOUT: 30000,
                SOCKET_URL: "/api/status",

                subscribe: function(eventType, callback) {
                    subscriptions[eventType] = function(data) {
                        var message = JSON.parse(data.body);
                        message.timestamp = new Date(message.timestamp);

                        callback(message.event, message.event.source, message, data);
                    };

                    if (initialized) {
                        socket.stomp.subscribe("/topic/event/" + eventType, subscriptions[eventType]);
                    }
                },

                /**
                 * Will receive any event that extends AbstractApplicationEvent, and call the callback with the
                 * appropriate application object, found in the list of provided applications.
                 *
                 * @param eventType String name of the event type to listen to
                 * @param applications List of all applications, generally pulled from $scope
                 * @param callback Function to call with (application, eventObject, eventSource) on event and found application
                 */
                subscribeForApplication: function(eventType, applications, callback) {
                    this.subscribe(eventType, function(eventObject, eventSource, message, data) {
                        var foundApplication = _.find(applications, function(application) {
                            return  application.tierName == eventSource.tierName &&
                                    application.environmentName == eventSource.environmentName &&
                                    application.id == eventSource.applicationId

                        });

                        if (foundApplication) {
                            callback(foundApplication, eventObject, eventSource);
                        }
                    });
                },

                /**
                 * Will receive any event that extends AbstractApplicationEvent, and call the callback with the
                 * appropriate application and serviceInstance object, found in the list of provided applications.
                 *
                 * @param eventType String name of the event type to listen to
                 * @param applications List of all applications, generally pulled from $scope
                 * @param callback Function to call with (application, serviceInstance, eventObject, eventSource) on event and found application
                 */
                subscribeForServiceInstance: function(eventType, applications, callback) {
                    this.subscribeForApplication(eventType, applications, function(application, eventObject, eventSource) {
                        var serviceInstance = _.find(application.serviceInstances, function(serviceInstance) {
                            return serviceInstance.name == eventObject.serviceInstance.name &&
                                serviceInstance.instanceNumber == eventObject.serviceInstance.instanceNumber;
                        });

                        if (serviceInstance) {
                            callback(application, serviceInstance, eventObject, eventSource);
                        }
                    });
                }
            };

            var socket = {
                client: new SockJS(service.SOCKET_URL)
            };

            var onConnect = function() {
                _.forOwn(subscriptions, function(callback, eventType) {
                    socket.stomp.subscribe("/topic/event/" + eventType, callback);
                });

                initialized = true;
            };

            var reconnect = function() {
                initialized = false;
                $timeout(function() { initialize(); }, this.RECONNECT_TIMEOUT);
            };

            var initialize = function() {
                socket.stomp = Stomp.over(socket.client);
                socket.stomp.debug = false;
                socket.stomp.connect({}, onConnect);
                socket.stomp.onclose = reconnect;
            };

            initialize();
            return service;

        });

});