package org.ventiv.docker.manager.controller;

import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to deal with authorizations
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @RequestMapping
    public Object getUserDetails(@AuthenticationPrincipal Object principal) {
        return principal;
    }

}
