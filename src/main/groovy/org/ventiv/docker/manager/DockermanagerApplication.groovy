package org.ventiv.docker.manager

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.webjars.requirejs.EnableWebJarsRequireJs

@SpringBootApplication
@EnableWebJarsRequireJs
@EnableConfigurationProperties(DockerManagerConfiguration)
class DockerManagerApplication {

    static ApplicationContext applicationContext;
    static DockerManagerConfiguration props;

    static void main(String[] args) {
        applicationContext = SpringApplication.run DockerManagerApplication, args
        props = applicationContext.getBean(DockerManagerConfiguration)
    }
}
