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
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.ventiv.docker.manager.security.DockerManagerPermission

/**
 * Created by jcrygier on 3/9/15.
 */
@ConfigurationProperties
class DockerManagerConfiguration {

    List<String> activeTiers;
    LdapConfiguration ldap;
    SecurityConfiguration auth;
    ConfigurationConfig config = new ConfigurationConfig();
    TemplateConfig template = new TemplateConfig();
    UiConfig ui = new UiConfig();
    List<Class> plugins;
    Long additionalMetricsRefreshDelay = 30 * 1000L;                 // 30 seconds
    Long dockerServerReconnectDelay = 1 * 60 * 60 * 1000L;           // 1 Hour

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
        List<String> permissionsToAudit;
        OAuthConfig oauth;

        public Collection<DockerManagerPermission> getAuditablePermissions() {
            if (permissionsToAudit == null)
                return DockerManagerPermission.getAllPermissions();

            return permissionsToAudit.collect { DockerManagerPermission.getPermission(it) }
        }

        @ConfigurationProperties
        public static class SecurityUser {
            String login;
            String password;
            List<String> roles;
        }

        @ConfigurationProperties
        public static class OAuthConfig {
            Boolean enabled;
            String signingKey;
            String keyStorePath;
            String keyStorePassword;
            String keyStoreAlias;
            List<BaseClientDetails> clients;
        }

        public static enum SecurityType {
            Ldap, InMemory, AllowAnyone
        }

    }

    @ConfigurationProperties
    public static class ConfigurationConfig {

        String location = "file:./config/env-config";
        GitConfig git;
        String buildHost = "boot2docker";
        Long refreshPeriod = 5000L;
        PrivateRegistryConfig registry;

        @ConfigurationProperties
        public static class GitConfig {
            String location = "config"
            String url;
            String user;
            String password;
            String branch = "master";
            Long refreshPeriod = 0L;            // 0 means do not refresh
        }

        @ConfigurationProperties
        public static class PrivateRegistryConfig {
            String username;
            String password;
            String email;
            String server;
        }

    }

    @ConfigurationProperties
    public static class TemplateConfig {

        String startToken = "#{"
        String endToken = "}";
        boolean ignoreMissingProperties = true;

    }

    @ConfigurationProperties
    public static class UiConfig {

        String splunkUrlTemplate;

    }

}
