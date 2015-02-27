package org.ventiv.docker.manager.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by jcrygier on 2/25/15.
 */
class ImageLayerInformation {

    String id;
    String parent;
    Date created;
    String container;

    @JsonProperty("container_config")
    Map<String, Object> containerConfig;

    public List<Integer> getExposedPorts() {
        return containerConfig.ExposedPorts.keySet().collect { Integer.parseInt(it.substring(0, it.indexOf("/"))) }
    }

    public Map<String, String> getEnvironmentVariables() {
        return containerConfig.Env.collectEntries { it.split("=") as List<String> }
    }
}
