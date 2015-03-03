package org.ventiv.docker.manager

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.ventiv.webjars.requirejs.EnableWebJarsRequireJs

@SpringBootApplication
@EnableWebJarsRequireJs
class DockermanagerApplication {

    static ApplicationContext applicationContext;

    static void main(String[] args) {
        applicationContext = SpringApplication.run DockermanagerApplication, args
    }
}
