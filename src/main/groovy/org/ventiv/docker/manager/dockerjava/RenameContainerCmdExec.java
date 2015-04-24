/**
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
package org.ventiv.docker.manager.dockerjava;

import com.github.dockerjava.jaxrs.AbstrDockerCmdExec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import static javax.ws.rs.client.Entity.entity;

/**
 * Created by jcrygier on 4/24/15.
 */
public class RenameContainerCmdExec extends AbstrDockerCmdExec<RenameContainerCmd, Void> implements RenameContainerCmd.Exec {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenameContainerCmdExec.class);

    public RenameContainerCmdExec(WebTarget baseResource) {
        super(baseResource);
    }

    @Override
    protected Void execute(RenameContainerCmd command) {
        WebTarget webResource = getBaseResource().path("/containers/{id}/rename").resolveTemplate("id", command.getContainerId()).queryParam("name", command.getNewName());

        LOGGER.trace("POST: {}", webResource);
        webResource.request().accept(MediaType.APPLICATION_JSON).post(entity(command, MediaType.APPLICATION_JSON)).close();

        return null;
    }

}
