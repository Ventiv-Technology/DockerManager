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
package org.ventiv.docker.manager.model.configuration

import javax.annotation.Nullable
import javax.validation.constraints.NotNull

/**
 * Configuration for retrieving additional metrics for a service instance.
 */
class AdditionalMetricsConfiguration {

    /**
     * The type of this additional metric.  Should be the name of a spring bean.
     */
    @NotNull
    String type;

    /**
     * Name of this metric
     */
    @NotNull
    String name;

    /**
     * User Interface configuration for this additional metric.  The user interface will always be a button for overview
     * and modal for details.
     */
    @Nullable
    AdditionalMetricsUiConfiguration ui;

    /**
     * Settings for this particular type.
     */
    @Nullable
    Map<String, String> settings;

    /**
     * Key value pair that should be stored in the Additional Metrics Data storage.  The key here is the key that will be
     * stored in Additional Metrics Data Storage, and the value is a SimpleTemplateService template operating on 'data'
     * as the object returned from this particular type of Additional Metrics.
     */
    @Nullable
    Map<String, String> storage;

    /**
     * Label Key value pair for additional metrics that are stored.  Used in the UI
     */
    @Nullable
    Map<String, String> storageDescriptions;

}
