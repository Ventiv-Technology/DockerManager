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
package org.ventiv.docker.manager.utils

import groovy.time.TimeCategory
import org.springframework.security.web.header.HeaderWriter
import org.springframework.stereotype.Component

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by jcrygier on 4/12/15.
 */
@Component
class CacheHeaderWriter implements HeaderWriter {

    @Override
    void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        use(TimeCategory) {
            if (request.getRequestURI().startsWith("/webjars/")) {
                response.setHeader("Cache-Control", "public, max-age=31536000")
                response.setDateHeader("Expires", (new Date() + 1.years).getTime());
            } else {
                response.addHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
                response.addHeader("Pragma", "no-cache");
                response.addHeader("Expires", "0");
            }
        }
    }
}
