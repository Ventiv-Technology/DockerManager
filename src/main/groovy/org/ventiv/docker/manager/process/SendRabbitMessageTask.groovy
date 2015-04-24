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
package org.ventiv.docker.manager.process

import org.activiti.engine.delegate.DelegateExecution
import org.activiti.engine.impl.el.Expression
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageBuilder
import org.springframework.amqp.core.MessagePropertiesBuilder
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate

import javax.annotation.Nullable
import javax.validation.constraints.NotNull

/**
 * Activiti Task to send a message to a RabbitMQ Exchange.
 */
class SendRabbitMessageTask extends AbstractDockerManagerTask {

    /**
     * The host name where RabbitMQ is sitting
     */
    @NotNull
    private Expression hostName;

    /**
     * The AMQP port of the RabbitMQ server.
     */
    @NotNull
    private Expression port;

    /**
     * The Exchange to send the message to
     */
    @NotNull
    private Expression exchange;

    /**
     * The Routing Key to use when publishing the message.  May be null.
     */
    @Nullable
    private Expression routingKey;

    /**
     * The Contents of the message to publish
     */
    @NotNull
    private Expression message;

    /**
     * Any headers to publish.  Should be a comma separated list of properites.  Example:
     * Header1=Header1Value,Header2=Header2Value
     */
    @Nullable
    private Expression headers;

    /**
     * The Content type of the message
     */
    @Nullable
    private Expression contentType;

    @Override
    void execute(DelegateExecution execution) throws Exception {
        if (hostName == null || port == null || exchange == null || message == null)
            return;

        Map<String, String> headers = headers?.getValue(execution)?.toString()?.split(',')?.collectEntries {
            def parts = it.split('=');
            return [parts[0], parts[1]]
        }

        MessagePropertiesBuilder messageProperties = MessagePropertiesBuilder.newInstance();
        if (contentType)
            messageProperties.setContentType(contentType.getValue(execution)?.toString());
        headers.each { k, v ->
            messageProperties.setHeader(k, v);
        }

        Message amqpMessage = MessageBuilder.withBody(message.getValue(execution).toString().getBytes())
                                            .andProperties(messageProperties.build())
                                            .build();

        getAmqpTemplate(execution).send(exchange?.getValue(execution)?.toString(), routingKey?.getValue(execution)?.toString(), amqpMessage);
    }

    private AmqpTemplate getAmqpTemplate(DelegateExecution execution) {
        ConnectionFactory factory = new CachingConnectionFactory(hostName.getValue(execution).toString(), Integer.parseInt(port.getValue(execution).toString()))
        return new RabbitTemplate(factory)
    }

}
