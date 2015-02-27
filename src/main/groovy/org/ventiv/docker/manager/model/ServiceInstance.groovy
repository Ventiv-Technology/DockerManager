package org.ventiv.docker.manager.model

import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.UniqueConstraint

/**
 * Created by jcrygier on 2/26/15.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = ["tierName", "environmentName"]))
class ServiceInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    String tierName;
    String environmentName;
    String name;
    String serviceName;
    String serverName;

    @OneToMany(targetEntity=PortDefinition.class, mappedBy="serviceInstance", fetch=FetchType.EAGER)
    List<PortDefinition> portDefinitions;

}
