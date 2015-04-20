package org.ventiv.docker.manager.model

import com.fasterxml.jackson.annotation.JsonIgnore

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table

/**
 * Created by jcrygier on 4/20/15.
 */
@Entity
@Table(name = "application")
class ApplicationThumbnail {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    String tierName;

    String environmentName;

    String applicationId;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "application", fetch = FetchType.LAZY)
    List<ServiceInstanceThumbnail> serviceInstances;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "applicationThumbnail", fetch = FetchType.LAZY)
    List<UserAudit> userAudits;

}
