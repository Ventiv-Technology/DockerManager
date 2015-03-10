package org.ventiv.docker.manager

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.webjars.requirejs.EnableWebJarsRequireJs

@SpringBootApplication
@EnableWebJarsRequireJs
@EnableScheduling
@EnableConfigurationProperties(DockerManagerConfiguration)
class DockerManagerApplication {

    static ApplicationContext applicationContext;
    static DockerManagerConfiguration props;

    static void main(String[] args) {
        applicationContext = SpringApplication.run DockerManagerApplication, args
        props = applicationContext.getBean(DockerManagerConfiguration)
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler();
    }
}
