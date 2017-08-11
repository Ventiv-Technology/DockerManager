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
