package org.ventiv.docker.manager.plugin

import org.ventiv.docker.manager.model.ServiceInstance
import spock.lang.Specification

/**
 * Created by jcrygier on 12/14/16.
 */
class DynatracePluginTest extends Specification {

    def 'simple configuration'() {
        setup:
        def providedEnv = [
                DYNATRACE_PATH: '/test/path/agent.so',
                DYNATRACE_SERVER: 'dynatrace.hotstname.com',
                DYNATRACE_NAME: 'test-agent-name',
        ]
        ServiceInstance testSi = new ServiceInstance(resolvedEnvironmentVariables: providedEnv, serverName: 'test-server.hostname.com');

        when:
        new DynatracePlugin().doWithServiceInstance(testSi);
        def finalEnv = testSi.getResolvedEnvironmentVariables();

        then:
        finalEnv['JAVA_OPTS'].trim() == '-agentpath:/test/path/agent.so=name=test-agent-name,server=dynatrace.hotstname.com,overridehostname=test-server.hostname.com'
    }

    def 'will exclude classes'() {
        setup:
        def providedEnv = [
                DYNATRACE_PATH: '/test/path/agent.so',
                DYNATRACE_SERVER: 'dynatrace.hotstname.com',
                DYNATRACE_NAME: 'test-agent-name',
                DYNATRACE_EXCLUDE: 'org.ventiv.docker.manager.AbstractIntegrationTest',
                DYNATRACE_EXCLUDE_1: 'org.ventiv.docker.manager.DockermanagerApplicationTests',
                DYNATRACE_EXCLUDE_2: 'contains:$Proxy'
        ]
        ServiceInstance testSi = new ServiceInstance(resolvedEnvironmentVariables: providedEnv, serverName: 'test-server.hostname.com');

        when:
        new DynatracePlugin().doWithServiceInstance(testSi);
        def finalEnv = testSi.getResolvedEnvironmentVariables();

        then:
        finalEnv['JAVA_OPTS'].trim() == '-agentpath:/test/path/agent.so=name=test-agent-name,server=dynatrace.hotstname.com,overridehostname=test-server.hostname.com,exclude=starts:org/ventiv/docker/manager/AbstractIntegrationTest;starts:org/ventiv/docker/manager/DockermanagerApplicationTests;contains:$Proxy'
        !finalEnv.containsKey("DYNATRACE_PATH")
        !finalEnv.containsKey("DYNATRACE_SERVER")
        !finalEnv.containsKey("DYNATRACE_NAME")
        !finalEnv.containsKey("DYNATRACE_EXCLUDE")
        !finalEnv.containsKey("DYNATRACE_EXCLUDE_1")
        !finalEnv.containsKey("DYNATRACE_EXCLUDE_2")
    }

}
