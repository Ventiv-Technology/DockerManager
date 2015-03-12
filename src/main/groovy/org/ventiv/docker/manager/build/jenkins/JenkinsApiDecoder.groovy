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