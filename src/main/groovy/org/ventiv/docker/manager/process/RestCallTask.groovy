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

import groovy.util.logging.Slf4j
import org.activiti.engine.delegate.DelegateExecution
import org.activiti.engine.impl.el.Expression
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.utils.AuthenticationRequestInterceptor

import javax.annotation.Nullable
import javax.validation.constraints.NotNull

/**
 * Task that makes a RESTful call to all of the Service Instances in an Application of a given type.  The result will then
 * be flattened (via groovy's Collection.flatten) and stored into the 'outputVariable' variable for use in subsequent
 * steps of the workflow.
 */
@Slf4j
class RestCallTask extends AbstractDockerManagerTask {

    /**
     * Application ID that we should look at for Service Instances
     */
    @NotNull
    private Expression applicationId;

    /**
     * Service Instances that should be utilized
     */
    @NotNull
    private Expression serviceInstanceType;

    /**
     * URL of the request.  This will be appended to the Service Instance's URL variable.
     */
    @NotNull
    private Expression url;

    /**
     * Name of the output variable to store the result in
     */
    @Nullable
    private Expression outputVariable;

    /**
     * If the call requires authentication, the type goes here.  See AuthenticationRequestInterceptor
     */
    @Nullable
    private Expression authentication;

    /**
     * If the call requires authentication, the type goes here.  See AuthenticationRequestInterceptor
     */
    @Nullable
    private Expression user;

    /**
     * If the call requires authentication, the type goes here.  See AuthenticationRequestInterceptor
     */
    @Nullable
    private Expression password;

    /**
     * If the value's from the response need to be extracted (like a JSONPath) it can go here.  This will use SimpleTemplateService
     * to do the extraction
     */
    @Nullable
    private Expression variableExtractionTemplate;

    /**
     * If the results need to be aggregated before putting the results into outputVariable, you may specify one here.  Valid values:
     * COUNT, MIN, MAX, SUM, AVG
     */
    private Expression aggregationType;

    // For Testing, so we can mock REST calls
    protected RestTemplate restTemplate;

    @Override
    void execute(DelegateExecution execution) throws Exception {
        if (applicationId == null || serviceInstanceType == null || url == null)
            return;

        String tierName = getTierName(execution);
        String environmentId = getEnvironmentId(execution);
        String appId = applicationId.getValue(execution).toString();
        String serviceType = serviceInstanceType.getValue(execution).toString();

        Collection<ServiceInstance> serviceInstances = getServiceInstanceService().getServiceInstances().findAll {
            it.getTierName() == tierName && it.getEnvironmentName() == environmentId && it.getApplicationId() == appId && it.getName() == serviceType && it.getStatus() == ServiceInstance.Status.Running
        }

        Collection<Object> instanceResponses = serviceInstances.collect {
            Object response = getRestTemplate(execution).getForObject(it.getUrl() + url.getValue(execution).toString(), Object);

            if (response && variableExtractionTemplate) {
                String template = "#{data." + variableExtractionTemplate.getValue(execution) + "}"

                if (response instanceof Collection)
                    return response.collect { getSimpleTemplateService().fillTemplate(template, [data: it]) }
                else
                    return getSimpleTemplateService().fillTemplate(template, [data: response]);
            }

            return response;
        }

        if (outputVariable) {
            Collection<Object> flattenedResponse = instanceResponses.flatten();

            def variableValue;
            if (aggregationType)
                variableValue = aggregateResponse(flattenedResponse, aggregationType.getValue(execution).toString());
            else
                variableValue = flattenedResponse;

            execution.setVariable(outputVariable.getValue(execution).toString(), variableValue);
            log.debug("Extracted Variable (${outputVariable.getValue(execution)}) from RestCallTask to be (${variableValue?.getClass()}): $variableValue");
        }
    }

    private Object aggregateResponse(Collection<Object> responses, String aggregationType) {
        if (aggregationType == 'COUNT')
            return responses.size();
        else if (aggregationType == 'MIN')
            return responses.min();
        else if (aggregationType == 'MAX')
            return responses.max();
        else if (aggregationType == 'SUM')
            return responses.sum();
        else if (aggregationType == 'AVG')
            return responses.sum() / responses.size();

        return responses;
    }

    RestTemplate getRestTemplate(DelegateExecution execution) {
        Map<String, String> settings = [
                authentication: authentication?.getValue(execution)?.toString(),
                user: user?.getValue(execution)?.toString(),
                password: password?.getValue(execution)?.toString()
        ]

        RestTemplate restTemplate = this.restTemplate ?: new RestTemplate();
        restTemplate.setInterceptors(Arrays.asList((ClientHttpRequestInterceptor) new AuthenticationRequestInterceptor(settings, getInitiatorAuthentication(execution))));

        return restTemplate;
    }
    
}
