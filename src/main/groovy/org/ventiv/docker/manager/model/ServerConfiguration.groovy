package org.ventiv.docker.manager.model

import javax.annotation.Nullable
import javax.validation.constraints.NotNull

/**
 * Created by jcrygier on 3/3/15.
 */
class ServerConfiguration {

    /**
     * Unique id for the host
     */
    @NotNull
    String id;

    /**
     * Name of the host to be shown in the UI.
     */
    @NotNull
    String description;

    /**
     * DNS Name of the host.
     */
    @NotNull
    String hostname;

    /**
     * Should we resolve this server to an IP and add it to /etc/hosts of any container created on this host?
     * This is mostly a workaround for boot2docker, since it cannot resolve to a real hostname.
     */
    @Nullable
    Boolean resolveHostname = false;

    /**
     * The list of services that are eligible to run on this host.
     */
    @NotNull
    List<EligibleServiceConfiguration> eligibleServices;

}
