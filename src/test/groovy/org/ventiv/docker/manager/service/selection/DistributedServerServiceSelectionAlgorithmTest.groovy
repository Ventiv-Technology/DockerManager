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
package org.ventiv.docker.manager.service.selection

import org.ventiv.docker.manager.model.ApplicationDetails
import org.ventiv.docker.manager.model.ServiceInstance
import spock.lang.Specification

/**
 * Created by jcrygier on 3/20/15.
 */
class DistributedServerServiceSelectionAlgorithmTest extends Specification {
    
    ApplicationDetails applicationDetails = new ApplicationDetails([id: "TestApplication"])

    def "heavy loading on server1"() {
        setup:
        List<ServiceInstance> allServiceInstances = [
                // Server1 Instances
                new ServiceInstance(instanceNumber: 1, name: "AwesomeService", serverName: "server1", status: ServiceInstance.Status.Running),
                new ServiceInstance(instanceNumber: 2, name: "AwesomeService", serverName: "server1", status: ServiceInstance.Status.Available),
                new ServiceInstance(instanceNumber: 3, name: "AwesomeService", serverName: "server1", status: ServiceInstance.Status.Running),

                // Server 2 Instances
                new ServiceInstance(instanceNumber: 4, name: "AwesomeService", serverName: "server2", status: ServiceInstance.Status.Available),
                new ServiceInstance(instanceNumber: 5, name: "AwesomeService", serverName: "server2", status: ServiceInstance.Status.Available),
                new ServiceInstance(instanceNumber: 6, name: "AwesomeService", serverName: "server2", status: ServiceInstance.Status.Available)
        ]

        when:
        ServiceInstance selectedInstance = new DistributedServerServiceSelectionAlgorithm().getAvailableServiceInstance("AwesomeService", allServiceInstances, applicationDetails);

        then:
        selectedInstance.getInstanceNumber() == 4
    }

    def "heavy loading on server2"() {
        setup:
        List<ServiceInstance> allServiceInstances = [
                // Server1 Instances
                new ServiceInstance(instanceNumber: 1, name: "AwesomeService", serverName: "server1", status: ServiceInstance.Status.Running),
                new ServiceInstance(instanceNumber: 2, name: "AwesomeService", serverName: "server1", status: ServiceInstance.Status.Available),
                new ServiceInstance(instanceNumber: 3, name: "AwesomeService", serverName: "server1", status: ServiceInstance.Status.Available),

                // Server 2 Instances
                new ServiceInstance(instanceNumber: 4, name: "AwesomeService", serverName: "server2", status: ServiceInstance.Status.Running),
                new ServiceInstance(instanceNumber: 5, name: "AwesomeService", serverName: "server2", status: ServiceInstance.Status.Running),
                new ServiceInstance(instanceNumber: 6, name: "AwesomeService", serverName: "server2", status: ServiceInstance.Status.Available)
        ]

        when:
        ServiceInstance selectedInstance = new DistributedServerServiceSelectionAlgorithm().getAvailableServiceInstance("AwesomeService", allServiceInstances, applicationDetails);

        then:
        selectedInstance.getInstanceNumber() == 2
    }

    def "no services available"() {
        setup:
        List<ServiceInstance> allServiceInstances = [
                // Server1 Instances
                new ServiceInstance(instanceNumber: 1, name: "AwesomeService", serverName: "server1", status: ServiceInstance.Status.Running),

                // Server 2 Instances
                new ServiceInstance(instanceNumber: 2, name: "AwesomeService", serverName: "server2", status: ServiceInstance.Status.Running),
        ]

        when:
        ServiceInstance selectedInstance = new DistributedServerServiceSelectionAlgorithm().getAvailableServiceInstance("AwesomeService", allServiceInstances, applicationDetails);

        then:
        selectedInstance == null;
    }

