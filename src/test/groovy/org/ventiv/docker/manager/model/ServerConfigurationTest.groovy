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

import org.yaml.snakeyaml.Yaml
import spock.lang.Specification

/**
 * Created by jcrygier on 3/23/15.
 */
class ServerConfigurationTest extends Specification {

    def "reading a configuration"() {
        setup:
        String yaml = """
            id: boot2docker
            description: Boot2Docker Host
            hostname: boot2docker
            resolveHostname: true
            eligibleServices:
                - type: rabbit
                  portMappings:
                    - type: http
                      ports: 15672-15680,15685
                    - type: amqp
                      ports: 5672-5681
        """
        ServerConfiguration serverConfiguration = new Yaml().loadAs(yaml, ServerConfiguration)

        when:
        List<EligibleServiceConfiguration> services = serverConfiguration.getEligibleServices()

        then:
        services.size() == 10
        services*.getType() == ['rabbit'] * 10

        services[0].getPortMappings()[0].getType() == 'http'
        services[0].getPortMappings()[0].getPort() == 15672
        services[0].getPortMappings()[1].getType() == 'amqp'
        services[0].getPortMappings()[1].getPort() == 5672

        services[9].getPortMappings()[0].getType() == 'http'
        services[9].getPortMappings()[0].getPort() == 15685
        services[9].getPortMappings()[1].getType() == 'amqp'
        services[9].getPortMappings()[1].getPort() == 5681
    }

    def "configuration with no ranges"() {
        setup:
        String yaml = """
            id: boot2docker
            description: Boot2Docker Host
            hostname: boot2docker
            resolveHostname: true
            eligibleServices:
                - type: rabbit
                  portMappings:
                    - type: http
                      port: 15672
                    - type: amqp
                      port: 5672
        """
        ServerConfiguration serverConfiguration = new Yaml().loadAs(yaml, ServerConfiguration)

        when:
        List<EligibleServiceConfiguration> services = serverConfiguration.getEligibleServices()

        then:
        services.size() == 1
    }

}
