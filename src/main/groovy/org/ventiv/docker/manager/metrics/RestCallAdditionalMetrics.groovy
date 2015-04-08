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
package org.ventiv.docker.manager.metrics

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.service.SimpleTemplateService
import org.ventiv.docker.manager.utils.AuthenticationRequestInterceptor

import javax.annotation.Resource

/**
 * Restful call to get additional metrics for a provided Service Instance
 */
@Slf4j
@CompileStatic
@Component("RestCall")
class RestCallAdditionalMetrics implements AdditionalMetrics {

    // Url to hit for the rest call - may have template parameters using the template service
    public static final String CONFIG_URL =                     'url'

    // See other settings in AuthenticationRequestInterceptor

    @Resource SimpleTemplateService templateService;
    RestTemplate restTemplate = new RestTemplate();

    @Override
    Object getAdditionalMetrics(ServiceInstance serviceInstance, Map<String, String> settings) {
        if (serviceInstance.getStatus() == ServiceInstance.Status.Running) {
            Map<String, Object> bindings = serviceInstance.getTemplateBindings();
            bindings.put("settings", settings);

            String url = templateService.fillTemplate(settings[CONFIG_URL], bindings);
            log.info("Getting addtional metrics for ${serviceInstance.name} at ${url}");

            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                restTemplate.setInterceptors(Arrays.asList((ClientHttpRequestInterceptor) new AuthenticationRequestInterceptor(settings, auth)));

                return restTemplate.getForObject(url, Object, bindings);
            } catch (Exception e) {
                log.error("Unable to retrieve additional metrics: ${e.getMessage()}");
            }
        }

        return null;
    }

}
