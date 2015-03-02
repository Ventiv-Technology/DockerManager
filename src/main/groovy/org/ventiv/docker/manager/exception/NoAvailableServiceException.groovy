package org.ventiv.docker.manager.exception

/**
 * Created by jcrygier on 2/27/15.
 */
class NoAvailableServiceException extends RuntimeException {

    NoAvailableServiceException(String serviceName, String tierName, String environmentId, String applicationId) {
        super("No available service '$serviceName' for tier '$tierName', environment: '${environmentId}', application: '${applicationId}")
    }
}
