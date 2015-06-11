/**
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

    @Override
    //@PostFilter("hasPermission(filterObject.tierName + '.' + filterObject.environmentName + '.' + filterObject.applicationId, 'READ_USER_AUDIT')")
    Page<UserAudit> findAll(Pageable pageable);

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
