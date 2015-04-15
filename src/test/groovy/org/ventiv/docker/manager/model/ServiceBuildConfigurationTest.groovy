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

import org.jdeferred.Promise
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.build.JenkinsBuild
import org.ventiv.docker.manager.build.jenkins.BuildQueueStatus
import org.ventiv.docker.manager.build.jenkins.BuildStartedResponse
import org.ventiv.docker.manager.build.jenkins.JenkinsApi
import org.ventiv.docker.manager.model.configuration.ServiceBuildConfiguration
import org.ventiv.docker.manager.model.configuration.ServiceConfiguration
import org.yaml.snakeyaml.Yaml
import spock.lang.Specification

import javax.annotation.Resource

/**
 * Created by jcrygier on 3/12/15.
 */
@WebAppConfiguration
@ContextConfiguration(loader = SpringApplicationContextLoader, classes = DockerManagerApplication)
class ServiceBuildConfigurationTest extends Specification {

    @Resource ApplicationContext ac;
    JenkinsApi mockedJenkinsApi;

    def setup() {
        DockerManagerApplication.applicationContext = ac;

        mockedJenkinsApi = Mock(JenkinsApi)
        ac.getBean(JenkinsBuild).mockJenkinsApi = mockedJenkinsApi
    }

    def "can perform a jenkins build"() {
        setup:
        def configYaml = """
            name: mysql
            description: MySql Sample Service for Testing
            image: mysql
            build:
                stages:
                    - type: JenkinsBuild
                      settings:
                        server: https://fake.jenkins.com/jenkins/
                        jobName: JohnCTest
                        authentication: ProvidedUserPassword
                        user: jcrygier
                        password: NotMyRealPassword
        """
        ServiceConfiguration config = new Yaml().loadAs(configYaml, ServiceConfiguration);
        mockedJenkinsApi.startNewBuild("JohnCTest") >> new BuildStartedResponse([success: true, statusUrl: "https://fake.jenkins.com/jenkins/queue/item/536/", queueId: 536])
        mockedJenkinsApi.getBuildQueueStatus(536) >> new BuildQueueStatus([task: new BuildQueueStatus.TaskInformation([name: "JohnCTest"]), executable: new BuildQueueStatus.ExecutableInformation([number: 56])]);

        when:
        ApplicationDetails details = new ApplicationDetails(tierName: "localhost", environmentName: "boot2docker")
        Promise<Map<String, Object>, Exception, String> buildContext = config.getBuild().execute(details, config, ServiceBuildConfiguration.BUILD_NEW_VERSION)

        then:
        buildContext
    }

}
