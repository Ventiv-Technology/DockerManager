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
package org.ventiv.docker.manager.security

import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl
import org.springframework.security.acls.model.Acl
import org.springframework.security.acls.model.AclService
import org.springframework.security.acls.model.NotFoundException
import org.springframework.security.acls.model.SidRetrievalStrategy
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.configuration.ApplicationConfiguration
import org.ventiv.docker.manager.service.EnvironmentConfigurationService
import org.ventiv.docker.manager.service.ServiceInstanceService

import javax.annotation.Resource

/**
 * Spring Security Permission Evaluator specifically for Docker Manager.  All permissions are at an Application Configuration
 * level, so the main purpose of this class is to find the ApplicationConfiguration object from the targetDomainObject (see getApplicationConfiguration)
 * and then compare the permissions.
 *
 * @author jcrygier
 */
@Component
class DockerManagerPermissionEvaluator implements PermissionEvaluator {

    public static final def CONTAINER_ID_PATTERN = /[a-f0-9]{6,64}/

    @Resource ServiceInstanceService serviceInstanceService;
    @Resource EnvironmentConfigurationService environmentConfigurationService;
    @Resource AclService aclService;

    private SidRetrievalStrategy sidRetrievalStrategy = new SidRetrievalStrategyImpl();

    @Override
    boolean hasPermission(Authentication authentication, Object targetDomainObject, Object rawPermission) {
        ApplicationConfiguration applicationConfiguration = getApplicationConfiguration(targetDomainObject);
        if (applicationConfiguration) {
            try {
                Acl acl = aclService.readAclById(applicationConfiguration.getObjectIdentity())
                return acl.isGranted([DockerManagerPermission.getPermission(rawPermission)], sidRetrievalStrategy.getSids(authentication), false)
            } catch (NotFoundException ignored) {       // No permissions have been granted at all for this application
                return false;
            }
        }

        // TODO: If the authorization.yml file is completely missing, ignore auth

        return true        // TODO: Verify permissions for docker containers that are not in the configuration / managed by DockerManager
    }

    @Override
    boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false
    }

    /**
     * Attempts to take a targetDomainObject (From a spring security expression) and translate it into the Application Configuration
     * Object, where the security authorizations live.  Supported mechanisms:
     *
     * - String with Docker Name (e.g. tier.environment.application.service.instanceNumber)
     * - String with container ID (e.g. hexadecimal number between 6 - 64 characters)
     * - ApplicationConfiguration object itself
     * - ServiceInstance object
     *
     * @param targetDomainObject
     * @return
     */
    private ApplicationConfiguration getApplicationConfiguration(Object targetDomainObject) {
        if (targetDomainObject instanceof ApplicationConfiguration)
            return targetDomainObject;
        else if (targetDomainObject instanceof String) {
            // First, let's check if it's a fully qualified docker name
            def matcher = targetDomainObject.toString() =~ ServiceInstance.DOCKER_NAME_PATTERN;
            if (matcher) {
                ServiceInstance serviceInstance = serviceInstanceService.getServiceInstances().find { it.tierName == targetDomainObject.toString() }
                return getApplicationConfigForServiceInstance(serviceInstance);
            }

            // Next, check if it's a container id, then get from Service Instance Service
            matcher = targetDomainObject.toString() =~ CONTAINER_ID_PATTERN;
            if (matcher) {
                return getApplicationConfigForServiceInstance(serviceInstanceService.getServiceInstance(targetDomainObject.toString()))
            }
        } else if (targetDomainObject instanceof ServiceInstance)
            return getApplicationConfigForServiceInstance(targetDomainObject)

        return null;
    }

    private ApplicationConfiguration getApplicationConfigForServiceInstance(ServiceInstance serviceInstance) {
        if (serviceInstance)
            return environmentConfigurationService.getEnvironment(serviceInstance.getTierName(), serviceInstance.getEnvironmentName())?.getApplications()?.find { it.getId() == serviceInstance.getApplicationId() }
        else
            return null;
    }

}
