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

import feign.RequestInterceptor
import feign.RequestTemplate
import groovy.transform.CompileStatic
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.support.HttpRequestWrapper
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails

/**
 * Request interceptor(s) that intercept an outgoing HTTP call in RestTemplate or Feign and inject headers with
 * BASIC authentication.
 */
@CompileStatic
class AuthenticationRequestInterceptor implements ClientHttpRequestInterceptor, RequestInterceptor {

    // Authentication Type - None, CurrentUser, ProvidedUserPassword
    public static final String CONFIG_AUTHENTICATION =          'authentication'

    // Credentials to call Jenkins - Only useful if authentication = ProvidedUserPassword
    public static final String CONFIG_USER =                    'user'
    public static final String CONFIG_PASSWORD =                'password'

    private Map<String, String> calculatedHeaders = [
            Accept: MediaType.APPLICATION_JSON_VALUE
    ];

    public AuthenticationRequestInterceptor(Map<String, String> configuration, Authentication userAuthentication) {
        AuthenticationType authType = configuration[CONFIG_AUTHENTICATION] ? AuthenticationType.valueOf(configuration[CONFIG_AUTHENTICATION]) : AuthenticationType.None;

        if (authType == AuthenticationType.CurrentUser) {
            String userName = userAuthentication.getPrincipal() instanceof String ? userAuthentication.getPrincipal() : ((UserDetails)userAuthentication.getPrincipal()).getUsername();
            String authHeader = "${userName}:${userAuthentication.getCredentials()}".bytes.encodeBase64().toString()

            calculatedHeaders.put("Authorization", "Basic " + authHeader)
        } else if (authType == AuthenticationType.ProvidedUserPassword) {
            String authHeader = "${configuration[CONFIG_USER]}:${configuration[CONFIG_PASSWORD]}".bytes.encodeBase64().toString()
            calculatedHeaders.put("Authorization", "Basic " + authHeader)
        }
    }

    @Override
    ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpRequest wrapper = new HttpRequestWrapper(request);
        calculatedHeaders.each { k, v ->
            wrapper.getHeaders().set(k, v);
        }

        return execution.execute(wrapper, body);
    }

    @Override
    void apply(RequestTemplate template) {
        calculatedHeaders.each { k, v ->
            template.header(k, v);
        }
    }

    public static final enum AuthenticationType {
        None, CurrentUser, ProvidedUserPassword
    }
}
