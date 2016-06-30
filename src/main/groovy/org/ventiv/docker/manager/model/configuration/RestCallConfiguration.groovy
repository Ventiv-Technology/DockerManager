/*
 * Copyright (c) 2014 - 2016 Ventiv Technology
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
package org.ventiv.docker.manager.model.configuration

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import org.apache.commons.codec.binary.Base64
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import org.ventiv.docker.manager.utils.CachingGroovyShell

/**
 * Represents / Executes a REST based call.  Loosely based off of the W3C GlobalFetch Spec:
 * https://developer.mozilla.org/en-US/docs/Web/API/GlobalFetch/fetch
 * https://developer.mozilla.org/en-US/docs/Web/API/Request
 *
 * Utilizes Spring's RestTemplate, so you can take advantage of URL Templates.
 */
@CompileStatic
class RestCallConfiguration {

    HttpMethod method = HttpMethod.GET;
    String uri;
    Map<String, String> headers;
    RestCallCredentials credentials;
    String body;

    @JsonIgnore
    private CachingGroovyShell urlTemplate;

    public Object makeCall(Map<String, ?> uriVariables) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity request = new HttpEntity(body, getHttpHeaders());

        ResponseEntity<Object> answer = restTemplate.exchange(getUri(), getMethod(), request, Object.class, uriVariables);
        return answer.getBody();
    }

    public HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        this.headers?.each { k, v -> headers.add(k, v) }

        // Handle Authentication (Credentials)
        if (credentials) {
            headers.remove(HttpHeaders.AUTHORIZATION);

            if (credentials.token)
                headers.add(HttpHeaders.AUTHORIZATION, "Bearer ${credentials.token}")
            else if (credentials.isBasic())
                headers.add(HttpHeaders.AUTHORIZATION, "Basic ${credentials.getBasicHeader()}")
        }

        return headers;
    }

    public String getFilledUrl(Map<String, Object> variables = [:]) {
        // If we don't have a 'template' just return the URI (appending the branch - if provided)
        if (!getUri().contains('$')) {
            return variables.branch ? "${getUri()}${variables.branch}" : getUri()
        }

        // Otherwise, use the template version - as branch can be anywhere.
        if (urlTemplate == null) {
            urlTemplate = new CachingGroovyShell('"' + getUri() + "'")
        }

        return urlTemplate.eval(variables);
    }

    public static final class RestCallCredentials {
        String username;
        String password;
        String token;

        boolean isBasic() {
            return username && password;
        }

        String getBasicHeader() {
            return Base64.encodeBase64URLSafeString("${username}:${password}".getBytes()) + "==";
        }
    }

}
