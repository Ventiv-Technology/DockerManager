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

import org.activiti.engine.delegate.DelegateExecution
import org.activiti.engine.delegate.JavaDelegate
import org.springframework.context.ApplicationContext
import org.springframework.security.core.Authentication
import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.controller.ProcessController
import org.ventiv.docker.manager.service.EnvironmentConfigurationService
import org.ventiv.docker.manager.service.ServiceInstanceService
import org.ventiv.docker.manager.service.SimpleTemplateService

/**
 * Base abstract class for custom Activiti tasks for Docker Manager
 */
abstract class AbstractDockerManagerTask implements JavaDelegate {

    protected ApplicationContext applicationContext = DockerManagerApplication.getApplicationContext();

    // Cached versions (or overriadable for testing)
    protected EnvironmentConfigurationService environmentConfigurationService;
    protected ServiceInstanceService serviceInstanceService;
    protected SimpleTemplateService simpleTemplateService;

    protected EnvironmentConfigurationService getEnvironmentConfigurationService() {
        if (environmentConfigurationService == null)
            environmentConfigurationService = applicationContext.getBean(EnvironmentConfigurationService);

        return environmentConfigurationService
    }

    protected ServiceInstanceService getServiceInstanceService() {
        if (serviceInstanceService == null)
            serviceInstanceService = applicationContext.getBean(ServiceInstanceService);

        return serviceInstanceService;
    }

    protected SimpleTemplateService getSimpleTemplateService() {
        if (simpleTemplateService == null)
            simpleTemplateService = applicationContext.getBean(SimpleTemplateService);

        return simpleTemplateService;
    }

    protected String getTierName(DelegateExecution execution) {
        execution.getVariable(ProcessController.TIER_NAME_VARIABLE_KEY);
    }

    protected String getEnvironmentId(DelegateExecution execution) {
        execution.getVariable(ProcessController.ENVIRONMENT_ID_VARIABLE_KEY);
    }

    protected Authentication getInitiatorAuthentication(DelegateExecution execution) {
        (Authentication) execution.getVariable(ProcessController.INITIATOR_AUTHENTICATION_OBJECT_VARIABLE_KEY);
    }

}
