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
package org.ventiv.docker.manager.service

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.ventiv.docker.manager.build.BuildContext
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.model.DockerTag
import spock.lang.Specification

/**
 * Created by jcrygier on 3/19/15.
 */
class SimpleTemplateServiceTest extends Specification {

    SimpleTemplateService templateService;

    def setup() {
        templateService = new SimpleTemplateService();
        templateService.setProps(new DockerManagerConfiguration())

        templateService.createPatternMatcher();
    }

    def "simple template"() {
        when:
        String template = "This is a #{test}";
        String filledTemplate = templateService.fillTemplate(template, [test: "Hello World"]);

        then:
        filledTemplate == "This is a Hello World"
    }

    def "template with complex object (BuildContext)"() {
        setup:
        Authentication dummyAuth = new UsernamePasswordAuthenticationToken("jcrygier", "mypassword");
        BuildContext buildContext = new BuildContext(userAuthentication: dummyAuth, buildingVersion: "b1234", outputDockerImage: new DockerTag("ventivtech/docker_manager:latest"))

        when:
        String template = "This is a build kicked off by #{buildContext.userAuthentication.principal} with a variable that is not found: #{buildContext.noVariableExists}";
        String filledTemplate = templateService.fillTemplate(template, [buildContext: buildContext]);

        then:
        filledTemplate == "This is a build kicked off by jcrygier with a variable that is not found: #{buildContext.noVariableExists}"
    }

    def "template with complex object that doesn't ignore missing"() {
        setup:
        Authentication dummyAuth = new UsernamePasswordAuthenticationToken("jcrygier", "mypassword");
        BuildContext buildContext = new BuildContext(userAuthentication: dummyAuth, buildingVersion: "b1234", outputDockerImage: new DockerTag("ventivtech/docker_manager:latest"))
        templateService.props.template.ignoreMissingProperties = false;

        when:
        String template = "This is a build kicked off by #{buildContext.userAuthentication.principal} with a variable that is not found: #{buildContext.noVariableExists}";
        String filledTemplate = templateService.fillTemplate(template, [buildContext: buildContext]);

        then:
        thrown(MissingPropertyException)
    }

}
