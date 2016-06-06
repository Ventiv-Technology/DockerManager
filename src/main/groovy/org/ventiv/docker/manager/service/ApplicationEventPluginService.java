/**
 * Copyright (c) 2014 - 2016 Ventiv Technology
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
package org.ventiv.docker.manager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.ventiv.docker.manager.plugin.EventPlugin;

import javax.annotation.Resource;

/**
 * Created by jcrygier on 6/6/16.
 */
@Service
public class ApplicationEventPluginService implements ApplicationListener<ApplicationEvent> {

    public static final Logger log = LoggerFactory.getLogger(ApplicationEventPluginService.class);
    @Resource private PluginService pluginService;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        for (EventPlugin eventPlugin : pluginService.getEventPlugins()) {
            try {
                eventPlugin.onApplicationEvent(event);
            } catch (Exception e) {
                log.error("Error executing Plugin, eating exception:", e);
            }
        }
    }

}
