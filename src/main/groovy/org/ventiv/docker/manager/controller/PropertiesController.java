/**
 * Copyright (c) 2014 - 2017 Ventiv Technology
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
package org.ventiv.docker.manager.controller;

import org.codehaus.groovy.runtime.metaclass.ConcurrentReaderHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.ventiv.docker.manager.config.DockerManagerConfiguration;
import org.ventiv.docker.manager.model.EnvironmentProperty;
import org.ventiv.docker.manager.model.configuration.ApplicationConfiguration;
import org.ventiv.docker.manager.model.configuration.ServiceInstanceConfiguration;
import org.ventiv.docker.manager.security.DockerManagerPermission;
import org.ventiv.docker.manager.security.SecurityUtil;
import org.ventiv.docker.manager.service.EnvironmentConfigurationService;
import org.ventiv.docker.manager.utils.CollectionUtils;
import org.ventiv.docker.manager.utils.DockerManagerConstructor;
import org.ventiv.docker.manager.utils.EncryptionUtil;
import org.ventiv.docker.manager.utils.StringUtils;
import org.ventiv.docker.manager.utils.YamlUtils;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by jcrygier on 7/27/17.
 */
@RequestMapping("/api/properties")
@RestController
public class PropertiesController {

    private static final Logger log = LoggerFactory.getLogger(PropertiesController.class);
    public static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.*)}");

    @javax.annotation.Resource DockerManagerConfiguration props;
    @javax.annotation.Resource EnvironmentConfigurationService environmentConfigurationService;

    @RequestMapping("/encrypt")
    public String encrypt(@RequestParam("value") String value) {
        KeyPair key = props.getKeystore().getKey();
        return EncryptionUtil.encrypt(key.getPublic(), value);
    }

    /**
     * Gets specific properties for a given Service within an Application, Environment, Tier.  This method does all of the
     * heavy lifting to get the properties back in a structured-data format, and other methods can serialize to different
     * formats (e.g. Java Properties)
     *
     * @param tierName
     * @param environmentName
     * @param applicationName
     * @param serviceName
     * @param propertySet
     * @return
     */
    @PreAuthorize("hasPermission(#tierName + '.' + #environmentName + '.' + #applicationName, 'PROPERTIES_READ')")
    @RequestMapping("/{tierName}/{environmentName}/{applicationName}/{serviceName}")
    public Map<String, EnvironmentProperty> getEnvironmentProperties(@PathVariable("tierName") String tierName,
                                                                     @PathVariable("environmentName") String environmentName,
                                                                     @PathVariable("applicationName") String applicationName,
                                                                     @PathVariable("serviceName") String serviceName,
                                                                     @RequestParam(value = "propertySet", required = false) String propertySet) {
        KeyPair key = props.getKeystore().getKey();

        // Build out a collection of EnvironmentProperty objects, with the 'lowest' level (Service Instance) first
        Collection<EnvironmentProperty> allProperties = new ArrayList<>();
        ApplicationConfiguration applicationConfiguration = environmentConfigurationService.getApplication(tierName, environmentName, applicationName);
        Optional<ServiceInstanceConfiguration> serviceInstanceConfiguration = applicationConfiguration.getServiceInstances().stream()
                .filter(it -> it.getType().equals(serviceName))
                .findFirst();

        serviceInstanceConfiguration.ifPresent(it -> allProperties.addAll(it.getProperties()));
        allProperties.addAll(applicationConfiguration.getProperties());
        allProperties.addAll(environmentConfigurationService.getEnvironment(tierName, environmentName).getProperties());
        allProperties.addAll(loadPropertiesYaml(serviceName));

        // Determine if we have permissions to decrypt
        boolean hasDecryptPermission = SecurityUtil.hasPermission(tierName, environmentName, applicationName, DockerManagerPermission.SECRETS);

        // Finally, filter the list from the request, taking the 'first' ones as the highest priority
        Map<String, EnvironmentProperty> answer = allProperties.stream()
                .filter(it -> isPresentOrEmpty(it.getTiers(), tierName))                            // Filter by tier name
                .filter(it -> isPresentOrEmpty(it.getEnvironments(), environmentName))              // Filter by environment name
                .filter(it -> isPresentOrEmpty(it.getApplications(), applicationName))              // Filter by application name
                .filter(it -> isPresentOrEmpty(it.getPropertySets(), propertySet))                  // Filter by property set
                .filter(distinctByKey(EnvironmentProperty::getName))                                // Ensure we have distinct values, before we return a 'final' list
                .map(it -> decryptIfNecessary(hasDecryptPermission, key.getPrivate(), it))                                // Decrypt the value, if necessary
                .collect(CollectionUtils.toLinkedHashMap(EnvironmentProperty::getName, Function.identity()));

        // Get the 'Global' Properties...strictly for filling in 'templates'
        Map<String, String> binding = loadPropertiesYaml("global-properties").stream()
                .map(it -> decryptIfNecessary(hasDecryptPermission, key.getPrivate(), it))                                // Decrypt the value, if necessary
                .collect(Collectors.toMap(EnvironmentProperty::getName, EnvironmentProperty::getValue));

        // TODO: Add in properties from Application + Service Instances

        // Fill in any props that need it
        answer.entrySet().stream()
                .filter(it -> it.getValue().getValue().contains("${"))
                .map(it -> {
                    String replaced = StringUtils.replace(it.getValue().getValue(), VARIABLE_PATTERN, m -> binding.get(m.group(1)));
                    it.getValue().setValue(replaced);

                    return it;
                })
                .collect(Collectors.toList());

        return answer;
    }

    /**
     * Gets properties for a specific Service, serializing to Java Properties format.
     *
     * @param tierName
     * @param environmentName
     * @param applicationName
     * @param serviceName
     * @param propertySet
     * @return
     */
    @PreAuthorize("hasPermission(#tierName + '.' + #environmentName + '.' + #applicationName, 'PROPERTIES_READ')")
    @RequestMapping(value = "/{tierName}/{environmentName}/{applicationName}/{serviceName}", produces = "text/plain")
    public String getEnvironmentPropertiesText(@PathVariable("tierName") String tierName,
                                                                     @PathVariable("environmentName") String environmentName,
                                                                     @PathVariable("applicationName") String applicationName,
                                                                     @PathVariable("serviceName") String serviceName,
                                                                     @RequestParam(value = "propertySet", required = false) String propertySet) {
        Map<String, EnvironmentProperty> props = getEnvironmentProperties(tierName, environmentName, applicationName, serviceName, propertySet);

        return props.entrySet().stream()
                .map(entry -> {
                    StringBuilder sb = new StringBuilder();
                    if (entry.getValue().getComments() != null)
                        sb.append("\r\n# ").append(Arrays.stream(entry.getValue().getComments().split("\n")).collect(Collectors.joining("\r\n# "))).append("\r\n");

                    sb.append(entry.getKey()).append("=").append(entry.getValue().getValue());
                    return sb.toString();
                })
                .collect(Collectors.joining("\n"));
    }

    /*************************************** Internal helper methods *************************************/
    private Collection<EnvironmentProperty> loadPropertiesYaml(String serviceName) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(props.getConfig().getLocation() + "/properties/" + serviceName + ".yml");

        if (resource.exists()) {
            Collection<Map<String, Object>> fileContents = YamlUtils.loadAs(resource, Collection.class);
            return fileContents.stream()
                    .map(DockerManagerConstructor::extractEnvironmentProperty)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private <T> boolean isPresentOrEmpty(Collection<T> coll, T toFind) {
        if (coll == null || coll.isEmpty() || toFind == null)
            return true;

        return coll.contains(toFind);
    }

    private EnvironmentProperty decryptIfNecessary(boolean hasPermission, PrivateKey decryptingKey, EnvironmentProperty prop) {
        if (hasPermission && prop.isSecure())
            prop.setValue(EncryptionUtil.decrypt(decryptingKey, prop.getValue()));

        return prop;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object,Boolean> seen = new ConcurrentReaderHashMap();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

}
