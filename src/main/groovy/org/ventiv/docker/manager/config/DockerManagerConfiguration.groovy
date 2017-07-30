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
import org.springframework.core.io.Resource
import org.ventiv.docker.manager.security.DockerManagerPermission
import sun.security.tools.keytool.CertAndKeyGen
import sun.security.x509.X500Name

import java.security.Key
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate

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
    KeyStoreConfig keystore = new KeyStoreConfig();
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

    @ConfigurationProperties
    public static class KeyStoreConfig {

        Resource location;
        String storepass;
        String alias;
        String keypassword;

        private KeyStore keyStore;

        public KeyPair getKey() {
            if (keyStore == null) {
                if (!location.exists())
                    generateKeyStore();

                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(location.getInputStream(), storepass.toCharArray());
            }

            Key key = keyStore.getKey(alias, keypassword.toCharArray());
            if (key instanceof PrivateKey) {
                Certificate cert = keyStore.getCertificate(alias);
                PublicKey publicKey = cert.getPublicKey();

                return new KeyPair(publicKey, (PrivateKey) key);
            }
        }

        public void generateKeyStore() {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            CertAndKeyGen certGen = new CertAndKeyGen("RSA", "SHA256WithRSA", null);
            certGen.generate(2048);

            long validSecs = (long) 10 * 365 * 24 * 60 * 60; // valid for ten years
            X509Certificate cert = certGen.getSelfCertificate(
                    new X500Name("CN=Docker Manager,O=My Organisation,L=My City,C=DE"), validSecs);

            keyStore.setKeyEntry(alias, certGen.getPrivateKey(), keypassword.toCharArray(), [ cert ] as X509Certificate[]);
            keyStore.store(new FileOutputStream(location.getFile()), storepass.toCharArray());
        }

    }

}
