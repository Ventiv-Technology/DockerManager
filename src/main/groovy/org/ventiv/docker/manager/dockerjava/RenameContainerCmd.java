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

import com.github.dockerjava.api.ConflictException;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.command.DockerCmd;
import com.github.dockerjava.api.command.DockerCmdExec;

/**
 * Created by jcrygier on 4/24/15.
 */
public interface RenameContainerCmd extends DockerCmd<Void> {

    public static interface Exec extends DockerCmdExec<RenameContainerCmd, Void> {
    }

    @Override
    public Void exec() throws NotFoundException, ConflictException;

    public String getContainerId();

    public String getNewName();

    public RenameContainerCmd withContainerId(String containerId);

    public RenameContainerCmd withNewName(String newName);

}
