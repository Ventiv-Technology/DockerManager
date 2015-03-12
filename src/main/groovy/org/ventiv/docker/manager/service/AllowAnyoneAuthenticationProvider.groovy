package org.ventiv.docker.manager.service

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException

/**
 * Simple Authentication Provider that allows anyone through.
 */
class AllowAnyoneAuthenticationProvider implements AuthenticationProvider {

    @Override
    Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return authentication;
    }

    @Override
    boolean supports(Class<?> authentication) {
        return true
    }

}
