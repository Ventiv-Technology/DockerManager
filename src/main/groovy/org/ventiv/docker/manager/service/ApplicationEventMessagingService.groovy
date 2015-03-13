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

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.messaging.converter.MessageConversionException
import org.springframework.messaging.core.MessageSendingOperations
import org.springframework.stereotype.Service
import org.ventiv.docker.manager.config.DockerManagerConfiguration

import javax.annotation.Resource

/**
 * Service that listens to all Spring ApplicationEvents, and forwards the configured ones to the message broker.  There
 * should be a client listening on the other side of the message broker on /topic/application-event.
 */
@Service
class ApplicationEventMessagingService implements ApplicationListener<ApplicationEvent> {

    @Resource private final MessageSendingOperations<String> messagingTemplate;
    @Resource private final DockerManagerConfiguration props;

    @Override
    void onApplicationEvent(ApplicationEvent event) {
        try {
            messagingTemplate.convertAndSend("/topic/event/${event.getClass().getSimpleName()}".toString(), [
                    type     : event.getClass().getName(),
                    source   : event.getSource(),
                    timestamp: event.getTimestamp()
            ])
        } catch (MessageConversionException ignored) {}
    }
}
