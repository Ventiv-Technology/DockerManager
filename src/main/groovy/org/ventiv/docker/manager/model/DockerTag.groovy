package org.ventiv.docker.manager.model

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * From https://docs.docker.com/reference/commandline/cli/#tag, the format is as follows:
 * [REGISTRYHOST/][USERNAME/]NAME[:TAG]
 *
 * Logic From: https://github.com/mafintosh/docker-parse-image/blob/master/index.js
 *
 * Created by jcrygier on 2/25/15.
 */
class DockerTag {

    String registry;
    String namespace;
    String repository;
    String tag;

    public DockerTag(String image) {
        def matcher = image =~ /^(?:([^\/]+)\/)?(?:([^\/]+)\/)?([^@:\/]+)(?:[@:](.+))?$/
        if (matcher) {
            registry = matcher[0][1]
            namespace = matcher[0][2] ?: "library"
            repository = matcher[0][3]
            tag = matcher[0][4]
        }
    }

    @JsonIgnore
    boolean isDockerHub() {
        return registry == null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()

        if (registry)
            sb.append(registry).append("/")

        if (namespace != "library")
            sb.append(namespace).append("/")

        sb.append(repository).append(":").append(tag);

        return sb.toString();
    }
}
