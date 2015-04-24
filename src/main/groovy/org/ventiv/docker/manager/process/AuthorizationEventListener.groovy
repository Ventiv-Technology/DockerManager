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
package org.ventiv.docker.manager.process

import org.activiti.engine.delegate.event.ActivitiEvent
import org.activiti.engine.delegate.event.ActivitiEventListener
import org.activiti.engine.delegate.event.ActivitiEventType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.ventiv.docker.manager.controller.ProcessController

/**
 * Listens for Activity start events, and sets the Authentication Object (from a variable inject in ProcessController) back in
 * the context (if it's not present already)
 */
@Component
class AuthorizationEventListener implements ActivitiEventListener {

    @Override
    void onEvent(ActivitiEvent event) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            if (event.getType() == ActivitiEventType.ACTIVITY_STARTED) {
                Authentication authentication = (Authentication) event.getEngineServices().getRuntimeService().getVariable(event.getExecutionId(), ProcessController.INITIATOR_AUTHENTICATION_OBJECT_VARIABLE_KEY)
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
    }

    @Override
    boolean isFailOnException() {
        return false
    }
}
