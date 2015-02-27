package org.ventiv.docker.manager.controller

import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.config.DockerEnvironmentConfiguration

/**
 * Created by jcrygier on 2/27/15.
 */
@RequestMapping("/environment")
@RestController
class EnvironmentController {

    @RequestMapping
    public Collection<String> getTiers() {
        getAllEnvironments().keySet();
    }

    @RequestMapping("/{tierName}")
    public Collection<String> getEnvironments(@PathVariable("tierName") String tierName) {
        getAllEnvironments()[tierName];
    }

    @RequestMapping("/{tierName}/{environmentName}")
    public def getEnvironmentDetails(@PathVariable("tierName") String tierName, @PathVariable("environmentName") String environmentName) {
        DockerEnvironmentConfiguration envConfiguration = new DockerEnvironmentConfiguration(tierName, environmentName);

        return envConfiguration.configuration.applications.collect { applicationDef ->
            return [
                    name: applicationDef.name,
                    url: applicationDef.url
            ]
        }
    }

    private Map<String, List<String>> getAllEnvironments() {
        // Search for all YAML files under /data/env-config/tiers
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        def allEnvironments = resolver.getResources("classpath:/data/env-config/tiers/**/*.yml")

        // Group by Directory, then Massage the ClassPathResource elements into the filename minus .yml
        return allEnvironments.groupBy { new File(it.path).getParentFile().getName() }.collectEntries { k, v -> [k, v.collect { it.getFilename().replaceAll("\\.yml", "") }] }
    }


}
