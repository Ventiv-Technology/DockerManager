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
package org.ventiv.docker.manager.metrics;

import org.ventiv.docker.manager.model.ServiceInstance;

import java.util.Map;

/**
 * Created by jcrygier on 4/3/15.
 */
public interface AdditionalMetrics {

    /**
     * Retrieve the additional metrics, and return it in it's own form to be used in it's corresponding UI.
     *
     * @param serviceInstance The Service Instance to get the additional metrics for
     * @param settings Settings provided in the configuration
     * @return
     */
    public Object getAdditionalMetrics(ServiceInstance serviceInstance, Map<String, String> settings);

}
