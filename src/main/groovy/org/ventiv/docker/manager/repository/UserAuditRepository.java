package org.ventiv.docker.manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.ventiv.docker.manager.model.UserAudit;

/**
 * Created by jcrygier on 4/20/15.
 */
public interface UserAuditRepository extends JpaRepository<UserAudit, Long> {
}
