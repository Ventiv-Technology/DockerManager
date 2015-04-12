package org.ventiv.docker.manager.utils

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
        if (request.getRequestURI().startsWith("/webjars/")) {
            response.addHeader("Cache-Control", "max-age=31536000, public")
            response.addHeader("Expires", "Fri, 01 Jan 9999 12:00:00 GMT");
        } else {
            response.addHeader("Cache-Control","no-cache, no-store, max-age=0, must-revalidate");
            response.addHeader("Pragma","no-cache");
            response.addHeader("Expires","0");
        }


    }
}
