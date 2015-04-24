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
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.model.Permission
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.ventiv.docker.manager.DockerManagerApplication

/**
 * Created by jcrygier on 4/15/15.
 */
class SecurityUtil {

    private static PermissionEvaluator permissionEvaluator;

    public static <T> Collection<T> filter(Collection<T> toFilter, Permission permission) {
        return toFilter.findAll() { getPermissionEvaluator().hasPermission(SecurityContextHolder.getContext().getAuthentication(), it, permission) }
    }

    public static PermissionEvaluator getPermissionEvaluator() {
        if (permissionEvaluator == null)
            permissionEvaluator = DockerManagerApplication.getApplicationContext().getBean(PermissionEvaluator)

        return permissionEvaluator;
    }

    public static final Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static final String getLoggedInUserId(Authentication authentication = getAuthentication()) {
        return new PrincipalSid(authentication).getPrincipal()
    }


}
