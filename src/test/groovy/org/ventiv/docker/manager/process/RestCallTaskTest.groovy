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
import org.activiti.engine.impl.el.FixedValue
import org.activiti.engine.impl.pvm.runtime.ExecutionImpl
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.service.ServiceInstanceService
import org.ventiv.docker.manager.service.SimpleTemplateService
import spock.lang.Specification

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

/**
 * Created by jcrygier on 5/4/15.
 */
class RestCallTaskTest extends Specification {

    def "simple call with one matching service"() {
        setup:
        ServiceInstanceService serviceInstanceService = new ServiceInstanceService([allServiceInstances: ["boot2docker": [
                new ServiceInstance([tierName: "TestingTier", environmentName: "TestingEnvironment", applicationId: "TestingApplication", name: "TestingService", status: ServiceInstance.Status.Running, url: "http://localhost:1/TestingService"]),
                new ServiceInstance([tierName: "TestingTier", environmentName: "TestingEnvironment", applicationId: "TestingApplication", name: "TestingService", status: ServiceInstance.Status.Stopped, url: "http://localhost:2/TestingService"]),
                new ServiceInstance([tierName: "TestingAnotherTier", environmentName: "TestingEnvironment", applicationId: "TestingApplication", name: "TestingService", status: ServiceInstance.Status.Running, url: "http://localhost:3/TestingService"])
        ]]]);

        RestCallTask task = new RestCallTask(serviceInstanceService: serviceInstanceService, simpleTemplateService: getSimpleTemplateService())
        task.applicationId = new FixedValue("TestingApplication");
        task.serviceInstanceType = new FixedValue("TestingService");
        task.url = new FixedValue("/healthCheck")
        task.outputVariable = new FixedValue("outputVariable")
        task.restTemplate = new RestTemplate();

        MockRestServiceServer mockServer = MockRestServiceServer.createServer(task.restTemplate);
        mockServer.expect(requestTo("http://localhost:1/TestingService/healthCheck")).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess('{ "testField1": 12345, "testField2": "StringValue" }', MediaType.APPLICATION_JSON));

        when:
        DelegateExecution execution = new ExecutionImpl(variables: [tierName: "TestingTier", environmentId: "TestingEnvironment"])
        task.execute(execution)

        then:
        mockServer.verify();
        execution.getVariable("outputVariable") == [[ testField1: 12345, testField2: "StringValue" ]]
    }

    def "aggregates and flattens two calls"() {
        setup:
        ServiceInstanceService serviceInstanceService = new ServiceInstanceService([allServiceInstances: ["boot2docker": [
                new ServiceInstance([tierName: "TestingTier", environmentName: "TestingEnvironment", applicationId: "TestingApplication", name: "TestingService", status: ServiceInstance.Status.Running, url: "http://localhost:1/TestingService"]),
                new ServiceInstance([tierName: "TestingTier", environmentName: "TestingEnvironment", applicationId: "TestingApplication", name: "TestingService", status: ServiceInstance.Status.Stopped, url: "http://localhost:2/TestingService"]),
                new ServiceInstance([tierName: "TestingTier", environmentName: "TestingEnvironment", applicationId: "TestingApplication", name: "TestingService", status: ServiceInstance.Status.Running, url: "http://localhost:3/TestingService"])
        ]]]);

        RestCallTask task = new RestCallTask(serviceInstanceService: serviceInstanceService, simpleTemplateService: getSimpleTemplateService())
        task.applicationId = new FixedValue("TestingApplication");
        task.serviceInstanceType = new FixedValue("TestingService");
        task.url = new FixedValue("/healthCheck")
        task.outputVariable = new FixedValue("outputVariable")
        task.restTemplate = new RestTemplate();

        MockRestServiceServer mockServer = MockRestServiceServer.createServer(task.restTemplate);

        mockServer.expect(requestTo("http://localhost:1/TestingService/healthCheck")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess('[{ "testField1": 12345, "testField2": "StringValue" }]', MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:3/TestingService/healthCheck")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess('[{ "testField1": 54321, "testField2": "AnotherStringValue" }, { "testField1": 98765, "testField2": "MoreValues" }]', MediaType.APPLICATION_JSON));

        when:
        DelegateExecution execution = new ExecutionImpl(variables: [tierName: "TestingTier", environmentId: "TestingEnvironment"])
        task.execute(execution)

        then:
        mockServer.verify();
        execution.getVariable("outputVariable") == [[ testField1: 12345, testField2: "StringValue" ], [ testField1: 54321, testField2: "AnotherStringValue" ], [ testField1: 98765, testField2: "MoreValues" ]]
    }

    def "aggregates (min) two calls"() {
        setup:
        ServiceInstanceService serviceInstanceService = new ServiceInstanceService([allServiceInstances: ["boot2docker": [
                new ServiceInstance([tierName: "TestingTier", environmentName: "TestingEnvironment", applicationId: "TestingApplication", name: "TestingService", status: ServiceInstance.Status.Running, url: "http://localhost:1/TestingService"]),
                new ServiceInstance([tierName: "TestingTier", environmentName: "TestingEnvironment", applicationId: "TestingApplication", name: "TestingService", status: ServiceInstance.Status.Stopped, url: "http://localhost:2/TestingService"]),
                new ServiceInstance([tierName: "TestingTier", environmentName: "TestingEnvironment", applicationId: "TestingApplication", name: "TestingService", status: ServiceInstance.Status.Running, url: "http://localhost:3/TestingService"])
        ]]]);

        RestCallTask task = new RestCallTask(serviceInstanceService: serviceInstanceService, simpleTemplateService: getSimpleTemplateService())
        task.applicationId = new FixedValue("TestingApplication");
        task.serviceInstanceType = new FixedValue("TestingService");
        task.url = new FixedValue("/healthCheck")
        task.outputVariable = new FixedValue("outputVariable")
        task.variableExtractionTemplate = new FixedValue("testField1")
        task.aggregationType = new FixedValue("MIN")
        task.restTemplate = new RestTemplate();

        MockRestServiceServer mockServer = MockRestServiceServer.createServer(task.restTemplate);

        mockServer.expect(requestTo("http://localhost:1/TestingService/healthCheck")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess('[{ "testField1": 12345, "testField2": "StringValue" }]', MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:3/TestingService/healthCheck")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess('[{ "testField1": 543, "testField2": "AnotherStringValue" }, { "testField1": 98765, "testField2": "MoreValues" }]', MediaType.APPLICATION_JSON));

        when:
        DelegateExecution execution = new ExecutionImpl(variables: [tierName: "TestingTier", environmentId: "TestingEnvironment"])
        task.execute(execution)

        then:
        mockServer.verify();
        execution.getVariable("outputVariable") == 543
    }

    private SimpleTemplateService getSimpleTemplateService() {
        SimpleTemplateService simpleTemplateService = new SimpleTemplateService([props: new DockerManagerConfiguration()])
        simpleTemplateService.createPatternMatcher();

        return simpleTemplateService;
    }

}
