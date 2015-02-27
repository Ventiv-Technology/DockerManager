package org.ventiv.docker.manager.api

import feign.RequestInterceptor
import feign.RequestTemplate

/**
 * Created by jcrygier on 2/25/15.
 */
class HeaderRequestInterceptor implements RequestInterceptor {

    private Map<String, String> headers;

    public HeaderRequestInterceptor(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    void apply(RequestTemplate template) {
        headers.each { k, v ->
            template.header(k, v);
        }
    }

}
