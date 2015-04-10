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

define([
    "dashboard/widgets/linklist/widget",
    "dashboard/widgets/connectedhosts/widget",
    "dashboard/widgets/serviceInstanceStatus/widget",
    "dashboard/widgets/containersCreated/widget",
    "dashboard/widgets/serviceInstanceList/widget",
    "dashboard/widgets/serviceStatus/widget",
    "dashboard/widgets/availableServiceList/widget",
    "dashboard/widgets/timeSeriesMetrics/widget"
], function () {

    angular.module('myApp.dashbaord.widgets', [
        'myApp.dashbaord.widget.linklist',
        'myApp.dashbaord.widget.connectedhosts',
        'myApp.dashbaord.widget.serviceInstanceStatus',
        'myApp.dashbaord.widget.containersCreated',
        'myApp.dashbaord.widget.serviceInstanceList',
        'myApp.dashbaord.widget.serviceStatus',
        'myApp.dashbaord.widget.availableServiceList',
        'myApp.dashbaord.widget.timeSeriesMetrics'
    ])

});