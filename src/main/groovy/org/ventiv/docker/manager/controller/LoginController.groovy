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
package org.ventiv.docker.manager.controller

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.ModelAndView
import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.config.DockerManagerConfiguration

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Created by jcrygier on 3/11/15.
 */
@Controller
class LoginController {

    @Resource DockerManagerConfiguration props;

    @RequestMapping("/login")
    def loginTest(HttpServletRequest request) {
        DockerManagerApplication.setApplicationUrl(request.getRequestURL().replaceAll('/login', '').toString());
        return new ModelAndView("login", [props: props])
    }

    @RequestMapping("/token")
    @ResponseBody
    String generateToken(@RequestParam(value = "expirationAmount", required = false) Integer expirationAmount,
                         @RequestParam(value = "expirationUnit", required = false) ChronoUnit expirationUnit,
                         Authentication authentication) {
        KeyPair key = props.getKeystore().getKey();
        Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) key.getPublic(), (RSAPrivateKey) key.getPrivate());

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            principal = principal.getUsername();
        }

        JWTCreator.Builder jwtBuilder = JWT.create()
                .withIssuer("DockerManager")
                .withSubject(principal.toString())
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(new Date())
                .withArrayClaim("authorities", authentication.getAuthorities().collect { it.getAuthority() } as String[]);

        if (expirationAmount) {
            LocalDate expirationDate = LocalDate.now();
            LocalDate adjustedExpiration = expirationDate.plus(expirationAmount, expirationUnit);

            jwtBuilder.withExpiresAt(java.sql.Date.valueOf(adjustedExpiration));
        }

        return jwtBuilder.sign(algorithm);
    }

}
