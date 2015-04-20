package org.ventiv.docker.manager.model

import org.springframework.boot.actuate.audit.AuditEvent
import org.ventiv.docker.manager.security.DockerManagerPermission

import javax.annotation.Nullable
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

/**
 * Represents a user action from the UI
 *
 * - Deploy Application
 * - Start / Stop Instance
 */
@Entity
@Table(name = "user_audit")
class UserAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @NotNull
    String principal;

    @NotNull
    DockerManagerPermission permission;

    @Nullable
    @ManyToOne
    @JoinColumn(name = "service_instance_id", nullable = true)
    ServiceInstanceThumbnail serviceInstanceThumbnail;

    @Nullable
    @ManyToOne
    @JoinColumn(name = "application_id", nullable = true)
    ApplicationThumbnail applicationThumbnail;

    public boolean isPersistable() {
        return permission != null;
    }

    public static UserAudit fromAuditEvent(AuditEvent auditEvent) {
        return new UserAudit([
                principal: auditEvent.getPrincipal(),
                auditType: DockerManagerPermission.getPermission(auditEvent.getType()),
                serviceInstanceThumbnail: auditEvent.getData().get("serviceInstanceThumbnail")
        ])
    }

}
