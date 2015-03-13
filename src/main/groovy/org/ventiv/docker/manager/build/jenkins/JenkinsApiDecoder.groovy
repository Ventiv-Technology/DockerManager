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
package org.ventiv.docker.manager.build.jenkins

import feign.Response
import feign.jackson.JacksonDecoder

import java.lang.reflect.Type

/**
 * Created by jcrygier on 3/12/15.
 */
class JenkinsApiDecoder extends JacksonDecoder {

    @Override
    public Object decode(Response response, Type type) throws IOException {
        if (type instanceof Class && type == BuildStartedResponse) {
            String statusUrl = response.headers()['Location'].first();
            Integer queueId = Integer.parseInt(statusUrl.substring(statusUrl.indexOf("/queue/item/") + 12, statusUrl.lastIndexOf("/")))

            return new BuildStartedResponse([success: true, statusUrl: statusUrl, queueId: queueId]);
        } else {
            return super.decode(response, type);
        }
    }

}