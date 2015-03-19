package org.ventiv.docker.manager.service

import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.plugin.CreateContainerPlugin

import javax.annotation.PostConstruct
import javax.annotation.Resource

/**
 * Creates all of the plugins in the system.  It will attempt to create based on the following constructor arguments, in order:
 *
 * Environment
 * No-Arg
 *
 */
@Service
class PluginService {

    @Resource ApplicationContext applicationContext;
    @Resource Environment env;
    @Resource DockerManagerConfiguration props;

    List<CreateContainerPlugin> createContainerPlugins;

    @PostConstruct
    public void loadPlugins() {
        // Instantiate all of the plugins
        List<Object> instantiatedPlugins = props.plugins.collect(this.&instantiatePlugin).findAll { return it != null }

        // Now, sort em!
        createContainerPlugins = instantiatedPlugins.findAll { it instanceof CreateContainerPlugin }
    }

    private Object instantiatePlugin(Class pluginClass) {
        try {
            return pluginClass.getDeclaredConstructor(Environment).newInstance(env);
        } catch (Exception ignored) {}

        try {
            return pluginClass.getDeclaredConstructor(ApplicationContext).newInstance(applicationContext);
        } catch (Exception ignored) {}

        try {
            return pluginClass.newInstance();
        } catch (Exception ignored) {}

        return null;
    }

}
