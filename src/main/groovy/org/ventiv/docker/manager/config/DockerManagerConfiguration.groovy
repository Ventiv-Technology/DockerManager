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
    ConfigurationConfig config;

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
        String basicRealm = "Docker Manager";

        public static enum SecurityType {
            Ldap
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
