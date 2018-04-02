/*
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
package org.ventiv.docker.manager.plugin

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.util.logging.Slf4j
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEvent
import org.springframework.core.env.Environment
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.client.RestTemplate
import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.service.SimpleTemplateService
import org.ventiv.docker.manager.utils.CachingGroovyShell

/**
 * Created by jcrygier on 6/6/16.
 */
@Slf4j
public class SlackNotificationPlugin implements EventPlugin {

    SimpleTemplateService simpleTemplateService;
    private List<EventConfiguration> configurations = [];

    public SlackNotificationPlugin(ApplicationContext ac, Environment env) {
        readConfiguration(env);
        this.simpleTemplateService = ac.getBean(SimpleTemplateService)
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        configurations
                .findAll { it.eventType.isAssignableFrom(event.getClass()) }
                .findAll {
                    return it.cachingGroovyShell.eval([event: event]);
                }
                .each { this.sendMessage(it, event) }
    }

    private void sendMessage(EventConfiguration config, ApplicationEvent event) {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        Map<String, Object> bindings = [event: event, user: user, applicationUrl: DockerManagerApplication.applicationUrl];

        SlackMessage message = new SlackMessage();
        message.channel = config.channel;

        SlackAttachment attachment = new SlackAttachment();
        message.attachments = [attachment];
        attachment.pretext = simpleTemplateService.fillTemplate(config.textTemplate, bindings);
        attachment.fallback = simpleTemplateService.fillTemplate(config.textTemplate, bindings);  // TODO: Parse fields into here
        attachment.color = config.color;

        attachment.fields = config.fields.collect {
            SlackField field = new SlackField();
            field.title = it.label;
            field.value = simpleTemplateService.fillTemplate('#{' + it.value + '}', bindings)
            field.shortField = it.shortField;

            return field;
        }

        RestTemplate restTemplate = new RestTemplate();
        try {
            restTemplate.postForLocation(config.url, message);
        } catch (Exception e) {
            log.error("Error sending Slack Notification: " + e.getMessage());
        }
    }

    private void readConfiguration(Environment env) {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            if (!env.containsProperty("plugin.slack[$i].eventType"))
                break;

            EventConfiguration config = new EventConfiguration();
            config.eventType = Class.forName(env.getProperty("plugin.slack[$i].eventType"));
            config.url = env.getProperty("plugin.slack[$i].url")
            config.channel = env.getProperty("plugin.slack[$i].channel").replaceAll('\\\\#', '#')
            config.textTemplate = env.getProperty("plugin.slack[$i].text").replaceAll('\\\\#', '#')
            config.color = env.getProperty("plugin.slack[$i].color");

            // Parse the criteria
            config.cachingGroovyShell = new CachingGroovyShell(env.getProperty("plugin.slack[$i].criteria"));

            // Get the fields
            String includedFields = env.getProperty("plugin.slack[$i].includedFields");
            if (includedFields) {
                String[] allFields = includedFields.split(',');

                config.fields = allFields.collect {
                    String fieldId = it.trim();
                    EventConfigurationField field = new EventConfigurationField();
                    field.id = fieldId;
                    field.label = env.getProperty("plugin.slack[$i].fields.${fieldId}.label")
                    field.value = env.getProperty("plugin.slack[$i].fields.${fieldId}.value")
                    field.shortField = env.getProperty("plugin.slack[$i].fields.${fieldId}.short", "true");

                    return field;
                }
            }

            configurations << config;
        }
    }

    public static final class EventConfiguration {
        Class eventType;
        String url;
        String channel;
        String textTemplate;
        String color;
        CachingGroovyShell cachingGroovyShell;
        List<EventConfigurationField> fields;
    }

    public static final class EventConfigurationField {
        String id;
        String label;
        String value;
        boolean shortField = true;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class SlackMessage {
        public String text;
        public String username;
        public String icon_url;
        public String icon_emoji;
        public String channel;
        public List<SlackAttachment> attachments;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class SlackAttachment {
        public String fallback;
        public String text;
        public String pretext;
        public String color;
        public List<SlackField> fields;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class SlackField {
        String title;
        String value;

        @JsonProperty("short")
        boolean shortField;
    }

}
