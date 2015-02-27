package org.ventiv.docker.manager.service

import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

import javax.annotation.Resource

/**
 * Created by jcrygier on 2/27/15.
 */
@Service
class PropertyService {

    @Resource Environment environment;

    def propertyMissing(String propertyName) {
        return environment.getProperty(propertyName);
    }

}
