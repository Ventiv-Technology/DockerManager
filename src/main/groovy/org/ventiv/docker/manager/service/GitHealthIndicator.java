/**
 * Copyright (c) 2014 - 2017 Ventiv Technology
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
package org.ventiv.docker.manager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

/**
 * Created by jcrygier on 8/11/17.
 */
@Component
public class GitHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    private GitService gitService;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        if (!gitService.isEnabled()) {
            builder.status(new Status("DISABLED", "Git Service Disabled"));
        } else if (gitService.isRunning()) {
            builder.up()
                    .withDetail("nextRunTimeInSeconds", gitService.getNextRunTime())
                    .withDetail("lastRunTime", gitService.getLastRunTime())
                    .withDetail("pullErrorCount", gitService.getPullErrorCount());
        } else {
            builder.outOfService()
                    .withDetail("lastRunTime", gitService.getLastRunTime())
                    .withException(gitService.getLastPollException());
        }
    }

}
