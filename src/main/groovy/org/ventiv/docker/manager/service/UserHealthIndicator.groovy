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

import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl
import org.springframework.security.acls.model.Acl
import org.springframework.security.acls.model.AclService
import org.springframework.security.acls.model.NotFoundException
import org.springframework.security.acls.model.Permission
import org.springframework.security.acls.model.Sid
import org.springframework.security.acls.model.SidRetrievalStrategy
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.ventiv.docker.manager.model.configuration.ApplicationConfiguration
import org.ventiv.docker.manager.model.configuration.EnvironmentConfiguration
import org.ventiv.docker.manager.security.DockerManagerPermission

import javax.annotation.Resource

/**
 * Created by jcrygier on 3/11/15.
 */
@Component
class UserHealthIndicator extends AbstractHealthIndicator {

    @Resource EnvironmentConfigurationService environmentConfigurationService;
    @Resource AclService aclService;

    private SidRetrievalStrategy sidRetrievalStrategy = new SidRetrievalStrategyImpl();

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication()
        if (auth) {
            builder.up().withDetail("user", [
                    name: auth.getName(),
                    authorities: auth.getAuthorities(),
                    effectivePermissions: getEffectivePermissions(auth)
            ]);
        }
    }

    protected Collection<EffectiveDockerManagerPermission> getEffectivePermissions(Authentication authentication) {
        Collection<EffectiveDockerManagerPermission> effectivePermissions = []

        environmentConfigurationService.getActiveEnvironments().each { EnvironmentConfiguration environmentConfiguration ->
            environmentConfiguration.getApplications().each { ApplicationConfiguration applicationConfiguration ->
                Acl acl = aclService.readAclById(applicationConfiguration.getObjectIdentity())

                List<Sid> allSids = sidRetrievalStrategy.getSids(authentication);
                allSids << new GrantedAuthoritySid("ALL_USERS");

                DockerManagerPermission.getAllPermissions().each { Permission permission ->
                    try {
                        if (acl.isGranted([permission], allSids, false)) {
                            effectivePermissions << new EffectiveDockerManagerPermission([
                                    tierName: applicationConfiguration.getTierName(),
                                    environmentId: applicationConfiguration.getEnvironmentId(),
                                    applicationId: applicationConfiguration.getId(),
                                    grantedPermission: DockerManagerPermission.getPermissionName(permission)
                            ]);
                        }
                    } catch (NotFoundException ignored) {}
                }
            }
        }

        return effectivePermissions
    }

    public static final class EffectiveDockerManagerPermission {
        String tierName;
        String environmentId;
        String applicationId;
        String grantedPermission;
    }

}
