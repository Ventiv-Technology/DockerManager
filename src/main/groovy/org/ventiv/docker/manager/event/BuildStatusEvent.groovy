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
package org.ventiv.docker.manager.event

import org.springframework.context.ApplicationEvent
import org.ventiv.docker.manager.model.BuildStatus

/**
 * Created by jcrygier on 3/13/15.
 */
class BuildStatusEvent extends ApplicationEvent {

    /**
     * Create a new ApplicationEvent.
     * @param source the component that published the event (never {@code null})
     */
    BuildStatusEvent(BuildStatus buildStatus) {
        super(buildStatus);
    }

}
