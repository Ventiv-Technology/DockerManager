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

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Created by jcrygier on 3/9/15.
 */
@ConfigurationProperties
class DockerManagerConfiguration {

    List<String> activeTiers;
    LdapConfiguration ldap;
    SecurityConfiguration auth;
    ConfigurationConfig config = new ConfigurationConfig();

    @ConfigurationProperties
    public static class LdapConfiguration {

        LdapServerConfiguration server;
        LdapSearchConfiguration user;
        LdapSearchConfiguration group;

        @ConfigurationProperties
        public static class LdapServerConfiguration {
            String url;
            String managerDn;
            String managerPassword;
        }

        public static class LdapSearchConfiguration {
            String searchBase;
            String searchFilter;
        }
    }

    @ConfigurationProperties
    public static class SecurityConfiguration {

        boolean bypass = false;
        SecurityType type;
        String realm = "Docker Manager";
        List<SecurityUser> users;

        @ConfigurationProperties
        public static class SecurityUser {
            String login;
            String password;
            List<String> roles;
        }

        public static enum SecurityType {
            Ldap, InMemory
        }

    }

    @ConfigurationProperties
    public static class ConfigurationConfig {

        String location = "file:./config/env-config";
        GitConfig git;

        @ConfigurationProperties
        public static class GitConfig {
            String location = "config"
            String url;
            String user;
            String password;
            String branch = "master";
            Long refreshPeriod = 0L;            // 0 means do not refresh
        }

    }

}
