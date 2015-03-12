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
            stages:
                - type: JenkinsBuild
                  settings:
                    server: https://fake.jenkins.com/jenkins/
                    jobName: JohnCTest
                    authentication: ProvidedUserPassword
                    user: jcrygier
                    password: NotMyRealPassword
        """
        ServiceBuildConfiguration config = new Yaml().loadAs(configYaml, ServiceBuildConfiguration);
        mockedJenkinsApi.startNewBuild("JohnCTest") >> new BuildStartedResponse([success: true, statusUrl: "https://fake.jenkins.com/jenkins/queue/item/536/", queueId: 536])
        mockedJenkinsApi.getBuildQueueStatus(536) >> new BuildQueueStatus([task: new BuildQueueStatus.TaskInformation([name: "JohnCTest"]), executable: new BuildQueueStatus.ExecutableInformation([number: 56])]);

        when:
        Promise<Map<String, Object>, Exception, String> buildContext = config.execute()

        then:
        buildContext
    }

}
