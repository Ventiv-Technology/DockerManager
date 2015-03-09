package org.ventiv.docker.manager.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Created by jcrygier on 3/9/15.
 */
@ConfigurationProperties
class DockerManagerConfiguration {

    List<String> activeTiers;
    LdapConfiguration ldap;
    EnvironmentConfiguration environment;
    SecurityConfiguration auth;

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
    public static class EnvironmentConfiguration {

        String configLocation;

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

}
