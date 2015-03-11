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

            if (!registry?.contains('.')) {
                namespace = registry;
                registry = null;
            }
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
