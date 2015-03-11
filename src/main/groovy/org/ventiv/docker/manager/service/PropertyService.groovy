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
package org.ventiv.docker.manager.service

import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

import javax.annotation.Resource

/**
 * Created by jcrygier on 2/27/15.
 */
@Service
class PropertyService {

    @Resource Environment environment;

    def propertyMissing(String propertyName) {
        return environment.getProperty(propertyName);
    }

}
