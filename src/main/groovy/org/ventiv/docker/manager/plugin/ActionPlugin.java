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
package org.ventiv.docker.manager.plugin;

import org.ventiv.docker.manager.model.ActionDetails;
import org.ventiv.docker.manager.model.ServiceInstance;

import java.util.List;

/**
 * Interface to extend functionality by performing any sort of action.  Actions are separate RESTful endpoints that can
 * be invoked from the UI, and return values.
 *
 * Each ActionPlugin may support multiple actions.
 */
public interface ActionPlugin {

    /**
     * Returns a map of actions that are supported by this plugin.
     *
     * @return Map.  Key = ID of the action.  Value = Description to provide to end user.
     */
    List<ActionDetails> getSupportedActions();

    /**
     * Performs the action.  Returns any object that the UI know's how to deal with (see: org.ventiv.docker.manager.model.ActionDetails#responsePartial)
     *
     * @param action
     * @param serviceInstance
     * @return
     * @throws Exception
     */
    Object performAction(ActionDetails action, ServiceInstance serviceInstance) throws Exception;

    /**
     * Lightweight test if this action is enabled for this Service Instance.
     *
     * @param action
     * @param serviceInstance
     * @return
     * @throws Exception
     */
    boolean isActionEnabled(ActionDetails action, ServiceInstance serviceInstance) throws Exception;

}
