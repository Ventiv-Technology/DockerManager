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
package org.ventiv.docker.manager

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.metrics.store.AbstractAdditionalMetricsStore
import org.ventiv.docker.manager.metrics.store.InMemoryAdditionalMetricsStore
import org.ventiv.docker.manager.metrics.store.JpaAdditionalMetricsStore
import org.ventiv.webjars.requirejs.EnableWebJarsRequireJs

@SpringBootApplication
@EnableWebJarsRequireJs
@EnableScheduling
@EnableConfigurationProperties(DockerManagerConfiguration)
class DockerManagerApplication {

    @Autowired JdbcTemplate jdbcTemplate;

    static ApplicationContext applicationContext;
    static DockerManagerConfiguration props;
    static String applicationUrl;

    static void main(String[] args) {
        applicationContext = SpringApplication.run DockerManagerApplication, args
        props = applicationContext.getBean(DockerManagerConfiguration)
    }

    static void setApplicationUrl(String url) {
        if (applicationUrl == null)
            applicationUrl = url;
    }

    @Primary
    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler();
    }

    @Bean
    public AbstractAdditionalMetricsStore additionalMetricsStore() {
        if (jdbcTemplate) {
            return new JpaAdditionalMetricsStore();
        } else
            return new InMemoryAdditionalMetricsStore();
    }

    //@Bean
    @ConditionalOnClass(name = ["org.h2.tools.Server"])
    public Object h2TcpServer() {
        String[] realArguments = [ "-tcpPort", "9092", "-tcpAllowOthers" ] as String[]
        String[][] arguments = [ realArguments ] as String[][]

        try {
            Object server = Class.forName("org.h2.tools.Server").getDeclaredMethod("createTcpServer", String[]).invoke(null, arguments);
            server.start()

            return server;
        } catch (Exception ignored) {}
    }

    @Bean
    @ConditionalOnClass(name = ["org.h2.tools.Server"])
    public Object h2WebServer() {
        String[] realArguments = [ "-webPort", "8082", "-webAllowOthers" ] as String[]
        String[][] arguments = [ realArguments ] as String[][]

        try {
            Object server = Class.forName("org.h2.tools.Server").getDeclaredMethod("createWebServer", String[]).invoke(null, arguments);
            server.start()

            return server;
        } catch (Exception ignored) {}
    }
}
