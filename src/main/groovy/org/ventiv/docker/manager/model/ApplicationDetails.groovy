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
package org.ventiv.docker.manager.model

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * The instantiated, running version of an ApplicationConfiguration.  Much of the information is copied from an
 * ApplicationConfiguration object.
 */
class ApplicationDetails {

    String id;
    String description;
    String url;
    String tierName;
    String environmentName;
    String version;
    Collection<String> availableVersions;
    Collection<ServiceInstance> serviceInstances;
    List<MissingService> missingServiceInstances;
    Map<String, String> buildServiceVersionsTemplate = [:];
    def buildStatus;

    @JsonIgnore
    ApplicationConfiguration applicationConfiguration;

    public ApplicationDetails withApplicationConfiguration(ApplicationConfiguration appConfig) {
        this.applicationConfiguration = appConfig;
        this.id = appConfig.getId();
        this.description = appConfig.getDescription();

        return this;
    }

}
