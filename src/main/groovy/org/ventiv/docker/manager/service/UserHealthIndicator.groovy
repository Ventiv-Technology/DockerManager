package org.ventiv.docker.manager.service

import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Created by jcrygier on 3/11/15.
 */
@Component
class UserHealthIndicator extends AbstractHealthIndicator {

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication()
        if (auth) {
            builder.up().withDetail("user", [
                    name: auth.getName(),
                    authorities: auth.getAuthorities()
            ]);
        }
    }

}
