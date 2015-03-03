package org.ventiv.docker.manager

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories("com.aon.esolutions.envmanager")
class DockermanagerApplication {

    static ApplicationContext applicationContext;

    static void main(String[] args) {
        applicationContext = SpringApplication.run DockermanagerApplication, args
    }
}
