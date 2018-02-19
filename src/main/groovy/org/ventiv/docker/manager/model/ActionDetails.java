/**
 * Copyright (c) 2014 - 2018 Ventiv Technology
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
package org.ventiv.docker.manager.model;

import org.ventiv.docker.manager.security.DockerManagerPermission;

import java.util.function.Function;

public class ActionDetails {
    private final String actionId;
    private final String description;
    private final String responsePartial;
    private final DockerManagerPermission requiredPermission;
    private final Function<ServiceInstance, Boolean> isActionEnabled;
    private final Function<ServiceInstance, Object> performAction;

    /**
     * Constructs an ActionDetails instance, with all the information needed to actually execute the action.
     *
     * @param actionId Unique ID to invoke the action
     * @param description Text Description to provide to the UI
     * @param responsePartial HTML Partial (URL) used to display the results of the action to the UI
     * @param requiredPermission What permissions are needed to invoke this action
     * @param isActionEnabled Functional Callback to verify if this Action is enabled for this ServiceInstance.  Should be lightweight, as it may be called often.
     */
    public ActionDetails(String actionId, String description, String responsePartial, DockerManagerPermission requiredPermission, Function<ServiceInstance, Boolean> isActionEnabled, Function<ServiceInstance, Object> performAction) {
        this.actionId = actionId;
        this.description = description;
        this.responsePartial = responsePartial;
        this.requiredPermission = requiredPermission;
        this.isActionEnabled = isActionEnabled;
        this.performAction = performAction;
    }

    public String getActionId() {
        return actionId;
    }

    public String getDescription() {
        return description;
    }

    public String getResponsePartial() {
        return responsePartial;
    }

    public DockerManagerPermission getRequiredPermission() {
        return requiredPermission;
    }

    public boolean isActionEnabled(ServiceInstance serviceInstance) {
        return isActionEnabled.apply(serviceInstance);
    }

    public Object performAction(ServiceInstance serviceInstance) {
        return performAction.apply(serviceInstance);
    }
}
