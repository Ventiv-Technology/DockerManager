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
package org.ventiv.docker.manager.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.config.DockerManagerConfiguration

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

/**
 * Created by jcrygier on 3/11/15.
 */
@Controller
class LoginController {

    @Resource DockerManagerConfiguration props;

    @RequestMapping("/login")
    def loginTest(HttpServletRequest request) {
        DockerManagerApplication.setApplicationUrl(request.getRequestURL().replaceAll('/login', '').toString());
        return new ModelAndView("login", [props: props])
    }

}
