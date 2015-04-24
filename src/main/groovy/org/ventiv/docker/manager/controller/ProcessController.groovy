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
package org.ventiv.docker.manager.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.activiti.engine.IdentityService
import org.activiti.engine.RepositoryService
import org.activiti.engine.RuntimeService
import org.activiti.engine.TaskService
import org.activiti.engine.repository.ProcessDefinition
import org.activiti.engine.runtime.Execution
import org.activiti.engine.runtime.ProcessInstance
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.configuration.ApplicationConfiguration
import org.ventiv.docker.manager.model.configuration.EnvironmentConfiguration
import org.ventiv.docker.manager.model.configuration.ServiceInstanceConfiguration
import org.ventiv.docker.manager.security.SecurityUtil
import org.ventiv.docker.manager.service.EnvironmentConfigurationService
import org.ventiv.docker.manager.service.ServiceInstanceService

import javax.annotation.Resource

/**
 * Created by jcrygier on 4/22/15.
 */
@Slf4j
@RequestMapping("/api/process")
@RestController
@CompileStatic
class ProcessController {

    public static final String TIER_NAME_VARIABLE_KEY = "tierName";
    public static final String ENVIRONMENT_ID_VARIABLE_KEY = "environmentId";
    public static final String INITIATOR_AUTHENTICATION_OBJECT_VARIABLE_KEY = "initiatorAuthenticationObject";

    @Resource ServiceInstanceService serviceInstanceService;
    @Resource EnvironmentConfigurationService environmentConfigurationService;
    @Resource RuntimeService runtimeService;
    @Resource TaskService taskService;
    @Resource IdentityService identityService;
    @Resource RepositoryService repositoryService;

    @RequestMapping(value = "/{tierName}/{environmentId}/{processKey}", method = RequestMethod.POST)
    public def startProcess(@PathVariable("tierName") String tierName, @PathVariable("environmentId") String environmentId, @PathVariable("processKey") String processKey) {
        // Query for the process key, to ensure it's startable by this user
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().active().processDefinitionKey(processKey).singleResult();
        if (!isUserAuthorizedToStart(processDefinition))
            throw new AccessDeniedException("User ${SecurityUtil.getLoggedInUserId()} not authrorized to start $processKey workflow process")

        EnvironmentConfiguration environmentConfiguration = environmentConfigurationService.getEnvironment(tierName, environmentId);
        Map<String, Object> variables = environmentConfiguration.getApplications().collectEntries(this.&extractApplication);
        variables.put(TIER_NAME_VARIABLE_KEY, tierName)
        variables.put(ENVIRONMENT_ID_VARIABLE_KEY, environmentId)
        variables.put(INITIATOR_AUTHENTICATION_OBJECT_VARIABLE_KEY, SecurityUtil.getAuthentication());

        try {
            identityService.setAuthenticatedUserId(SecurityUtil.getLoggedInUserId())
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey, variables);

            return processInstance.getProcessInstanceId();
        } finally {
            identityService.setAuthenticatedUserId(null);
        }
    }

    @RequestMapping(value = "/{tierName}/{environmentId}", method = RequestMethod.GET)
    public def getProcesses(@PathVariable("tierName") String tierName, @PathVariable("environmentId") String environmentId) {
        Collection<ProcessDefinition> definitionsForEnv = repositoryService.createProcessDefinitionQuery()
                .active()
                .processDefinitionCategoryLike("%$tierName.$environmentId%")
                .list()
                .findAll { isUserAuthorizedToStart(it) };

        return definitionsForEnv.collect { ProcessDefinition processDefinition ->
            [
                    key: processDefinition.getKey(),
                    version: processDefinition.getVersion(),
                    name: processDefinition.getName(),
                    id: processDefinition.getId(),
                    deploymentId: processDefinition.getDeploymentId(),
                    processInstances: runtimeService.createProcessInstanceQuery().active().includeProcessVariables().processDefinitionKey(processDefinition.getKey()).list().findAll {
                        it.getProcessVariables()[TIER_NAME_VARIABLE_KEY] == tierName && it.getProcessVariables()[ENVIRONMENT_ID_VARIABLE_KEY] == environmentId
                    }.collect { ProcessInstance processInstance ->
                        return [
                                processInstanceId: processInstance.getProcessInstanceId(),
                                executions: runtimeService.createExecutionQuery().processInstanceId(processInstance.getProcessInstanceId()).list().findAll() {
                                    it.getParentId() != null
                                }.collect { Execution execution ->
                                    return [
                                            id: execution.getId(),
                                            activityId: execution.getActivityId(),
                                            currentStage: repositoryService.getBpmnModel(processDefinition.getId()).getMainProcess().getFlowElement(execution.getActivityId()).getName()

                                    ]
                                }
                        ]
                    }
            ]
        };
    }

    private boolean isUserAuthorizedToStart(ProcessDefinition processDefinition) {
        if (repositoryService.getIdentityLinksForProcessDefinition(processDefinition.getId()).size() == 0)
            return true;
        else
            return repositoryService.createProcessDefinitionQuery().startableByUser(SecurityUtil.getLoggedInUserId()).processDefinitionId(processDefinition.getId()).singleResult() != null
    }

    private Map<String, ?> extractApplication(ApplicationConfiguration applicationConfiguration) {
        return [(applicationConfiguration.getId()): applicationConfiguration.getServiceInstances().collectEntries {
            return extractServices(applicationConfiguration, it)
        }]
    }

    private Map<String, ?> extractServices(ApplicationConfiguration applicationConfiguration, ServiceInstanceConfiguration serviceInstanceConfiguration) {
        ServiceInstance serviceInstance = serviceInstanceService.getServiceInstances().find { ServiceInstance serviceInstance ->
            return serviceInstance.getTierName() == applicationConfiguration.getTierName() && serviceInstance.getEnvironmentName() == applicationConfiguration.getEnvironmentId() && serviceInstance.getApplicationId() == applicationConfiguration.getId() && serviceInstance.getName() == serviceInstanceConfiguration.getType()
        };

        Map<String, ?> templateBindings = serviceInstance?.getTemplateBindings();
        templateBindings?.remove("instance");            // Must do this, or it will attempt to serialize ServiceInstance

        return [(serviceInstanceConfiguration.getType()): templateBindings]
    }

}
