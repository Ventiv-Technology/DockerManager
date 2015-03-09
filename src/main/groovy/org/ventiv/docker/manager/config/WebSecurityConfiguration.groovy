package org.ventiv.docker.manager.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

import javax.annotation.Resource

/**
 * Web Security Configuration.  Currently only supports LDAP configured by properties.
 */
@Configuration
@EnableWebSecurity
class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Resource DockerManagerConfiguration props;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        if (props.auth.bypass)
            http.authorizeRequests().anyRequest().permitAll();
        else {
            http.authorizeRequests()
                    .anyRequest()
                        .fullyAuthenticated()
                        .and()
                    .httpBasic()
                        .realmName(props.auth.basicRealm)
                        .and()
                    .formLogin();
        }

        http.csrf().disable();
    }

    @Configuration
    protected static class AuthenticationConfiguration extends GlobalAuthenticationConfigurerAdapter {

        @Resource DockerManagerConfiguration props;

        @Override
        public void init(AuthenticationManagerBuilder auth) throws Exception {
            if (props.auth.bypass) {
                auth.inMemoryAuthentication();
            } else if (props.auth.type == DockerManagerConfiguration.SecurityConfiguration.SecurityType.Ldap) {
                auth.ldapAuthentication()
                        .userSearchBase(props.ldap.user.searchBase)
                        .userSearchFilter(props.ldap.user.searchFilter)
                        .groupSearchBase(props.ldap.group.searchBase)
                        .groupSearchFilter(props.ldap.group.searchFilter)
                        .contextSource()
                            .url(props.ldap.server.url)
                            .managerDn(props.ldap.server.managerDn)
                            .managerPassword(props.ldap.server.managerPassword)
            }
        }
    }
}