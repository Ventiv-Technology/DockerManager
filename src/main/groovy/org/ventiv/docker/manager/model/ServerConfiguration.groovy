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

import javax.annotation.Nullable
import javax.validation.constraints.NotNull

/**
 * Created by jcrygier on 3/3/15.
 */
class ServerConfiguration {

    /**
     * Unique id for the host
     */
    @NotNull
    String id;

    /**
     * Name of the host to be shown in the UI.
     */
    @NotNull
    String description;

    /**
     * DNS Name of the host.
     */
    @NotNull
    String hostname;

    /**
     * Should we resolve this server to an IP and add it to /etc/hosts of any container created on this host?
     * This is mostly a workaround for boot2docker, since it cannot resolve to a real hostname.
     */
    @Nullable
    Boolean resolveHostname = false;

    /**
     * The list of services that are eligible to run on this host.
     */
    @NotNull
    List<EligibleServiceConfiguration> eligibleServices;

}
