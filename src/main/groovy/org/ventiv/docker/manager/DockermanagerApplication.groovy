package org.ventiv.docker.manager

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories("com.aon.esolutions.envmanager")
class DockermanagerApplication {

    static void main(String[] args) {
        SpringApplication.run DockermanagerApplication, args
    }
}
