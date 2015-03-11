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
 * Created by jcrygier on 3/4/15.
 */
class ServiceInstanceConfiguration {

    /**
     * What type of service is this.  This will refer to an instance of ServiceConfiguration.
     */
    @NotNull
    String type;

    /**
     * How many instances of this service should be running for this application.
     */
    @NotNull
    Integer count;

    /**
     * Environment variables to set when constructing a ServiceInstance.  These variables can be thought of as specific to
     * a running application.  All environment variables may use variable replacement a la Groovy (e.g. ${application.id})
     * to get information about the running environment:
     *
     * {
     *      "application": ApplicationDetails
     *      "serviceInstances": {
     *          "<ServiceInstance.name>": {
     *              "server": ServiceInstance.serverName
     *              "port": {
     *                  "<portType>": <hostPort>
     *              }
     *          }
     *      }
     * }
     *
     * So, as an example, to refer to a the server name of a ServiceInstance named 'couch', you would do: serviceInstances.couch.server.
     * And to refer to it's HTTP port: serviceInstances.couch.port.http
     */
    @Nullable
    Map<String, String> environment;

    @Nullable
    List<VolumeMapping> volumeMappings;

}
