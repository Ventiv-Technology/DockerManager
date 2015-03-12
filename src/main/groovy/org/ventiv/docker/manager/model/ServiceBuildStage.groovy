package org.ventiv.docker.manager.model

import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject
import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.build.AsyncBuildStage
import org.ventiv.docker.manager.build.BuildStage

/**
 * Created by jcrygier on 3/11/15.
 */
class ServiceBuildStage {

    String type;
    Map<String, String> settings;

    public Promise execute(Map<String, Object> buildContext) {
        def buildStage = DockerManagerApplication.getApplicationContext().getBean(type);

        if (buildStage instanceof BuildStage) {
            DeferredObject deferred = new DeferredObject();

            buildStage.doBuild(settings, buildContext);
            deferred.resolve("finished");

            return deferred.promise();
        } else if (buildStage instanceof AsyncBuildStage) {
            return buildStage.doBuild(settings, buildContext);
        }
    }

}
