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

import groovy.util.logging.Slf4j
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl
import org.springframework.security.acls.model.Acl
import org.springframework.security.acls.model.AclService
import org.springframework.security.acls.model.NotFoundException
import org.springframework.security.acls.model.Sid
import org.springframework.security.acls.model.SidRetrievalStrategy
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.model.ApplicationDetails
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.model.ServiceInstanceThumbnail
import org.ventiv.docker.manager.model.UserAudit
import org.ventiv.docker.manager.model.configuration.ApplicationConfiguration
import org.ventiv.docker.manager.service.EnvironmentConfigurationService
import org.ventiv.docker.manager.service.ServiceInstanceService
import org.ventiv.docker.manager.utils.UserAuditFilter

import javax.annotation.Resource

/**
 * Spring Security Permission Evaluator specifically for Docker Manager.  All permissions are at an Application Configuration
 * level, so the main purpose of this class is to find the ApplicationConfiguration object from the targetDomainObject (see getApplicationConfiguration)
 * and then compare the permissions.
 *
 * @author jcrygier
 */
@Slf4j
@Component
class DockerManagerPermissionEvaluator implements PermissionEvaluator {

    public static final def CONTAINER_ID_PATTERN = /[a-f0-9]{6,64}/
    public static final def DOCKER_NAME_PATTERN = /([a-zA-Z0-9][a-zA-Z0-9_-]*)\.([a-zA-Z0-9][a-zA-Z0-9_-]*)\.([a-zA-Z0-9][a-zA-Z0-9_-]*).*/

    @Resource DockerManagerConfiguration props;
    @Resource ServiceInstanceService serviceInstanceService;
    @Resource EnvironmentConfigurationService environmentConfigurationService;
    @Resource AuthorizationConfigurationService authorizationConfigurationService;
    @Resource AclService aclService;

    private SidRetrievalStrategy sidRetrievalStrategy = new SidRetrievalStrategyImpl();

    @Override
    boolean hasPermission(Authentication authentication, Object targetDomainObject, Object rawPermission) {
        if (!authorizationConfigurationService.isResourceExists())
            return true;

        ApplicationConfiguration applicationConfiguration = getApplicationConfiguration(targetDomainObject);
        if (applicationConfiguration) {
            try {
                Acl acl = aclService.readAclById(applicationConfiguration.getObjectIdentity())
                List<Sid> allSids = sidRetrievalStrategy.getSids(authentication);
                allSids << new GrantedAuthoritySid("ALL_USERS");
                DockerManagerPermission permission = DockerManagerPermission.getPermission(rawPermission)

                boolean granted = acl.isGranted([permission], allSids, false)

                if (granted)
                    auditAuthorizedPermission(authentication, targetDomainObject, applicationConfiguration, permission);

                return granted;
            } catch (NotFoundException e) {       // No permissions have been granted at all for this application
                log.debug("Object not authorized: $targetDomainObject, Permission: $rawPermission")
                return false;
            }
        }

        return true        // TODO: Verify permissions for docker containers that are not in the configuration / managed by DockerManager
    }

    void auditAuthorizedPermission(Authentication authentication, Object targetDomainObject, ApplicationConfiguration applicationConfiguration, DockerManagerPermission permission) {
        if (props.getAuth().getAuditablePermissions().contains(permission)) {
            UserAudit toPersist = new UserAudit([
                    principal               : new PrincipalSid(authentication).getPrincipal(),
                    permission              : DockerManagerPermission.getPermissionName(permission),
                    permissionEvaluated     : new Date(),
                    serviceInstanceThumbnail: getServiceInstanceThumbnail(targetDomainObject),
                    applicationThumbnail    : environmentConfigurationService.getApplicationThumbnail(applicationConfiguration.getTierName(), applicationConfiguration.getEnvironmentId(), applicationConfiguration.getId())
            ])

            // Put the UserAudit object in the ThreadLocal.  The UserAuditFilter will do the persisting in bulk at the end of the request.
            UserAuditFilter.getUserAudits().get()?.add(toPersist);
        }
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
            def matcher = targetDomainObject.toString() =~ DOCKER_NAME_PATTERN;
            if (matcher) {
                ServiceInstance serviceInstance = serviceInstanceService.getServiceInstances().find {
                    return it.tierName == matcher[0][1] && it.environmentName == matcher[0][2] && it.applicationId == matcher[0][3]
                }
                return getApplicationConfigForServiceInstance(serviceInstance);
            }

            // Next, check if it's a container id, then get from Service Instance Service
            matcher = targetDomainObject.toString() =~ CONTAINER_ID_PATTERN;
            if (matcher) {
                return getApplicationConfigForServiceInstance(serviceInstanceService.getServiceInstance(targetDomainObject.toString()))
            }
        } else if (targetDomainObject instanceof ServiceInstance) {
            return getApplicationConfigForServiceInstance(targetDomainObject)
        } else if (targetDomainObject instanceof ApplicationDetails) {
            return targetDomainObject.getApplicationConfiguration()
        }

        return null;
    }

    private ServiceInstanceThumbnail getServiceInstanceThumbnail(Object targetDomainObject) {
        if (targetDomainObject instanceof ServiceInstance)
            return serviceInstanceService.getServiceInstanceThumbnail(targetDomainObject);
        else if (targetDomainObject instanceof String) {
            def matcher = targetDomainObject.toString() =~ CONTAINER_ID_PATTERN;
            if (matcher) {
                return serviceInstanceService.getServiceInstanceThumbnail(serviceInstanceService.getServiceInstance(targetDomainObject.toString()))
            }
        }

        return null;
    }

    private ApplicationConfiguration getApplicationConfigForServiceInstance(ServiceInstance serviceInstance) {
        if (serviceInstance)
            return environmentConfigurationService.getEnvironment(serviceInstance.getTierName(), serviceInstance.getEnvironmentName())?.getApplications()?.find { it.getId() == serviceInstance.getApplicationId() }
        else
            return null;
    }

}
