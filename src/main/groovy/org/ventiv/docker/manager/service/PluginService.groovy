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
package org.ventiv.docker.manager.service

import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.plugin.CreateContainerPlugin
import org.ventiv.docker.manager.plugin.EventPlugin

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
    List<EventPlugin> eventPlugins;

    @PostConstruct
    public void loadPlugins() {
        // Instantiate all of the plugins
        List<Object> instantiatedPlugins = props.plugins.collect(this.&instantiatePlugin).findAll { return it != null }

        // Now, sort em!
        createContainerPlugins = instantiatedPlugins.findAll { it instanceof CreateContainerPlugin }
        eventPlugins = instantiatedPlugins.findAll { it instanceof EventPlugin }
    }

    private Object instantiatePlugin(Class pluginClass) {
        try {
            return pluginClass.getDeclaredConstructor(ApplicationContext, Environment).newInstance(applicationContext, env);
        } catch (Exception ignored) {}

        try {
            return pluginClass.getDeclaredConstructor(Environment).newInstance(env);
        } catch (Exception ignored) {}

        try {
            return pluginClass.getDeclaredConstructor(ApplicationContext).newInstance(applicationContext);
        } catch (Exception ignored) {}

        try {
            return pluginClass.getDeclaredConstructor(DockerManagerConfiguration).newInstance(props);
        } catch (Exception ignored) {}

        try {
            return pluginClass.newInstance();
        } catch (Exception ignored) {}

        return null;
    }

}
