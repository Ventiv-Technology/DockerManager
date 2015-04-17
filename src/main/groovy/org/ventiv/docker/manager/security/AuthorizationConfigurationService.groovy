package org.ventiv.docker.manager.security

import groovy.util.logging.Slf4j
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.model.MutableAcl
import org.springframework.security.acls.model.NotFoundException
import org.springframework.security.acls.model.ObjectIdentity
import org.springframework.security.acls.model.Permission
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.model.configuration.ApplicationConfiguration
import org.ventiv.docker.manager.service.ResourceWatcherService
import org.ventiv.docker.manager.utils.StringUtils
import org.yaml.snakeyaml.Yaml

import javax.annotation.PostConstruct

/**
 * Reads the authorization.yml file and populates the InMemoryMutableAclService for use in other parts of the application
 */
@Slf4j
@Service
class AuthorizationConfigurationService {

    @javax.annotation.Resource DockerManagerConfiguration props;
    @javax.annotation.Resource ResourceWatcherService resourceWatcherService;
    @javax.annotation.Resource InMemoryMutableAclService mutableAclService;

    boolean resourceExists = false;

    @PostConstruct
    public void loadConfigurationFromFile() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        Resource resource = resolver.getResource(props.config.location + "/authorization.yml")

        // Watch the resource
        resourceWatcherService.watchResource(resource, this.&readConfiguration)

        // Load it immediately
        readConfiguration(resource);
    }

    public void readConfiguration(Resource resource) {
        resourceExists = resource.exists();

        if (resource.exists()) {
            Yaml yaml = new Yaml()

            if (log.isDebugEnabled()) {
                log.debug("Loading authorization definition from YAML: " + resource);
            }

            def authorizationFile = yaml.loadAs(resource.getInputStream(), Map);

            // Populate the MutableAclService
            mutableAclService.clearAll();
            authorizationFile.each { String tierName, def environments ->
                environments.each { String environmentId, def applications ->
                    applications.each { String applicationId, def permissions ->
                        String mapKey = "${tierName}.${environmentId}.${applicationId}";

                        if (permissions instanceof String) {        // We have a list of users/groups defined for ALL permissions
                            DockerManagerPermission.getAllPermissions().each { Permission permission ->
                                addPermissionToConfig(mapKey, permission, permissions)
                            }
                        } else {                                    // We have each permission broken out with a list of users/groups
                            permissions.each { String permissionStr, String usersAndGroups ->
                                Permission permission = (Permission) DockerManagerPermission[StringUtils.toSnakeCase(permissionStr).toUpperCase()];
                                addPermissionToConfig(mapKey, permission, usersAndGroups);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addPermissionToConfig(String key, Permission permission, String usersAndGroups) {
        ObjectIdentity oi = new ObjectIdentityImpl(ApplicationConfiguration, key);

        usersAndGroups?.split(',')?.each { String userOrGroup ->
            MutableAcl acl = null;
            try {
                acl = mutableAclService.readAclById(oi)
            } catch (NotFoundException ignored) {
                acl = mutableAclService.createAcl(oi);
            }

            if (userOrGroup.trim().startsWith('[USER]')) {
                String principal = userOrGroup.trim().substring(6).trim()
                acl.insertAce(acl.getEntries().size(), permission, new PrincipalSid(principal), true);
            } else {
                String grantedAuthority = userOrGroup.trim();
                acl.insertAce(acl.getEntries().size(), permission, new GrantedAuthoritySid("ROLE_" + grantedAuthority), true);
            }

            mutableAclService.updateAcl(acl);
        }
    }

}
