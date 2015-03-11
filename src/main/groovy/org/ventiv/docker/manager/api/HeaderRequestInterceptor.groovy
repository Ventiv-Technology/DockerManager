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
