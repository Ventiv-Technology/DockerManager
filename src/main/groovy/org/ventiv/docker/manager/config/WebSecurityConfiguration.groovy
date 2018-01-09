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
package org.ventiv.docker.manager.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.ldap.userdetails.InetOrgPersonContextMapper
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.ventiv.docker.manager.security.DockerManagerPermissionEvaluator
import org.ventiv.docker.manager.security.JwtAuthenticationFilter
import org.ventiv.docker.manager.service.AllowAnyoneAuthenticationProvider
import org.ventiv.docker.manager.utils.CacheHeaderWriter

import javax.annotation.Resource

/**
 * Web Security Configuration.  Currently only supports LDAP configured by properties.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Resource DockerManagerConfiguration props;
    @Resource DockerManagerPermissionEvaluator permissionEvaluator;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        if (props.auth.bypass)
            http.authorizeRequests().anyRequest().permitAll();
        else {
            DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
            expressionHandler.setPermissionEvaluator(permissionEvaluator);

            http.authorizeRequests()
                    .antMatchers("/webjars/**/*").permitAll()
                    .antMatchers("/app/css/*").permitAll()
                    .antMatchers("/health").permitAll()
                    .antMatchers("/api/feed.rss").permitAll()
                    .expressionHandler(expressionHandler)
                    .anyRequest()
                        .fullyAuthenticated()
                        .and()
                    .formLogin()
                        .loginPage("/login")
                        .permitAll()
                        .and()
                    .httpBasic()
                        .and()
                    .logout()
                        .logoutUrl("/logout")
                        .permitAll();
        }

        http.addFilterBefore(new JwtAuthenticationFilter(props), BasicAuthenticationFilter.class);

        // No matter what, we always want to set these headers, and disable CSRF protection
        http.headers()
                .contentTypeOptions()
                .xssProtection()
                .httpStrictTransportSecurity()
                .frameOptions()
                .addHeaderWriter(new CacheHeaderWriter())
                .and()
            .csrf()
                .disable();
    }

    @Configuration
    protected static class AuthenticationConfiguration extends GlobalAuthenticationConfigurerAdapter {

        @Resource DockerManagerConfiguration props;

        @Override
        public void init(AuthenticationManagerBuilder auth) throws Exception {
            auth.eraseCredentials(false);

            if (props.auth.bypass) {
                auth.inMemoryAuthentication()
            } else if (props.auth.type == DockerManagerConfiguration.SecurityConfiguration.SecurityType.InMemory) {
                InMemoryUserDetailsManagerConfigurer inMem = auth.inMemoryAuthentication();

                props.auth.users.each {
                    inMem.withUser(it.login).password(it.password).roles(it.roles as String[])
                }
            } else if (props.auth.type == DockerManagerConfiguration.SecurityConfiguration.SecurityType.Ldap) {
                auth.ldapAuthentication()
                        .userSearchBase(props.ldap.user.searchBase)
                        .userSearchFilter(props.ldap.user.searchFilter)
                        .groupSearchBase(props.ldap.group.searchBase)
                        .groupSearchFilter(props.ldap.group.searchFilter)
                        .userDetailsContextMapper(new InetOrgPersonContextMapper())
                        .contextSource()
                            .url(props.ldap.server.url)
                            .managerDn(props.ldap.server.managerDn)
                            .managerPassword(props.ldap.server.managerPassword)
            } else if (props.auth.type == DockerManagerConfiguration.SecurityConfiguration.SecurityType.AllowAnyone)
                auth.authenticationProvider(new AllowAnyoneAuthenticationProvider());
        }
    }
}