package org.ventiv.docker.manager.exception

/**
 * Created by jcrygier on 2/27/15.
 */
class NoAvailableServiceException extends RuntimeException {

    NoAvailableServiceException(String serviceName, String environmentName) {
        super("No available service '$serviceName' in environment '$environmentName'")
    }
}
