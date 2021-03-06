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

import org.ventiv.docker.manager.model.EnvironmentProperty

/**
 * Created by jcrygier on 3/3/15.
 */
class EnvironmentConfiguration {

    String tierName;
    String id;
    String description;
    List<ServerConfiguration> servers;
    List<ApplicationConfiguration> applications;
    Collection<EnvironmentProperty> properties = [];

    void setApplications(List<ApplicationConfiguration> applications) {
        applications.each {
            it.setTierName(tierName);
            it.setEnvironmentId(id);
        }

        this.applications = applications;
    }

    void setTierName(String value) {
        this.tierName = value;
        applications?.each { it.setTierName(value) }
    }

    void setId(String value) {
        this.id = value;
        applications?.each { it.setEnvironmentId(value) }
    }
}
