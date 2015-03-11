package org.ventiv.docker.manager.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import org.ventiv.docker.manager.config.DockerManagerConfiguration

import javax.annotation.Resource

/**
 * Created by jcrygier on 3/11/15.
 */
@Controller
class LoginController {

    @Resource DockerManagerConfiguration props;

    @RequestMapping("/login")
    def loginTest() {
        return new ModelAndView("login", [props: props])
    }

}
