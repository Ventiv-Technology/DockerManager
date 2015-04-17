package org.ventiv.docker.manager.security

import org.springframework.security.acls.domain.AbstractPermission
import org.springframework.security.acls.model.Permission

/**
 * The recognized permissions in the Docker Manager Application
 */
class DockerManagerPermission extends AbstractPermission {
    // Permissions for an Application
    public static final Permission READ =                   new DockerManagerPermission(1 << 0, 'R'); // 1
    public static final Permission START =                  new DockerManagerPermission(1 << 1, 'S'); // 2
    public static final Permission STOP =                   new DockerManagerPermission(1 << 2, 'K'); // 4
    public static final Permission RESTART =                new DockerManagerPermission(1 << 3, 'B'); // 8
    public static final Permission DEPLOY =                 new DockerManagerPermission(1 << 4, 'D'); // 16             NOTE: If you have DEPLOY permissions, you get READ, START, STOP, REMOVE as well
    public static final Permission LOGS =                   new DockerManagerPermission(1 << 5, 'L'); // 32
    public static final Permission REMOVE =                 new DockerManagerPermission(1 << 6, 'V'); // 64
    public static final Permission METRICS_OVERVIEW =       new DockerManagerPermission(1 << 7, 'O'); // 128
    public static final Permission METRICS_DETAILS =        new DockerManagerPermission(1 << 8, 'M'); // 256
    public static final Permission METRICS_TIME_SERIES =    new DockerManagerPermission(1 << 9, 'T'); // 512

    protected DockerManagerPermission(int mask) {
        super(mask);
    }

    protected DockerManagerPermission(int mask, String code) {
        super(mask, code.charAt(0));
    }

    public static Collection<Permission> getAllPermissions() {
        return [
                READ, START, STOP, RESTART, DEPLOY, LOGS, REMOVE, METRICS_DETAILS, METRICS_OVERVIEW, METRICS_TIME_SERIES
        ]
    }

    public static DockerManagerPermission getPermission(Object rawPermission) {
        if (rawPermission instanceof DockerManagerPermission)
            return (DockerManagerPermission) rawPermission;
        else
            return (DockerManagerPermission) DockerManagerPermission."$rawPermission";
    }

    public static String getPermissionName(Permission permission) {
        DockerManagerPermission.getDeclaredFields().find { it.get(null) == permission }?.getName()
    }
}
