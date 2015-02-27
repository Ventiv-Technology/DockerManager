package org.ventiv.docker.manager.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

/**
 * Created by jcrygier on 2/26/15.
 */
@Entity
class PortDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    Integer hostPort;
    Integer containerPort;
    String portType;

    @ManyToOne(targetEntity = ServiceInstance)
    ServiceInstance serviceInstance;

}
