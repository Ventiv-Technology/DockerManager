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
package org.ventiv.docker.manager.model

import javax.annotation.Nullable
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MapKeyColumn
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
    String permission;

    @NotNull
    Date permissionEvaluated;

    Date requestFinished;

    UUID requestUUID;

    @Nullable
    @ManyToOne
    @JoinColumn(name = "service_instance_id", nullable = true)
    ServiceInstanceThumbnail serviceInstanceThumbnail;

    @Nullable
    @ManyToOne
    @JoinColumn(name = "application_id", nullable = true)
    ApplicationThumbnail applicationThumbnail;

    @ElementCollection
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    @CollectionTable(name = "user_audit_details", joinColumns = @JoinColumn(name = "user_audit_id"))
    Map<String, String> auditDetails;

    public boolean isPersistable() {
        return permission != null;
    }

}
