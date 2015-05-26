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
package org.ventiv.docker.manager.model.configuration

import javax.validation.constraints.NotNull

/**
 * Created by jcrygier on 5/26/15.
 */
class LinkConfiguration {

    /**
     * The service name of the container that you wish to link
     */
    @NotNull
    String container;

    /**
     * The alias for within the container to be created for this link.  This will become the hostname that resolves to the
     * foreign container.
     */
    @NotNull
    String alias;

}
