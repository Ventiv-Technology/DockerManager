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

import org.springframework.security.acls.domain.AbstractPermission
import org.springframework.security.acls.model.Permission

/**
 * The recognized permissions in the Docker Manager Application
 */
class DockerManagerPermission extends AbstractPermission {
    // Permissions for an Application
    public static final DockerManagerPermission READ =                   new DockerManagerPermission(1 << 0, 'R'); // 1
    public static final DockerManagerPermission START =                  new DockerManagerPermission(1 << 1, 'S'); // 2
    public static final DockerManagerPermission STOP =                   new DockerManagerPermission(1 << 2, 'K'); // 4
    public static final DockerManagerPermission RESTART =                new DockerManagerPermission(1 << 3, 'B'); // 8
    public static final DockerManagerPermission DEPLOY =                 new DockerManagerPermission(1 << 4, 'D'); // 16             NOTE: If you have DEPLOY permissions, you get READ, START, STOP, REMOVE as well
    public static final DockerManagerPermission LOGS =                   new DockerManagerPermission(1 << 5, 'L'); // 32
    public static final DockerManagerPermission REMOVE =                 new DockerManagerPermission(1 << 6, 'V'); // 64
    public static final DockerManagerPermission METRICS_OVERVIEW =       new DockerManagerPermission(1 << 7, 'O'); // 128
    public static final DockerManagerPermission METRICS_DETAILS =        new DockerManagerPermission(1 << 8, 'M'); // 256
    public static final DockerManagerPermission METRICS_TIME_SERIES =    new DockerManagerPermission(1 << 9, 'T'); // 512
    public static final DockerManagerPermission READ_USER_AUDIT =        new DockerManagerPermission(1 << 10, 'A'); //1024

    protected DockerManagerPermission(int mask) {
        super(mask);
    }

    protected DockerManagerPermission(int mask, String code) {
        super(mask, code.charAt(0));
    }

    public static Collection<DockerManagerPermission> getAllPermissions() {
        return [
                READ, START, STOP, RESTART, DEPLOY, LOGS, REMOVE, METRICS_DETAILS, METRICS_OVERVIEW, METRICS_TIME_SERIES, READ_USER_AUDIT
        ]
    }

    public static DockerManagerPermission getPermission(Object rawPermission) {
        try {
            if (rawPermission instanceof DockerManagerPermission)
                return (DockerManagerPermission) rawPermission;
            else
                return (DockerManagerPermission) DockerManagerPermission."$rawPermission";
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String getPermissionName(Permission permission) {
        DockerManagerPermission.getDeclaredFields().find { it.get(null) == permission }?.getName()
    }
}
