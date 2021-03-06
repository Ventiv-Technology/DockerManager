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
package org.ventiv.docker.manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.ventiv.docker.manager.model.ServiceInstanceThumbnail;
import org.ventiv.docker.manager.model.metrics.AdditionalMetricsStorage;

import java.util.List;

/**
 * Created by jcrygier on 4/9/15.
 */
public interface AdditionalMetricsStorageRepository extends JpaRepository<AdditionalMetricsStorage, Long> {

    public List<AdditionalMetricsStorage> findByServiceInstanceThumbnailAndTimestampBetweenOrderByTimestampDesc(ServiceInstanceThumbnail serviceInstanceThumbnail, Long lowTimestamp, Long highTimestamp);

}
