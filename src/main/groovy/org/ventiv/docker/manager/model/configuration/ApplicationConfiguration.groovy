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

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.model.ObjectIdentity
import org.ventiv.docker.manager.service.selection.ServiceSelectionAlgorithm

import javax.annotation.Nullable
import javax.validation.constraints.NotNull

/**
 * Data representing an Application's Configuration.  This differentiates from an ApplicationDetails object by being
 * a configuration as opposed to what is actually running.
 *
 * Generally configured via YAML.
 */
class ApplicationConfiguration {

    @NotNull
    String id;

    @NotNull
    String description;

    /**
     * Hardcoded URL.  Often used when there is an external load balancer to utilize multiple services.
     */
    @Nullable
    String url;

    /**
     * If this is populated, it will look for that particular ServiceInstance (by name) and copy it's url to this url.
     */
    @Nullable
    String serviceInstanceUrl;

    @Nullable
    Object loadBalance;     // TODO: Build out

    @Nullable
    Class<? extends ServiceSelectionAlgorithm> serviceSelectionAlgorithm;

    @NotNull
    List<ServiceInstanceConfiguration> serviceInstances;

    /**
     * Not null, but pushed here on object creation
     */
    @NotNull
    String tierName;

    /**
     * Not null, but pushed here on object creation
     */
    @NotNull
    String environmentId;

    @JsonIgnore
    public ObjectIdentity getObjectIdentity() {
        return new ObjectIdentityImpl(this.getClass(), "${tierName}.${environmentId}.${id}".toString())
    }

}