    def "no services available - other services available"() {
        setup:
        List<ServiceInstance> allServiceInstances = [
                // Server1 Instances
                new ServiceInstance(instanceNumber: 1, name: "OtherService", serverName: "server1", status: ServiceInstance.Status.Running),

                // Server 2 Instances
                new ServiceInstance(instanceNumber: 2, name: "OtherService", serverName: "server2", status: ServiceInstance.Status.Available),
        ]

        when:
        ServiceInstance selectedInstance = new DistributedServerServiceSelectionAlgorithm().getAvailableServiceInstance("AwesomeService", allServiceInstances, applicationDetails);

        then:
        selectedInstance == null;
    }

    def "randomness 1"() {
        setup:
        List<ServiceInstance> allServiceInstances = [
                // Server1 Instances
                new ServiceInstance(instanceNumber: 1, name: "AwesomeService", serverName: "server1", status: ServiceInstance.Status.Running),
                new ServiceInstance(instanceNumber: 2, name: "AwesomeService", serverName: "server1", status: ServiceInstance.Status.Running),
                new ServiceInstance(instanceNumber: 3, name: "AwesomeService", serverName: "server1", status: ServiceInstance.Status.Running),

                // Server 2 Instances
                new ServiceInstance(instanceNumber: 4, name: "AwesomeService", serverName: "server2", status: ServiceInstance.Status.Stopped),
                new ServiceInstance(instanceNumber: 5, name: "AwesomeService", serverName: "server2", status: ServiceInstance.Status.Available),
                new ServiceInstance(instanceNumber: 6, name: "AwesomeService", serverName: "server2", status: ServiceInstance.Status.Available),

                // Server 3 Instances
                new ServiceInstance(instanceNumber: 7, name: "AwesomeService", serverName: "server3", status: ServiceInstance.Status.Available),
                new ServiceInstance(instanceNumber: 8, name: "OtherService", serverName: "server3", status: ServiceInstance.Status.Available),
                new ServiceInstance(instanceNumber: 9, name: "OtherService", serverName: "server3", status: ServiceInstance.Status.Stopped),
                new ServiceInstance(instanceNumber: 10, name: "OtherService", serverName: "server3", status: ServiceInstance.Status.Running),
        ]

        when:
        ServiceInstance selectedInstance = new DistributedServerServiceSelectionAlgorithm().getAvailableServiceInstance("AwesomeService", allServiceInstances, applicationDetails);

        then:
        selectedInstance.getInstanceNumber() == 7
    }

    def "randomness 2"() {
        setup:
        List<ServiceInstance> allServiceInstances = [
                // Server1 Instances
                new ServiceInstance(instanceNumber: 1, name: "AwesomeService", serverName: "server1", status: ServiceInstance.Status.Running),
                new ServiceInstance(instanceNumber: 2, name: "AwesomeService", serverName: "server1", status: ServiceInstance.Status.Running),
                new ServiceInstance(instanceNumber: 3, name: "AwesomeService", serverName: "server1", status: ServiceInstance.Status.Running),

                // Server 2 Instances
                new ServiceInstance(instanceNumber: 4, name: "AwesomeService", serverName: "server2", status: ServiceInstance.Status.Stopped),
                new ServiceInstance(instanceNumber: 5, name: "AwesomeService", serverName: "server2", status: ServiceInstance.Status.Available),
                new ServiceInstance(instanceNumber: 6, name: "AwesomeService", serverName: "server2", status: ServiceInstance.Status.Available),

                // Server 3 Instances
                new ServiceInstance(instanceNumber: 7, name: "OtherService", serverName: "server3", status: ServiceInstance.Status.Available),
                new ServiceInstance(instanceNumber: 8, name: "OtherService", serverName: "server3", status: ServiceInstance.Status.Available),
                new ServiceInstance(instanceNumber: 9, name: "OtherService", serverName: "server3", status: ServiceInstance.Status.Stopped),
                new ServiceInstance(instanceNumber: 10, name: "OtherService", serverName: "server3", status: ServiceInstance.Status.Running),
        ]

        when:
        ServiceInstance selectedInstance = new DistributedServerServiceSelectionAlgorithm().getAvailableServiceInstance("AwesomeService", allServiceInstances, applicationDetails);

        then:
        selectedInstance.getInstanceNumber() == 5
    }

}
