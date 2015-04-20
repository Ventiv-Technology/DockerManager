package org.ventiv.docker.manager.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;
import org.ventiv.docker.manager.model.UserAudit;

/**
 * Created by jcrygier on 4/20/15.
 */
public interface UserAuditRepository extends JpaRepository<UserAudit, Long> {

    @PreAuthorize("hasPermission(#tierName + '.' + #environmentName + '.' + #applicationId, 'READ_USER_AUDIT')")
    @Query("from UserAudit ua where ua.applicationThumbnail.tierName = :tierName and ua.applicationThumbnail.environmentName = :environmentName and ua.applicationThumbnail.applicationId = :applicationId")
    public Page<UserAudit> findUserAuditsForApplication(@Param("tierName") String tierName,
                                                              @Param("environmentName") String environmentName,
                                                              @Param("applicationId") String applicationId,
                                                              Pageable pageable);

    @PreAuthorize("hasPermission(#tierName + '.' + #environmentName + '.' + #applicationId, 'READ_USER_AUDIT')")
    @Query("from UserAudit ua where ua.serviceInstanceThumbnail.serverName = :serverName and ua.serviceInstanceThumbnail.application.tierName = :tierName and ua.serviceInstanceThumbnail.application.environmentName = :environmentName and ua.serviceInstanceThumbnail.application.applicationId = :applicationId and ua.serviceInstanceThumbnail.name = :name and ua.serviceInstanceThumbnail.instanceNumber = :instanceNumber")
    public Page<UserAudit> findUserAuditsForServiceInstance(@Param("serverName") String serverName,
                                                                  @Param("tierName") String tierName,
                                                                  @Param("environmentName") String environmentName,
                                                                  @Param("applicationId") String applicationId,
                                                                  @Param("name") String name,
                                                                  @Param("instanceNumber") Integer instanceNumber,
                                                                  Pageable pageable);

}
